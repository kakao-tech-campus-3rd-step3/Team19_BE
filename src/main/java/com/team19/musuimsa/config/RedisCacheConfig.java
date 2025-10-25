package com.team19.musuimsa.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Profile("prod")
@EnableCaching
@Configuration
public class RedisCacheConfig {

    @Value("${cache.shelters.expire-after-write}")
    private Duration sheltersExpireAfterWrite;

    @Value("${cache.weather.expire-after-write}")
    private Duration weatherExpireAfterWrite;


    @Bean
    public CacheManager cacheManager(RedisConnectionFactory cf, ObjectMapper baseMapper) {
        ObjectMapper mapper = baseMapper.copy()
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(json))
                .prefixCacheNameWith("musuimsa::");

        Map<String, RedisCacheConfiguration> conf = new HashMap<String, RedisCacheConfiguration>();
        conf.put("sheltersMap", base.entryTtl(sheltersExpireAfterWrite));
        conf.put("weather", base.entryTtl(weatherExpireAfterWrite));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(conf)
                .build();
    }
}
