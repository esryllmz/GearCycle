package com.gearcycle.device.infrastructure.persistence;

import com.gearcycle.device.domain.model.Device;
import com.gearcycle.device.domain.model.DeviceCategory;
import com.gearcycle.device.domain.model.DeviceId;
import com.gearcycle.device.domain.model.DeviceLifecycleStatus;
import com.gearcycle.device.domain.port.DeviceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaDeviceRepository implements DeviceRepository {

    private final SpringDataDeviceJpaRepository springDataRepo;

    public JpaDeviceRepository(SpringDataDeviceJpaRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Device save(Device device) {
        UUID id = device.getId().value();

        DeviceJpaEntity entity = springDataRepo.findById(id)
                .orElseGet(() -> new DeviceJpaEntity(
                        id,
                        device.getCategory().name(),
                        device.getStatus().name(),
                        device.getReceivedAt(),
                        device.getNotes()));

        entity.setLifecycleStatus(device.getStatus().name());
        entity.setNotes(device.getNotes());

        springDataRepo.save(entity);
        return device;
    }

    @Override
    public Optional<Device> findById(DeviceId id) {
        return springDataRepo.findById(id.value())
                .map(this::toDomain);
    }

    private Device toDomain(DeviceJpaEntity entity) {
        return Device.reconstitute(
                new DeviceId(entity.getId()),
                DeviceCategory.valueOf(entity.getCategory()),
                DeviceLifecycleStatus.valueOf(entity.getLifecycleStatus()),
                entity.getReceivedAt(),
                entity.getNotes());
    }
}
