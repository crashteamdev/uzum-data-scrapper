package dev.crashteam.uzumdatascrapper.service.integration.interceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@RequiredArgsConstructor
public class AuthHeaderRequestInterceptor implements ClientHttpRequestInterceptor {

    private final static String AUTH_TOKEN = "UZUM_AUTH_TOKEN";

    private final static String CHECK_AUTH_TOKEN_URL = "https://api.uzum.uz/api/user/permittedToCheckout";

    private final static String GET_AUTH_TOKEN_URL = "https://id.uzum.uz/api/auth/token";

    private final RedisTemplate<String, String> redisTemplate;

    private final RestTemplate restTemplate;

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        String authToken = redisTemplate.opsForValue().get(AUTH_TOKEN);
        boolean isAuthTokenValid = checkAuthToken(authToken);
        if (isAuthTokenValid) {
            request.getHeaders().add("Authorization", "Bearer %s".formatted(authToken));
        } else {
            String newAuthToken = getAuthToken();
            redisTemplate.opsForValue().set(AUTH_TOKEN, newAuthToken);
            request.getHeaders().add("Authorization", "Bearer %s".formatted(newAuthToken));
        }

        return execution.execute(request, body);
    }

    private String getAuthToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.add("Referer", "https://id.uzum.uz/ru/security");
        headers.add("Content-Type", "application/json");
        headers.add("Host", "id.uzum.uz");
        headers.add("Accept-Language", "ru");
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                GET_AUTH_TOKEN_URL, HttpMethod.POST, httpEntity, String.class);
        HttpHeaders responseHeaders = responseEntity.getHeaders();
        String responseCookie = responseHeaders.getFirst(HttpHeaders.SET_COOKIE);

        return extractAccessToken(responseCookie);
    }

    private boolean checkAuthToken(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer %s".formatted(authToken));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                CHECK_AUTH_TOKEN_URL, HttpMethod.GET, httpEntity, String.class);

        return responseEntity.getStatusCode() == HttpStatus.OK;
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
