package foodbank.webapp.service;


import foodbank.webapp.dto.ListingResponse;
import foodbank.webapp.dto.TimeSlot;
import foodbank.webapp.entity.Listing;
import foodbank.webapp.repositories.ListingTagRepository;
import foodbank.webapp.repositories.ListingRepository;
import foodbank.webapp.repositories.TimeslotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListingService {
    private final ListingRepository listingRepostory;
    private final ListingTagRepository listingTagRepository;
    private final TimeslotRepository timeslotRepository;

    public Page<Listing> findAllListing(Pageable pageable){
        return listingRepostory.findAll(pageable);
    }

    public Page<ListingResponse> findAllListingResponse(Pageable pageable){
        return findAllListing(pageable).map(listing -> new ListingResponse(
                listing.getCategory(),
                listing.getNotes(),
                listing.getCategory(),
                listing.getExpiryDate() == null ? null : listing.getExpiryDate().toString(),
                listing.getCollectionDeadline() == null ? null : listing.getCollectionDeadline().toString(),
                listingTagRepository.findByListing(listing).stream()
                        .map(listingTag -> listingTag.getTag().getName())
                        .toArray(String[]::new),
                timeslotRepository.findByListing(listing).stream()
                        .map(timeslot -> new TimeSlot(
                                timeslot.getStartTime().toString(),
                                timeslot.getEndTime().toString(),
                                timeslot.getRemainingSlot()
                        ))
                        .toArray(TimeSlot[]::new)
        ));
    }
}
