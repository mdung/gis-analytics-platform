package com.example.gis.service;

import com.example.gis.entity.Device;
import com.example.gis.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public List<Device> findAll() {
        return deviceRepository.findAll();
    }

    public Device findById(UUID id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found"));
    }

    @Transactional
    public Device updatePosition(String code, double lng, double lat) {
        Device device = deviceRepository.findByCodeAndDeletedAtIsNull(code)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        
        Point point = geometryFactory.createPoint(
                new org.locationtech.jts.geom.Coordinate(lng, lat)
        );
        device.setLastPosition(point);
        device.setUpdatedAt(java.time.OffsetDateTime.now());
        
        return deviceRepository.save(device);
    }
}

