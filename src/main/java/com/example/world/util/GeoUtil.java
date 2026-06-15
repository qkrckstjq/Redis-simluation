package com.example.world.util;

import com.example.world.entity.RedisEntity;
import org.springframework.stereotype.Component;

@Component
public class GeoUtil {
    private static final double METERS_PER_DEGREE = 111_000.0;
    public static final int MIN_COORDINATE = 0;
    public static final int MAX_COORDINATE = 256;

    public static int setCoordinate(int num) {
        if(num < MIN_COORDINATE) return MIN_COORDINATE;
        return Math.min(num, MAX_COORDINATE);
    }

    public static double scaleIn(double num) {
        return num / METERS_PER_DEGREE;
    }

    public static double getDist(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }
}
