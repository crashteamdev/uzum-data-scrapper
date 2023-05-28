package dev.crashteam.uzumdatascrapper.service.integration;


import dev.crashteam.uzumdatascrapper.model.ProxyRequestParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StyxProxyService {

    private final RestTemplate restTemplate;

    @Value("${app.integration.styx}")
    private String styxUrl;

    public <T> T getProxyResult(ProxyRequestParams body, ParameterizedTypeReference<T> typeReference) {
        HttpEntity<ProxyRequestParams> request = new HttpEntity<>(body);
        return restTemplate.exchange(styxUrl + "/v2/proxy", HttpMethod.POST, request, typeReference).getBody();
    }
}
