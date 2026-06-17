package com.example.world.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@ConfigurationProperties(prefix = "spring.data.redis")
@Configuration
@Getter
@Setter
public class RedisProperties {
    private String port;
    private String host;
    private Cluster cluster;
    private String password;

    @Getter
    @Setter
    static class Cluster {
        private List<String> nodes;
    }
}

