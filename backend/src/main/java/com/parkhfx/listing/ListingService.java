package com.parkhfx.listing;

import java.util.List;
import java.util.UUID;

import com.parkhfx.listing.ListingDtos.ListingDetail;
import com.parkhfx.listing.ListingDtos.ListingSummary;

import org.springframework.stereotype.Service;

@Service
public class ListingService {

    private final ListingRepository listings;

    public ListingService(ListingRepository listings) {
        this.listings = listings;
    }

    public List<ListingSummary> search(
            Double minLongitude, Double minLatitude,
            Double maxLongitude, Double maxLatitude,
            Double longitude, Double latitude, Double radiusKm) {
        boolean hasBounds = minLongitude != null || minLatitude != null
                || maxLongitude != null || maxLatitude != null;
        boolean hasRadius = longitude != null || latitude != null || radiusKm != null;

        if (hasBounds && hasRadius) {
            throw new IllegalArgumentException("Use either map bounds or radius search, not both.");
        }
        if (hasBounds) {
            if (minLongitude == null || minLatitude == null
                    || maxLongitude == null || maxLatitude == null) {
                throw new IllegalArgumentException("All four map bounds parameters are required.");
            }
            return listings.findInBounds(
                    minLongitude, minLatitude, maxLongitude, maxLatitude);
        }
        if (hasRadius) {
            if (longitude == null || latitude == null || radiusKm == null) {
                throw new IllegalArgumentException(
                        "longitude, latitude, and radiusKm are required for radius search.");
            }
            return listings.findNear(longitude, latitude, radiusKm * 1000.0);
        }
        return listings.findAll();
    }

    public ListingDetail detail(UUID id) {
        return listings.findDetail(id).orElseThrow(ListingNotFoundException::new);
    }
}
