package io.github.joicepassos.billing.shared.web;

/** Becomes a 404 Problem Details. Also used for resources of other tenants, so their existence is never revealed. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " " + id + " was not found");
    }
}
