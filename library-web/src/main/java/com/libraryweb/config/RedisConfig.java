package com.libraryweb.config;

import com.alibaba.fastjson2.support.spring6.data.redis.GenericFastJsonRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * <p>使用 fastjson2 作为值序列化器，并开启类型信息写入与可信包白名单。
 */
@Configuration
public class RedisConfig {

    /**
     * 可信包白名单：仅允许这些包下的类进行反序列化
     */
    private static final String[] AUTO_TYPE_WHITE_LIST = {
            "com.librarycommon",
            "com.libraryadmin",
            "com.libraryweb",
            "java.util",
            "java.lang"
    };

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // fastjson2 Redis 序列化器：写入类型信息 + 白名单
        GenericFastJsonRedisSerializer jsonSerializer = new GenericFastJsonRedisSerializer(AUTO_TYPE_WHITE_LIST);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // key / hashKey 使用字符串序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value / hashValue 使用 JSON 序列化，并写入类型信息
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
