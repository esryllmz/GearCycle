package com.gearcycle.device.domain.model;

import com.gearcycle.device.domain.exception.InvalidDeviceStateException;
import com.gearcycle.device.domain.exception.InvalidLifecycleTransitionException;

import java.time.Instant;

public final class Device {

    private final DeviceId id;
    private final DeviceCategory category;
    private DeviceLifecycleStatus status;
    private final Instant receivedAt;
    private String notes;

    private Device(DeviceId id, DeviceCategory category, DeviceLifecycleStatus status,
                   Instant receivedAt, String notes) {
        this.id = id;
        this.category = category;
        this.status = status;
        this.receivedAt = receivedAt;
        this.notes = notes;
    }

    public static Device intake(DeviceCategory category) {
        if (category == null) {
            throw new InvalidDeviceStateException("Device category must not be null");
        }
        return new Device(DeviceId.generate(), category, DeviceLifecycleStatus.INTAKEN,
                Instant.now(), null);
    }

    public static Device reconstitute(DeviceId id, DeviceCategory category,
                                      DeviceLifecycleStatus status, Instant receivedAt,
                                      String notes) {
        return new Device(id, category, status, receivedAt, notes);
    }

    public void transitionTo(DeviceLifecycleStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidDeviceStateException("Target lifecycle status must not be null");
        }
        if (status == DeviceLifecycleStatus.READY_FOR_RESALE
                && newStatus == DeviceLifecycleStatus.AWAITING_INSPECTION) {
            throw new InvalidLifecycleTransitionException(status, newStatus,
                    "Use reopenForInspection(String reason) — a mandatory reason is required for this transition.");
        }
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidLifecycleTransitionException(status, newStatus);
        }
        this.status = newStatus;
    }

    public void reopenForInspection(String reason) {
        if (status != DeviceLifecycleStatus.READY_FOR_RESALE) {
            throw new InvalidLifecycleTransitionException(status, DeviceLifecycleStatus.AWAITING_INSPECTION,
                    "reopenForInspection() may only be called when the device is in READY_FOR_RESALE status.");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidDeviceStateException("A non-blank reason is required to reopen a device for inspection.");
        }
        this.notes = reason;
        this.status = DeviceLifecycleStatus.AWAITING_INSPECTION;
    }

    public DeviceId getId() {
        return id;
    }

    public DeviceCategory getCategory() {
        return category;
    }

    public DeviceLifecycleStatus getStatus() {
        return status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getNotes() {
        return notes;
    }
}
