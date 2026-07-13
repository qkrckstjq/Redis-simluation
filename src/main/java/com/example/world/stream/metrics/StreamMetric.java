package com.example.world.stream.metrics;

import com.example.world.entity.StateEnum;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class StreamMetric {
    private final Counter spawn;
    private final Counter hunt;
    private final Counter processError;
    private final Counter pendingError;
    private final Counter dlqError;

    public StreamMetric(MeterRegistry meterRegistry) {
        spawn = meterRegistry.counter("simulation.stream.metrics", "state", "SPAWN");
        hunt = meterRegistry.counter("simulation.stream.metrics", "state", "HUNT");
        processError = meterRegistry.counter("simulation.stream.error", "type", "PROCESS");
        pendingError = meterRegistry.counter("simulation.stream.error", "type", "PENDING");
        dlqError = meterRegistry.counter("simulation.stream.error", "type", "DLQ");
    }

    public void incrementState(String state) {
        StateEnum stateEnum = StateEnum.valueOf(state);
        switch (stateEnum) {
            case SPAWN:
                spawn.increment();
            case ATTACK:
                hunt.increment();
        }
    }

    public void incrementProcessError(){
        processError.increment();
    }

    public void incrementPendingError(){
        pendingError.increment();
    }

    public void incrementDqlError() {
        dlqError.increment();
    }
}
