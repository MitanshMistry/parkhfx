package com.parkhfx.listing;

import java.util.List;
import java.util.UUID;

import com.parkhfx.listing.ListingDtos.ListingDetail;
import com.parkhfx.listing.ListingDtos.ListingSummary;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/listings")
public class ListingController {

    private final ListingService listings;

    public ListingController(ListingService listings) {
        this.listings = listings;
    }

    @GetMapping
    public List<ListingSummary> search(
            @RequestParam(required = false) Double minLongitude,
            @RequestParam(required = false) Double minLatitude,
            @RequestParam(required = false) Double maxLongitude,
            @RequestParam(required = false) Double maxLatitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double radiusKm) {
        return listings.search(
                minLongitude, minLatitude, maxLongitude, maxLatitude,
                longitude, latitude, radiusKm);
    }

    @GetMapping("/{id}")
    public ListingDetail detail(@PathVariable UUID id) {
        return listings.detail(id);
    }
}
