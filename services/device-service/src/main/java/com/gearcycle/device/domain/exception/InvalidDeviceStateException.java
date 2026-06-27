package com.gearcycle.device.domain.exception;

public class InvalidDeviceStateException extends RuntimeException {

    public InvalidDeviceStateException(String message) {
        super(message);
    }
}
