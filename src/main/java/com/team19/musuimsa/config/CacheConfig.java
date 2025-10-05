package com.team19.musuimsa.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {

    @Value("${spring.cache.caffeine.expire-after-write}")
    private String expireAfterWrite;

    @Value("${spring.cache.caffeine.maximum-size}")
    private long maximumSize;

    @Bean
    public CacheManager cacheManager() {
        Duration expiryDuration = Duration.parse("PT" + expireAfterWrite.toUpperCase());

        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .expireAfterWrite(expiryDuration)
                .maximumSize(maximumSize);

        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(caffeine);

        return manager;
    }
}