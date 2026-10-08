package foodbank.webapp.service;

import foodbank.webapp.dto.DonorSettingsResponse;
import foodbank.webapp.dto.UpdateDonorSettingsRequest;
import foodbank.webapp.entity.Donor;
import foodbank.webapp.repositories.DonorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class DonorSettingsService {

    private final DonorRepository donorRepository;

    public DonorSettingsResponse getSettings(Integer donorId) {
        Donor donor = donorRepository
                .findById(donorId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Donor not found"
                        )
                );

        return new DonorSettingsResponse(
                donor.getId(),
                donor.getHideContact()
        );
    }

    @Transactional
    public DonorSettingsResponse updateSettings(
            Integer donorId,
            UpdateDonorSettingsRequest request
    ) {
        Donor donor = donorRepository
                .findById(donorId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Donor not found"
                        )
                );

        donor.setHideContact(request.hideContact());

        donorRepository.save(donor);

        return new DonorSettingsResponse(
                donor.getId(),
                donor.getHideContact()
        );
    }
}