package com.example.world.util;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Component
public class ByteTypeConverter {
    public static byte[] stringToByte(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }
    public static byte[] IntegerToByte(Integer i) {
        return String.valueOf(i).getBytes(StandardCharsets.UTF_8);
    }
}
