package com.eventbooking.exception;

public class InsufficientTicketsException extends RuntimeException {
    public InsufficientTicketsException(int available, int requested) {
        super("Not enough tickets. Available: " + available + ", Requested: " + requested);
    }
}
