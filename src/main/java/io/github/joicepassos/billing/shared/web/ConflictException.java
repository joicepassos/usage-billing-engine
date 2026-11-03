package io.github.joicepassos.billing.shared.web;

/** Becomes a 409 Problem Details. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
