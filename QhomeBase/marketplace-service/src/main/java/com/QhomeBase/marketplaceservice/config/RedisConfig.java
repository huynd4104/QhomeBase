package com.QhomeBase.marketplaceservice.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// Temporarily disabled due to Lettuce version compatibility issue
/*
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
*/

@Configuration
@EnableCaching
public class RedisConfig {

    // Temporarily disabled due to Lettuce version compatibility issue
    /*
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
    */

    @Bean
    public CacheManager cacheManager() {
        // Temporarily use in-memory cache instead of Redis
        return new ConcurrentMapCacheManager(
                "categories",
                "popularPosts",
                "postList",
                "postDetails",
                "userLikes",
                "residentInfo"
        );
    }
    
    /*
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)) // Default TTL: 5 minutes
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Categories cache - rarely change, cache for 1 hour
        cacheConfigurations.put("categories", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Popular posts cache - update every 15 minutes
        cacheConfigurations.put("popularPosts", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Post details cache - 5 minutes
        cacheConfigurations.put("postDetails", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // User likes cache - 10 minutes
        cacheConfigurations.put("userLikes", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Post list cache - 2 minutes (shorter for freshness)
        cacheConfigurations.put("postList", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
    */
}

