package com.gearcycle.device.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices")
class DeviceJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 50, updatable = false)
    private String category;

    @Column(name = "lifecycle_status", nullable = false, length = 50)
    private String lifecycleStatus;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected DeviceJpaEntity() {
    }

    DeviceJpaEntity(UUID id, String category, String lifecycleStatus,
                    Instant receivedAt, String notes) {
        this.id = id;
        this.category = category;
        this.lifecycleStatus = lifecycleStatus;
        this.receivedAt = receivedAt;
        this.notes = notes;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    UUID getId() { return id; }
    String getCategory() { return category; }
    String getLifecycleStatus() { return lifecycleStatus; }
    Instant getReceivedAt() { return receivedAt; }
    String getNotes() { return notes; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
    Long getVersion() { return version; }

    void setLifecycleStatus(String lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }
    void setNotes(String notes) { this.notes = notes; }
}
