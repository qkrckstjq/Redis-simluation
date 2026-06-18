package com.example.world.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class ByteTypeConverter {
    public static byte[] stringToByte(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }
    public static byte[] numToByte(Object i) {
        return String.valueOf(i).getBytes(StandardCharsets.UTF_8);
    }
}
