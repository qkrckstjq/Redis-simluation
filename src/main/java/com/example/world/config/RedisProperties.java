package com.example.world.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "spring.data.redis")
@Configuration
@Getter
@Setter
public class RedisProperties {
    private String port;
    private String host;
}
