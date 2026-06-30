package com.example.world.config;

import com.example.world.cluster.service.EntityClusterMapper;
import com.example.world.cluster.service.EntityClusterService;
import com.example.world.service.EntityMapper;
import com.example.world.service.EntityService;
import com.example.world.service.RedisEntityService;
import com.example.world.service.RedisService;
import com.example.world.service.inmemory.InMemoryEntityService;
import com.example.world.service.inmemory.InMemoryRedisService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public RedisService redisService(InMemoryRedisService redisService) {
        return redisService;
    }

    @Bean
    public EntityService entityService(InMemoryEntityService entityService) {
        return entityService;
    }

    @Bean
    public EntityMapper entityMapper(EntityClusterMapper entityMapper) {
        return entityMapper;
    }

//    @Bean
//    public RedisService redisService(EntityClusterService redisService) {
//        return redisService;
//    }
//
//    @Bean
//    public EntityService entityService(RedisEntityService entityService) {
//        return entityService;
//    }
}
