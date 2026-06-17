package com.example.world.util;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandUtil {
    private static final Random random = new Random();

    public static int getIntRand(int min, int max) {
        return random.nextInt(min, max);
    }

    public static boolean percent(int percent) {
        return random.nextInt(0, 99) < percent;
    }
}
