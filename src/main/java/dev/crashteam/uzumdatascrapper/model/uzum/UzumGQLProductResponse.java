package dev.crashteam.uzumdatascrapper.model.uzum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class UzumGQLProductResponse {

    private ProductData data;
    private List<UzumGQLResponse.GQLError> errors;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductData {
        public ProductPage productPage;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductPage {
        private Product product;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Product {
        public int id;
        public int ordersQuantity;
        public int feedbackQuantity;
        public int feedbackPhotosCount;
        public Object photo360;
        public List<Photo> photos;
        public double rating;
        public Object video;
        public String title;
        public Category category;
        public int minFullPrice;
        public int minSellPrice;
        public List<Characteristic> characteristics;
        public List<Badge> badges;
        public String description;
        public boolean favorite;
        public Shop shop;
        public String shortDescription;
        public List<SkuList> skuList;
        public List<String> attributes;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Action {
        public String location;
        public String type;
        public Image image;
        public String text;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Avatar {
        public String low;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Badge {
        public String backgroundColor;
        public String description;
        public int id;
        public Object link;
        public String text;
        public String textColor;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Category {
        public int id;
        public List<ParentList> parentList;
        public String title;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Characteristic {
        public int id;
        public String title;
        public String type;
        public List<Value> values;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Characteristic2 {
        public int id;
        public String title;
        public String type;
        public List<Value> values;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CharacteristicValue {
        public int id;
        public Photo photo;
        public String title;
        public String value;
        public Characteristic characteristic;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DiscountBadge {
        public String backgroundColor;
        public int id;
        public String text;
        public String textColor;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FastDeliveryInfo {
        public String title;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Filter {
        public Object description;
        public int id;
        public Object measurementUnit;
        public String title;
        public String type;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Image {
        public String low;
        public String high;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LegalRecord {
        public Object name;
        public String value;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Link {
        public String high;
        public String low;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ParentList {
        public int id;
        public String title;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentOption {
        public int paymentPerMonth;
        public String paymentInfo;
        public String text;
        public String type;
        public int id;
        public boolean active;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Photo {
        public String key;
        public Link link;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Photo2 {
        public String key;
        public Link link;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Property {
        public Object description;
        public Filter filter;
        public int id;
        public String image;
        public String name;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Seller {
        public int accountId;
        public List<LegalRecord> legalRecords;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Shop {
        public Avatar avatar;
        public int feedbackQuantity;
        public int id;
        public boolean official;
        public int ordersQuantity;
        public double rating;
        public Seller seller;
        public Object shortTitle;
        public String title;
        public String url;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkuList {
        public int id;
        public int availableAmount;
        public Photo photo;
        public List<PaymentOption> paymentOptions;
        public String skuTitle;
        public int sellPrice;
        public Object discount;
        public List<Property> properties;
        public DiscountBadge discountBadge;
        public List<CharacteristicValue> characteristicValues;
        public int fullPrice;
        public Vat vat;
        public Object discountTimer;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Value {
        public int id;
        public Photo photo;
        public String title;
        public String value;
        public Characteristic characteristic;
        public String __typename;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Vat {
        public int vatRate;
        public int vatAmount;
        public String type;
        public int price;
        public String __typename;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GQLError {
        private String message;
    }
}