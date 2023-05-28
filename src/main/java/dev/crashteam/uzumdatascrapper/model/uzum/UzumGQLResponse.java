package dev.crashteam.uzumdatascrapper.model.uzum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UzumGQLResponse {

    private ResponseData data;
    private List<GQLError> errors;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseData {
        private MakeSearch makeSearch;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MakeSearch {
        private String id;
        private String queryId;
        private String queryText;
        private SearchCategory category;
        private List<ResponseCategoryWrapper> categoryTree;
        private List<CatalogCardWrapper> items;
        private Long total;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchCategory {
        private Long id;
        private String title;
        private SearchCategory parent;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseCategoryWrapper {
        private ResponseCategory category;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseCategory {
        private Long id;
        private String icon;
        private String title;
        private boolean adult;
        private ResponseCategory parent;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogCardWrapper {
        private CatalogCard catalogCard;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogCard {
        private boolean adult;
        private boolean favorite;
        private Long feedbackQuantity;
        private Long ordersQuantity;
        private Long id;
        private Long minFullPrice;
        private Long minSellPrice;
        private Long productId;
        private String title;
        private List<CharacteristicValue> characteristicValues;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CharacteristicValue {
        private Long id;
        private String value;
        private String title;
        private CharacteristicWrapper characteristic;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CharacteristicWrapper {
        private List<CharacteristicValue> values;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GQLError {
        private String message;
    }

}
