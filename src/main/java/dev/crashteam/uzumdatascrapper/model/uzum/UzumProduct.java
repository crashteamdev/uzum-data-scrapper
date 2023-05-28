package dev.crashteam.uzumdatascrapper.model.uzum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UzumProduct {

    private Payload payload;
    private List<ProductError> errors;

    @Data
    public static class Payload {
        private ProductData data;
    }

    @Data
    public static class ProductData {
        private Long id;
        private String title;
        private ProductCategory category;
        private String rating;
        private Long reviewsAmount;
        private Long ordersAmount;
        private Long totalAvailableAmount;
        private Long charityCommission;
        private String description;
        private List<String> attributes;
        private List<String> tags;
        private List<ProductPhoto> photos;
        private List<CharacteristicsData> characteristics;
        private List<SkuData> skuList;
        private ProductSeller seller;
        private Feedback topFeedback;
        private boolean isEco;
        private boolean adultCategory;

    }

    @Data
    public static class ProductCategory implements Serializable {
        private Long id;
        private String title;
        private Long productAmount;
        private ProductCategory parent;
    }

    @Data
    public static class ProductPhoto {
        private String color;
        private String photoKey;
    }

    @Data
    public static class CharacteristicsData {
        private Long id;
        private String title;
        private List<Characteristic> values;
    }

    @Data
    public static class Characteristic {
        private Long id;
        private String title;
        private String value;
    }

    @Data
    public static class SkuData {
        private Long id;
        private List<ScuCharacteristic> characteristics;
        private Long availableAmount;
        private String fullPrice;
        private String charityProfit;
        private String purchasePrice;
        private String barcode;
        private Long sellPrice;
    }

    @Data
    public static class ScuCharacteristic {
        private Integer charIndex;
        private Integer valueIndex;
    }

    @Data
    public static class ProductSeller {
        private Long id;
        private String title;
        private String link;
        private String description;
        private Long registrationDate;
        private String rating;
        private Long reviews;
        private Long orders;
        private Long sellerAccountId;
        private List<Contact> contacts;
    }

    @Data
    public static class Feedback {
        private Long reviewId;
        private Long productId;
        private Long date;
        private Boolean edited;
        private String customer;
        private FeedBackReply reply;
        private Long rating;
        private String content;
        private String status;
        private Long id;
    }

    @Data
    public static class FeedBackReply {
        private Long id;
        private Long date;
        private Boolean edited;
        private String content;
        private String shop;
    }

    @Data
    public static class ProductError {
        private String code;
        private String message;
        private String detailMessage;
    }

    @Data
    public static class Contact {
        private String type;
        private String value;
    }
}
