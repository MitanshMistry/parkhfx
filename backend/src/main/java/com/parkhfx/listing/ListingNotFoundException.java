package com.parkhfx.listing;

public class ListingNotFoundException extends RuntimeException {

    public ListingNotFoundException() {
        super("Listing not found.");
    }
}
