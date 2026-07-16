package com.example.world.entity.log.prometheus;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@Getter
@Setter
public class EntitiesMetrics {
    private final AtomicLong entitySize = new AtomicLong();

    public EntitiesMetrics(MeterRegistry meterRegistry) {
        Gauge.builder(
                "simulation.entity.metrics",
                entitySize,
                AtomicLong::get
        ).tag("type", "SIZE").register(meterRegistry);
    }

    public void setEntitySize(Long entitySize) {
        this.entitySize.set(entitySize);
    }
}
