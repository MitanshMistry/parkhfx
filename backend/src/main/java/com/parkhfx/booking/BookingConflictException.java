package com.parkhfx.booking;

/** Thrown when the requested time range overlaps an existing active booking or hold. */
public class BookingConflictException extends RuntimeException {

    public BookingConflictException() {
        super("That spot is already booked for part of the requested time.");
    }
}
