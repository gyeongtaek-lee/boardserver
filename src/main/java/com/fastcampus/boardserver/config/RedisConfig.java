package com.fastcampus.boardserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPwd;

    @Value("${expire.defaultTime}")
    private long defaultExpireSecond;

    /**
     * 역직렬화를 위해 ObjectMapper 사용.
     * @return
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // timestamp를 설정하지 못하도록 처리
        mapper.registerModules(new JavaTimeModule(), new Jdk8Module()); // 자바의 날짜값을 인식하도록 설정, jdk8에서 추가된 라이브러리임
        return mapper;
    }

    /**
     * connection facotory 패턴을 이용해서 미리 커넥션을 만들어 놓아 연결 비용을 줄인다.
     * Lettuce가 비동기로 더 빠른 성능
     * @return
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setPort(redisPort);
        redisStandaloneConfiguration.setHostName(redisHost);
        redisStandaloneConfiguration.setPassword(redisPwd);
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration);
        return lettuceConnectionFactory;
    }

    /**
     * 직렬화, 역직렬화 필요.
     * @param redisConnectionFactory
     * @param objectMapper
     * @return
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory,
                                               ObjectMapper objectMapper) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()     // null을 허용하지 않겠다.
                .entryTtl(Duration.ofSeconds(defaultExpireSecond))  // 만료시간(ttl)
                .serializeKeysWith(             // key 직렬화 설정
                        RedisSerializationContext.SerializationPair     // 두가지 변수를 한가지 변수로 합해주는 것
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(           // value 직렬화 설정
                        RedisSerializationContext.SerializationPair     // 두가지 변수를 한가지 변수로 합해주는 것
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(configuration).build();
    }
}