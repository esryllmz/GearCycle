package com.gearcycle.device.domain.exception;

import com.gearcycle.device.domain.model.DeviceLifecycleStatus;

public class InvalidLifecycleTransitionException extends RuntimeException {

    public InvalidLifecycleTransitionException(DeviceLifecycleStatus from, DeviceLifecycleStatus to) {
        super(String.format("Invalid lifecycle transition: %s -> %s", from, to));
    }

    public InvalidLifecycleTransitionException(DeviceLifecycleStatus from, DeviceLifecycleStatus to, String detail) {
        super(String.format("Invalid lifecycle transition: %s -> %s. %s", from, to, detail));
    }
}
