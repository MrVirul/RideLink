package com.ridelink.account_service.exception;

public class IncorrectOldPasswordException extends RuntimeException {

    public IncorrectOldPasswordException(String message) {
        super(message);
    }
}
