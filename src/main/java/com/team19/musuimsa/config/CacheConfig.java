package com.team19.musuimsa.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@EnableCaching
@Configuration
public class CacheConfig {

    @Value("${cache.weather.expire-after-write}")
    private String weatherExpireAfterWrite;

    @Value("${cache.weather.maximum-size}")
    private long weatherMaximumSize;

    @Bean
    public CacheManager cacheManager() {
        Duration expiryDuration = Duration.parse("PT" + weatherExpireAfterWrite.toUpperCase());

        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .expireAfterWrite(expiryDuration)
                .maximumSize(weatherMaximumSize);

        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(caffeine);

        return manager;
    }
}