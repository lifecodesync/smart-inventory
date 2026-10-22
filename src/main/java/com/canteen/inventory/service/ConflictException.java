package com.canteen.inventory.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Becomes HTTP 409 Conflict: the request is valid but clashes with current data. */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
