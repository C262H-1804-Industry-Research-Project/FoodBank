package foodbank.webapp.service;

import foodbank.webapp.dto.AdminDonorAccountResponse;
import foodbank.webapp.dto.AdminRecipientAccountResponse;
import foodbank.webapp.entity.BanRecord;
import foodbank.webapp.entity.User;
import foodbank.webapp.repositories.BanRecordRepository;
import foodbank.webapp.repositories.CollectionHistoryRepository;
import foodbank.webapp.repositories.DonorRepository;
import foodbank.webapp.repositories.ListingRepository;
import foodbank.webapp.repositories.RecipientRepository;
import foodbank.webapp.repositories.ReportRepository;
import foodbank.webapp.repositories.RequestRepository;
import foodbank.webapp.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final DonorRepository donorRepository;
    private final RecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final BanRecordRepository banRecordRepository;
    private final ListingRepository listingRepository;
    private final RequestRepository requestRepository;
    private final CollectionHistoryRepository collectionHistoryRepository;

    public List<AdminDonorAccountResponse> getDonors() {
        return donorRepository.findAll().stream()
                .map(donor -> {
                    User user = getUser(donor.getUserId());
                    return new AdminDonorAccountResponse(
                            donor.getId(),
                            user.getUserId(),
                            donor.getOrgName(),
                            reportRepository.countByReportedId(user.getUserId()),
                            listingRepository.countByDonor_Id(donor.getId()),
                            collectionHistoryRepository.countByDonorId(donor.getId()),
                            banRecordRepository.countByUserId(user.getUserId()),
                            accountStatus(user),
                            banDaysRemaining(user.getUserId())
                    );
                })
                .sorted(Comparator
                        .comparingLong(AdminDonorAccountResponse::reportsAgainst)
                        .reversed()
                        .thenComparing(AdminDonorAccountResponse::organisation,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<AdminRecipientAccountResponse> getRecipients() {
        return recipientRepository.findAll().stream()
                .map(recipient -> {
                    User user = getUser(recipient.getUserId());
                    return new AdminRecipientAccountResponse(
                            recipient.getRecipientId(),
                            user.getUserId(),
                            recipient.getName(),
                            reportRepository.countByReportedId(user.getUserId()),
                            requestRepository.countByRecipientId(recipient.getRecipientId()),
                            collectionHistoryRepository.countByRecipientId(recipient.getRecipientId()),
                            banRecordRepository.countByUserId(user.getUserId()),
                            accountStatus(user),
                            banDaysRemaining(user.getUserId())
                    );
                })
                .sorted(Comparator
                        .comparingLong(AdminRecipientAccountResponse::reportsAgainst)
                        .reversed()
                        .thenComparing(AdminRecipientAccountResponse::recipient,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private User getUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Account user does not exist"
                ));
    }

    private String accountStatus(User user) {
        if ("BANNED".equalsIgnoreCase(user.getStatus())) {
            return "BANNED";
        }
        if ("PENDING".equalsIgnoreCase(user.getStatus())) {
            return "UNDER_REVIEW";
        }
        return "ACTIVE";
    }

    private Long banDaysRemaining(Integer userId) {
        return banRecordRepository
                .findFirstByUserIdAndBanTypeIgnoreCaseAndActiveTrueOrderByEndDateDesc(
                        userId,
                        "TEMPORARY"
                )
                .map(BanRecord::getEndDate)
                .map(endDate -> Math.max(
                        0,
                        ChronoUnit.DAYS.between(LocalDate.now(), endDate)
                ))
                .orElse(null);
    }
}
