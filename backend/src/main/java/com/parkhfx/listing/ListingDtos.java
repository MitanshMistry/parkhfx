package com.parkhfx.listing;

import java.util.List;
import java.util.UUID;

public final class ListingDtos {

    private ListingDtos() {
    }

    public record ListingSummary(
            UUID id,
            String title,
            String address,
            String neighborhood,
            int hourlyRateCents,
            int dailyMaxCents,
            String vehicleSize,
            boolean covered,
            boolean evCharger,
            double latitude,
            double longitude,
            String photoUrl) {
    }

    public record AvailabilityWindow(int dayOfWeek, String startTime, String endTime) {
    }

    public record ListingDetail(
            UUID id,
            String title,
            String description,
            String address,
            String neighborhood,
            int hourlyRateCents,
            int dailyMaxCents,
            String vehicleSize,
            boolean covered,
            boolean evCharger,
            double latitude,
            double longitude,
            String hostName,
            List<String> photoUrls,
            List<AvailabilityWindow> availability) {
    }
}
