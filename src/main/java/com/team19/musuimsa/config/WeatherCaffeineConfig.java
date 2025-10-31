package com.team19.musuimsa.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@EnableCaching
@Configuration
public class WeatherCaffeineConfig {

    @Value("${cache.weather.expire-after-write}")
    private Duration weatherExpireAfterWrite;

    @Value("${cache.weather.maximum-size}")
    private long weatherMaximumSize;

    @Bean(name = "caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCache weather = new CaffeineCache(
                "weather",
                Caffeine.newBuilder()
                        .expireAfterWrite(weatherExpireAfterWrite)
                        .maximumSize(weatherMaximumSize)
                        .build()
        );
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(weather));
        return manager;
    }
}
