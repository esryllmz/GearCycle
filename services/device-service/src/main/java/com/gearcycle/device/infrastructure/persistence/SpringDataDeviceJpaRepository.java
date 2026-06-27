package com.gearcycle.device.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataDeviceJpaRepository extends JpaRepository<DeviceJpaEntity, UUID> {
}
