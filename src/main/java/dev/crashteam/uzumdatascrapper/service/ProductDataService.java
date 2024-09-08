package dev.crashteam.uzumdatascrapper.service;


import dev.crashteam.uzumdatascrapper.model.ProductCacheData;
import dev.crashteam.uzumdatascrapper.model.RedisKey;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProductDataService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, ProductCacheData> hashOperations;

    public ProductDataService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }

    public boolean save(Long productId) {
        return hashOperations.putIfAbsent(RedisKey.UZUM_PRODUCT.getKey(), String.valueOf(productId), new ProductCacheData(productId, LocalDateTime.now()));
    }

    public void delete() {
        redisTemplate.delete(RedisKey.UZUM_PRODUCT.getKey());
    }

}
