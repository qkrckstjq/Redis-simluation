package com.example.world.entity;

import com.example.world.entity.log.PerformanceMetric;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Tick {
    String type;
    long tick;
    List<EntitySnapshotDto> entities;
    PerformanceMetric metric;

    public Tick(
            String type,
            long tick,
            List<EntitySnapshotDto> entities,
            PerformanceMetric metric
    ) {
        this.type = type;
        this.tick = tick;
        this.entities = entities;
        this.metric = metric;
    }
}
