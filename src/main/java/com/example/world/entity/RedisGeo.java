package com.example.world.entity;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RedisGeo {
    private double x;
    private double y;
    private String key;

    public RedisGeo(double x, double y, String key) {
        this.x = x;
        this.y = y;
        this.key = key;
    }
}
