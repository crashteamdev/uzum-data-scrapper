package dev.crashteam.uzumdatascrapper.model.uzum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UzumCategoryChild {

    private CategoryData payload;
    private List<CategoryError> errors;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CategoryData {
        private UzumCategory.Data category;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CategoryError {
        private String message;
        private String detailMessage;
    }
}
