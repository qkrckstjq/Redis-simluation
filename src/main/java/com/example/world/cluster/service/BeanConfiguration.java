package com.example.world.cluster.service;

import com.example.world.service.EntityMapper;
import com.example.world.service.RedisService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {
    @Bean
    public EntityMapper entityMapper(EntityClusterMapper entityMapper) {
        return entityMapper;
    }

    @Bean
    public RedisService redisService(EntityClusterService redisService) {
        return redisService;
    }
}
