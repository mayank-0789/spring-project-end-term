package com.eventbooking.exception;

public class BookingExpiredException extends RuntimeException {
    public BookingExpiredException(String bookingReference) {
        super("Booking " + bookingReference + " has expired");
    }
}
