package com.example.world.entity.log;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceLog {
    private long tick;
    private PerformanceMetric metric;

    public PerformanceLog(PerformanceMetric performanceMetric) {
        this.metric = performanceMetric;
    }
}
