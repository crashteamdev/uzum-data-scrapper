package dev.crashteam.uzumdatascrapper.model.uzum;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UzumSearchQuery {

    private String operationName;
    private Variables variables;
    private String query;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Variables {
        private QueryInput queryInput;
        private Long productId;
        private String linkTrans4;
        private String linkTrans5;
        private String linkTrans6;
        private String linkTrans7;

    }

    @Data
    @Builder
    public static class QueryInput {
        private String categoryId;
        private String showAdultContent;
        private List<String> filters;
        private String sort;
        private Pagination pagination;
    }

    @Data
    @Builder
    public static class Pagination {
        private long offset;
        private long limit;
    }
}
