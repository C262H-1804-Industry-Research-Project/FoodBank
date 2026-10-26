package foodbank.webapp.service;

import foodbank.webapp.dto.AdminAccountResponse;
import foodbank.webapp.dto.AdminDonorAccountResponse;
import foodbank.webapp.dto.AdminRecipientAccountResponse;
import foodbank.webapp.entity.BanRecord;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.entity.Recipient;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
                            user.getEmail(),
                            user.getPhone(),
                            donor.getSuburb(),
                            donor.getRegion(),
                            reportRepository.countByReportedId(user.getUserId()),
                            listingRepository.countByDonor_Id(donor.getId()),
                            collectionHistoryRepository.countSuccessfulByDonorId(donor.getId()),
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
                            user.getEmail(),
                            user.getPhone(),
                            recipient.getSuburb(),
                            recipient.getRegion(),
                            reportRepository.countByReportedId(user.getUserId()),
                            requestRepository.countByRecipientId(recipient.getRecipientId()),
                            collectionHistoryRepository.countSuccessfulByRecipientId(recipient.getRecipientId()),
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

    public static final Set<String> ROLES = Set.of("DONOR", "RECIPIENT");
    public static final Set<String> STATUSES = Set.of("ACTIVE", "UNDER_REVIEW", "BANNED");

    // Accounts sit in two tables, so the filtering, sorting and paging happen here on
    // lightweight rows (a few queries in total); the per-account stats are only counted
    // for the accounts on the requested page.
    public Page<AdminAccountResponse> findAccounts(String role, String status, String search, Pageable pageable) {
        String roleFilter = normalise(role, ROLES, "role");
        String statusFilter = normalise(status, STATUSES, "status");
        String searchTerm = search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);

        Map<Integer, User> users = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
        Map<Integer, Long> reportCounts = new HashMap<>();
        for (Object[] row : reportRepository.countReportsPerReportedUser()) {
            reportCounts.put((Integer) row[0], (Long) row[1]);
        }

        List<AccountRow> rows = new ArrayList<>();
        if (roleFilter == null || roleFilter.equals("DONOR")) {
            for (Donor donor : donorRepository.findAll()) {
                User user = users.get(donor.getUserId());
                if (user != null) rows.add(new AccountRow(user, "DONOR", donor.getOrgName(),
                        donor.getSuburb(), donor.getRegion(), donor, null));
            }
        }
        if (roleFilter == null || roleFilter.equals("RECIPIENT")) {
            for (Recipient recipient : recipientRepository.findAll()) {
                User user = users.get(recipient.getUserId());
                if (user != null) rows.add(new AccountRow(user, "RECIPIENT", recipient.getName(),
                        recipient.getSuburb(), recipient.getRegion(), null, recipient));
            }
        }

        List<AccountRow> matching = rows.stream()
                .filter(row -> statusFilter == null || statusFilter.equals(accountStatus(row.user())))
                .filter(row -> searchTerm == null || row.matches(searchTerm))
                .sorted(Comparator
                        .comparingLong((AccountRow row) -> reportCounts.getOrDefault(row.user().getUserId(), 0L))
                        .reversed()
                        .thenComparing(AccountRow::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(row -> row.user().getUserId()))
                .toList();

        int from = (int) Math.min(pageable.getOffset(), matching.size());
        int to = Math.min(from + pageable.getPageSize(), matching.size());
        List<AdminAccountResponse> content = matching.subList(from, to).stream()
                .map(row -> toAccountResponse(row, reportCounts.getOrDefault(row.user().getUserId(), 0L)))
                .toList();
        return new PageImpl<>(content, pageable, matching.size());
    }

    private AdminAccountResponse toAccountResponse(AccountRow row, long reportsAgainst) {
        User user = row.user();
        long activityCount;
        long successfulCollections;
        long totalRequests;
        long completedCollections;
        if (row.donor() != null) {
            totalRequests = requestRepository.countTotalByDonorId(row.donor().getId());
            completedCollections = requestRepository.countCollectedByDonorId(row.donor().getId());
            activityCount = listingRepository.countByDonor_Id(row.donor().getId());
            successfulCollections = collectionHistoryRepository.countSuccessfulByDonorId(row.donor().getId());
        } else {
            totalRequests = requestRepository.countByRecipientId(row.recipient().getRecipientId());
            completedCollections = requestRepository.countCollectedByRecipientId(row.recipient().getRecipientId());
            activityCount = requestRepository.countByRecipientId(row.recipient().getRecipientId());
            successfulCollections = collectionHistoryRepository.countSuccessfulByRecipientId(row.recipient().getRecipientId());
        }
        return new AdminAccountResponse(
                user.getUserId(),
                row.role(),
                row.name(),
                user.getEmail(),
                user.getPhone(),
                row.suburb(),
                row.region(),
                accountStatus(user),
                reportsAgainst,
                activityCount,
                successfulCollections,
                banRecordRepository.countByUserId(user.getUserId()),
                banDaysRemaining(user.getUserId()),
                totalRequests,
                completedCollections
        );
    }

    private static String normalise(String value, Set<String> allowed, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalised = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalised)) {
            throw new IllegalArgumentException(
                    field + " must be one of " + String.join(", ", allowed.stream().sorted().toList())
            );
        }
        return normalised;
    }

    private record AccountRow(User user, String role, String name, String suburb, String region,
                              Donor donor, Recipient recipient) {
        boolean matches(String searchTerm) {
            return String.valueOf(user.getUserId()).contains(searchTerm)
                    || contains(name, searchTerm)
                    || contains(user.getEmail(), searchTerm)
                    || contains(suburb, searchTerm);
        }

        private static boolean contains(String value, String searchTerm) {
            return value != null && value.toLowerCase(Locale.ROOT).contains(searchTerm);
        }
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
