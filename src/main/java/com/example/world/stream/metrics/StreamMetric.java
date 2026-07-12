package com.example.world.stream.metrics;

import com.example.world.entity.StateEnum;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class StreamMetric {
    private final Counter spawn;
    private final Counter hunt;

    public StreamMetric(MeterRegistry meterRegistry) {
        spawn = meterRegistry.counter("simulation.stream.metrics", "state", "SPAWN");
        hunt = meterRegistry.counter("simulation.stream.metrics", "state", "HUNT");
    }

    public void increment(String state) {
        StateEnum stateEnum = StateEnum.valueOf(state);
        switch (stateEnum) {
            case SPAWN:
                spawn.increment();
            case ATTACK:
                hunt.increment();
        }
    }
}
