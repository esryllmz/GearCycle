package com.gearcycle.device.domain.port;

import com.gearcycle.device.domain.model.Device;
import com.gearcycle.device.domain.model.DeviceId;

import java.util.Optional;

public interface DeviceRepository {

    Device save(Device device);

    Optional<Device> findById(DeviceId id);
}
