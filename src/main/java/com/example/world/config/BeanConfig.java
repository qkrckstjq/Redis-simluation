package com.example.world.config;

import com.example.world.cluster.service.EntityClusterMapper;
import com.example.world.entity.log.PerformanceLog;
import com.example.world.entity.log.PerformanceMetric;
import com.example.world.service.EntityMapper;
import com.example.world.service.EntityService;
import com.example.world.service.RedisService;
import com.example.world.service.inmemory.InMemoryEntityService;
import com.example.world.service.inmemory.InMemoryRedisService;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
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

    @Bean
    public PerformanceMetric performanceMetric() {
        return new PerformanceMetric();
    }

    @Bean
    public PerformanceLog performanceLog(PerformanceMetric performanceMetric) {
        return new PerformanceLog(performanceMetric);
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }
}
