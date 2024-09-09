package dev.crashteam.uzumdatascrapper.service.integration.interceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthHeaderRequestService {

    private final static String AUTH_TOKEN = "UZUM_AUTH_TOKEN";

    private final RedisTemplate<String, String> redisTemplate;

    public String getCachedToken() {
       return redisTemplate.opsForValue().get(AUTH_TOKEN);
    }

}
