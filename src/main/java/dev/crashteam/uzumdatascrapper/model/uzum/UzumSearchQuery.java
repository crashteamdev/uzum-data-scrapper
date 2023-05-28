package dev.crashteam.uzumdatascrapper.model.uzum;

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
    public static class Variables {
        private QueryInput queryInput;
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
