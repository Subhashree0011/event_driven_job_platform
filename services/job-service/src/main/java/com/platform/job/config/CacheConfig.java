package com.platform.job.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Spring Cache Manager backed by Redis.
 * Different TTLs for different cache regions:
 * - job-search: 60 seconds (frequently changing)
 * - job-detail: 300 seconds (less volatile)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Duration defaultTtl = Objects.requireNonNull(Duration.ofMinutes(5), "Default TTL must not be null");
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("job-search",
                defaultConfig.entryTtl(Objects.requireNonNull(Duration.ofSeconds(60))));
        cacheConfigurations.put("job-detail",
                defaultConfig.entryTtl(Objects.requireNonNull(Duration.ofSeconds(300))));
        cacheConfigurations.put("company",
                defaultConfig.entryTtl(Objects.requireNonNull(Duration.ofMinutes(30))));

        return RedisCacheManager.builder(Objects.requireNonNull(connectionFactory, "connectionFactory must not be null"))
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
