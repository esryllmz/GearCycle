package com.gearcycle.device.domain.model;

import java.util.UUID;

public record DeviceId(UUID value) {

    public DeviceId {
        if (value == null) {
            throw new IllegalArgumentException("DeviceId value must not be null");
        }
    }

    public static DeviceId generate() {
        return new DeviceId(UUID.randomUUID());
    }

    public static DeviceId of(UUID value) {
        return new DeviceId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
