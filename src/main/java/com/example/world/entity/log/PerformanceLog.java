package com.example.world.entity.log;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@RequiredArgsConstructor
public class PerformanceLog {
    private long tick;
    private final PerformanceMetric metric;
}
