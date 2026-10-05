package com.libraryweb.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存操作工具类
 */
@Component
@RequiredArgsConstructor
public class RedisCacheUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 写入缓存并设置过期时间
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 读取缓存
     *
     * @param key  键
     * @param type 值类型
     * @param <T>  泛型
     * @return 缓存值，不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 删除缓存
     *
     * @param key 键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 为已存在的键续期
     *
     * @param key     键
     * @param timeout 新的过期时间
     * @param unit    时间单位
     * @return true 续期成功（键存在且已设置过期时间）
     */
    public boolean renew(String key, long timeout, TimeUnit unit) {
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(exists)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }
}
