package dev.crashteam.uzumdatascrapper.service.integration.interceptor;

import dev.crashteam.uzumdatascrapper.model.ProxyRequestParams;
import dev.crashteam.uzumdatascrapper.model.StyxProxyResult;
import dev.crashteam.uzumdatascrapper.service.integration.StyxProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthHeaderRequestService {

    private final static String AUTH_TOKEN = "UZUM_AUTH_TOKEN";

    private final static String CHECK_AUTH_TOKEN_URL = "https://api.uzum.uz/api/user/permittedToCheckout";

    private final static String GET_AUTH_TOKEN_URL = "https://id.uzum.uz/api/auth/token";

    private final RedisTemplate<String, String> redisTemplate;

    private final RestTemplate restTemplate;

    private final StyxProxyService proxyService;

    public String getCachedToken() {
        String authToken = redisTemplate.opsForValue().get(AUTH_TOKEN);
        boolean isAuthTokenValid = checkAuthToken(authToken);
        if (isAuthTokenValid) {
            return "Bearer %s".formatted(authToken);
        } else {
            String newAuthToken = getAuthToken();
            redisTemplate.opsForValue().set(AUTH_TOKEN, newAuthToken);
            return "Bearer %s".formatted(newAuthToken);
        }
    }

    private String getAuthToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.add("Referer", "https://id.uzum.uz/ru/security");
        headers.add("Content-Type", "application/json");
        headers.add("Host", "id.uzum.uz");
        headers.add("Accept-Language", "ru");
        headers.add("Accept-Encoding", "gzip, deflate, br");
        headers.add("Connection", "keep-alive");

        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                GET_AUTH_TOKEN_URL, HttpMethod.POST, httpEntity, String.class);
        HttpHeaders responseHeaders = responseEntity.getHeaders();
        String responseCookie = responseHeaders.getFirst(HttpHeaders.SET_COOKIE);

        return extractAccessToken(responseCookie);
    }

    private boolean checkAuthToken(String authToken) {
        ProxyRequestParams.ContextValue headers = ProxyRequestParams.ContextValue.builder()
                .key("headers")
                .value(Map.of("Authorization", "Bearer %s".formatted(authToken))).build();
        Random randomTimeout = new Random();
        ProxyRequestParams requestParams = ProxyRequestParams.builder()
                .timeout(randomTimeout.nextLong(50L, 500L))
                .url(CHECK_AUTH_TOKEN_URL)
                .httpMethod(HttpMethod.GET.name())
                .context(Collections.singletonList(headers))
                .build();
        StyxProxyResult<?> proxyResult = proxyService.getProxyResult(requestParams, new ParameterizedTypeReference<>() {
        });
        return proxyResult.getOriginalStatus().equals(HttpStatus.OK.value());
    }

    private String extractAccessToken(String cookieHeader) {
        int start = cookieHeader.indexOf("access_token=");
        if (start != -1) {
            int end = cookieHeader.indexOf(";", start);
            if (end != -1) {
                return cookieHeader.substring(start + "access_token=".length(), end);
            } else {
                return cookieHeader.substring(start + "access_token=".length());
            }
        }
        return null;
    }
}
