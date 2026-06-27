package com.gearcycle.device.infrastructure.persistence;

import com.gearcycle.device.domain.model.Device;
import com.gearcycle.device.domain.model.DeviceCategory;
import com.gearcycle.device.domain.model.DeviceId;
import com.gearcycle.device.domain.model.DeviceLifecycleStatus;
import com.gearcycle.device.domain.port.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static java.time.temporal.ChronoUnit.SECONDS;

@SpringBootTest
@Testcontainers
class JpaDeviceRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private SpringDataDeviceJpaRepository springDataRepo;

    @Test
    void save_persistsNewDevice_andCanBeRetrievedById() {
        Device device = Device.intake(DeviceCategory.SMARTPHONE);

        deviceRepository.save(device);

        Optional<Device> found = deviceRepository.findById(device.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(device.getId());
        assertThat(found.get().getCategory()).isEqualTo(DeviceCategory.SMARTPHONE);
        assertThat(found.get().getStatus()).isEqualTo(DeviceLifecycleStatus.INTAKEN);
        assertThat(found.get().getNotes()).isNull();
    }

    @Test
    void save_populatesCreatedAtAndUpdatedAt() {
        Instant before = Instant.now();
        Device device = Device.intake(DeviceCategory.LAPTOP);

        deviceRepository.save(device);

        DeviceJpaEntity entity = springDataRepo.findById(device.getId().value()).orElseThrow();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isCloseTo(before, within(5, SECONDS));
        assertThat(entity.getUpdatedAt()).isCloseTo(before, within(5, SECONDS));
    }

    @Test
    void save_setsVersionToZeroOnFirstPersist() {
        Device device = Device.intake(DeviceCategory.TABLET);

        deviceRepository.save(device);

        DeviceJpaEntity entity = springDataRepo.findById(device.getId().value()).orElseThrow();
        assertThat(entity.getVersion()).isZero();
    }

    @Test
    void save_incrementsVersionOnUpdate() {
        Device device = Device.intake(DeviceCategory.SMARTPHONE);
        deviceRepository.save(device);

        device.transitionTo(DeviceLifecycleStatus.INSPECTED);
        deviceRepository.save(device);

        DeviceJpaEntity entity = springDataRepo.findById(device.getId().value()).orElseThrow();
        assertThat(entity.getVersion()).isEqualTo(1L);
    }

    @Test
    void save_updatesUpdatedAtOnUpdate() {
        Device device = Device.intake(DeviceCategory.LAPTOP);
        deviceRepository.save(device);
        DeviceJpaEntity before = springDataRepo.findById(device.getId().value()).orElseThrow();
        Instant createdAt = before.getCreatedAt();

        device.transitionTo(DeviceLifecycleStatus.INSPECTED);
        deviceRepository.save(device);

        DeviceJpaEntity after = springDataRepo.findById(device.getId().value()).orElseThrow();
        assertThat(after.getCreatedAt()).isEqualTo(createdAt);
        assertThat(after.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

    @Test
    void findById_returnsEmpty_whenDeviceDoesNotExist() {
        Optional<Device> result = deviceRepository.findById(DeviceId.generate());

        assertThat(result).isEmpty();
    }

    @Test
    void save_persistsLifecycleTransition() {
        Device device = Device.intake(DeviceCategory.TABLET);
        deviceRepository.save(device);

        device.transitionTo(DeviceLifecycleStatus.INSPECTED);
        deviceRepository.save(device);

        Optional<Device> found = deviceRepository.findById(device.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(DeviceLifecycleStatus.INSPECTED);
    }
}
