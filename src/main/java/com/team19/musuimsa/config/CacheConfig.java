package com.team19.musuimsa.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@EnableCaching
@Configuration
public class CacheConfig {

    @Value("${cache.shelters.expire-after-write}")
    private Duration sheltersExpireAfterWrite;

    @Value("${cache.shelters.maximum-size:2000}")
    private long sheltersMaximumSize;

    @Value("${cache.weather.expire-after-write}")
    private Duration weatherExpireAfterWrite;

    @Value("${cache.weather.maximum-size}")
    private long weatherMaximumSize;

    @Bean(name = "caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCache shelters = new CaffeineCache(
                "sheltersMap",
                Caffeine.newBuilder()
                        .expireAfterWrite(sheltersExpireAfterWrite)
                        .maximumSize(sheltersMaximumSize)
                        .build()
        );

        CaffeineCache weather = new CaffeineCache(
                "weather",
                Caffeine.newBuilder()
                        .expireAfterWrite(weatherExpireAfterWrite)
                        .maximumSize(weatherMaximumSize)
                        .build()
        );

        SimpleCacheManager manager = new SimpleCacheManager();
        List<Cache> caches = Arrays.asList(shelters, weather);
        manager.setCaches(caches);
        return manager;
    }
}