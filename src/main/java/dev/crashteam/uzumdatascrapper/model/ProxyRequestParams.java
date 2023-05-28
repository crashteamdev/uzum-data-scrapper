package dev.crashteam.uzumdatascrapper.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProxyRequestParams {

    private String url;
    private String httpMethod;
    private Long timeout;
    private List<ContextValue> context;

    @Data
    @Builder
    public static class ContextValue {
        private String key;
        private Object value;
    }
}
