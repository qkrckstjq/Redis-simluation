package com.example.world.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class LuaConfig {

    @Bean
    public String updateEntitySha(StringRedisTemplate redisTemplate) throws IOException {

        Resource resource = new ClassPathResource("lua/update_entity.lua");

        byte[] script;

        try (InputStream is = resource.getInputStream()) {
            script = is.readAllBytes();
        }

        return redisTemplate.execute((RedisCallback<String>) connection ->
                connection.scriptingCommands().scriptLoad(script)
        );
    }

    @Bean
    public String createEntitySha(StringRedisTemplate redisTemplate) throws IOException {

        Resource resource = new ClassPathResource("lua/create_entity.lua");

        byte[] script;

        try (InputStream is = resource.getInputStream()) {
            script = is.readAllBytes();
        }

        return redisTemplate.execute((RedisCallback<String>) connection ->
                connection.scriptingCommands().scriptLoad(script)
        );
    }

    @Bean
    public String spawnEntitySha(StringRedisTemplate redisTemplate) throws IOException {

        Resource resource = new ClassPathResource("lua/spawn_entity.lua");

        byte[] script;

        try (InputStream is = resource.getInputStream()) {
            script = is.readAllBytes();
        }

        return redisTemplate.execute((RedisCallback<String>) connection ->
                connection.scriptingCommands().scriptLoad(script)
        );
    }
}
