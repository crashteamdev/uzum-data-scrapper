package dev.crashteam.uzumdatascrapper.job;

import dev.crashteam.uzumdatascrapper.model.ProxyRequestParams;
import dev.crashteam.uzumdatascrapper.model.StyxProxyResult;
import dev.crashteam.uzumdatascrapper.service.integration.StyxProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenHandlerJob {

    private final static String AUTH_TOKEN = "UZUM_AUTH_TOKEN";

    private final static String CHECK_AUTH_TOKEN_URL = "https://api.uzum.uz/api/user/permittedToCheckout";

    private final static String GET_AUTH_TOKEN_URL = "https://id.uzum.uz/api/auth/token";

    @Autowired
    private StyxProxyService proxyService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Async
    @Scheduled(cron = "${app.job.cron.token-job}")
    public void execute()  {
        String authToken = redisTemplate.opsForValue().get(AUTH_TOKEN);
        boolean isAuthTokenValid = checkAuthToken(authToken);
        if (isAuthTokenValid) {
            return;
        }
        String newAuthToken = getAuthToken();
        log.info("Got new token - {}", newAuthToken);
        redisTemplate.opsForValue().set(AUTH_TOKEN, newAuthToken);
    }

    private String getAuthToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Netty");
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
        if (authToken != null) {
            ProxyRequestParams.ContextValue headers = ProxyRequestParams.ContextValue.builder()
                    .key("headers")
                    .value(Map.of("Authorization", "Bearer %s".formatted(authToken))).build();
            ProxyRequestParams.ContextValue market = ProxyRequestParams.ContextValue.builder()
                    .key("market")
                    .value("UZUM").build();
            ProxyRequestParams requestParams = ProxyRequestParams.builder()
                    .url(CHECK_AUTH_TOKEN_URL)
                    .httpMethod(HttpMethod.GET.name())
                    .context(List.of(headers, market))
                    .build();
            StyxProxyResult<?> proxyResult = proxyService.getProxyResult(requestParams, new ParameterizedTypeReference<>() {
            });
            return proxyResult.getOriginalStatus().equals(HttpStatus.OK.value());
        }
        return false;
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
