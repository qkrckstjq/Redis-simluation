package com.example.world.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Tick {
    String type;
    long tick;
    List<EntitySnapshotDto> entities;

    public Tick(
            String type,
            long tick,
            List<EntitySnapshotDto> entities
    ) {
        this.type = type;
        this.tick = tick;
        this.entities = entities;
    }
}
