package com.team19.musuimsa.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Profile("dev")
@EnableCaching
@Configuration
public class CacheConfig {

    @Value("${cache.shelters.expire-after-write:120s}")
    private String sheltersExpireAfterWrite;

    @Value("${cache.shelters.maximum-size:2000}")
    private long sheltersMaximumSize;

    @Value("${cache.weather.expire-after-write}")
    private String weatherExpireAfterWrite;

    @Value("${cache.weather.maximum-size}")
    private long weatherMaximumSize;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache shelters = new CaffeineCache(
                "sheltersMap",
                Caffeine.newBuilder()
                        .expireAfterWrite(parse(sheltersExpireAfterWrite))
                        .maximumSize(sheltersMaximumSize)
                        .build()
        );

        CaffeineCache weather = new CaffeineCache(
                "weather",
                Caffeine.newBuilder()
                        .expireAfterWrite(parse(weatherExpireAfterWrite))
                        .maximumSize(weatherMaximumSize)
                        .build()
        );

        SimpleCacheManager manager = new SimpleCacheManager();
        List<Cache> caches = Arrays.asList(shelters, weather);
        manager.setCaches(caches);
        return manager;
    }

    private Duration parse(String s) {
        String v = s.trim().toLowerCase();

        if (v.startsWith("pt")) {
            return Duration.parse(v.toUpperCase());
        }
        if (v.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(v.replace("ms", "")));
        }
        if (v.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(v.replace("s", "")));
        }
        if (v.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(v.replace("m", "")));
        }
        if (v.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(v.replace("h", "")));
        }

        return Duration.ofSeconds(Long.parseLong(v));
    }
}