
package dev.crashteam.uzumdatascrapper.mapper;

import com.google.protobuf.Timestamp;
import dev.crashteam.uzum.scrapper.data.v1.UzumProductChange;
import dev.crashteam.uzum.scrapper.data.v1.UzumSkuCharacteristic;
import dev.crashteam.uzum.scrapper.data.v1.UzumValue;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumGQLProductResponse;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.temporal.TemporalAdjusters.firstDayOfYear;

public class UzumGQLProductResponseToMessageMapper {

    public static UzumProductChange mapToProtobuf(UzumGQLProductResponse.Product product) {

        UzumProductChange.Builder productChangeBuilder = UzumProductChange.newBuilder();

        // Mapping main fields
        productChangeBuilder.setProductId(String.valueOf(product.getId()));
        productChangeBuilder.setTitle(product.getTitle());
        if (!CollectionUtils.isEmpty(product.getSkuList())) {
            int totalAvailableAmount = product.getSkuList()
                    .stream()
                    .map(UzumGQLProductResponse.SkuList::getAvailableAmount)
                    .mapToInt(Integer::valueOf)
                    .sum();
            productChangeBuilder.setTotalAvailableAmount(totalAvailableAmount);
        } else {
            productChangeBuilder.setTotalAvailableAmount(0L);
        }
        productChangeBuilder.setOrders(product.getOrdersQuantity());
        productChangeBuilder.setReviewsAmount(product.getFeedbackQuantity());
        productChangeBuilder.setRating(product.getRating());

        // Mapping characteristics
        if (product.getCharacteristics() != null) {
            for (UzumGQLProductResponse.Characteristic characteristic : product.getCharacteristics()) {
                UzumProductChange.UzumProductCharacteristic.Builder characteristicBuilder = UzumProductChange.UzumProductCharacteristic.newBuilder();
                characteristicBuilder.setId(characteristic.getId());
                characteristicBuilder.setTitle(characteristic.getTitle());

                for (UzumGQLProductResponse.Value value : characteristic.getValues()) {
                    UzumValue.Builder valueBuilder = UzumValue.newBuilder();
                    if (value.getCharacteristic() != null) {
                        valueBuilder.setId(String.valueOf(value.getCharacteristic().getId()));
                    }
                    valueBuilder.setTitle(value.getTitle());
                    valueBuilder.setValue(value.getValue());
                    characteristicBuilder.addValues(valueBuilder);
                }

                productChangeBuilder.addCharacteristics(characteristicBuilder);
            }
        }

        // Mapping category
        UzumGQLProductResponse.Category category = product.getCategory();
        if (category != null) {
            UzumProductChange.UzumProductCategory.Builder categoryBuilder = UzumProductChange.UzumProductCategory.newBuilder();
            categoryBuilder.setId(category.getId());
            categoryBuilder.setTitle(category.getTitle());
            productChangeBuilder.setCategory(categoryBuilder);
        }

        // Mapping shop
        var shop = product.getShop();
        if (shop != null) {
            Integer accountId = Optional.ofNullable(shop.getSeller()).map(UzumGQLProductResponse.Seller::getAccountId).orElse(null);
            UzumProductChange.UzumProductSeller.Builder sellerBuilder = UzumProductChange.UzumProductSeller.newBuilder();
            sellerBuilder.setId(shop.getId());
            if (accountId != null) {
                sellerBuilder.setAccountId(accountId);
            }
            sellerBuilder.setSellerLink(shop.getUrl());
            sellerBuilder.setSellerTitle(shop.getTitle());
            sellerBuilder.setReviews(shop.getFeedbackQuantity());
            sellerBuilder.setOrders(shop.getOrdersQuantity());
            sellerBuilder.setRating(String.valueOf(shop.getRating()));
            LocalDate now = LocalDate.now();
            LocalDate firstDay = now.with(firstDayOfYear());
            long epoch = firstDay.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();

            sellerBuilder.setRegistrationDate(Timestamp.newBuilder().setSeconds(epoch));

            sellerBuilder.addContacts(UzumProductChange.UzumProductContact.newBuilder().build());

            productChangeBuilder.setSeller(sellerBuilder);
        }

        // Mapping SKUs
        if (product.getSkuList() != null) {
            for (var sku : product.getSkuList()) {
                UzumProductChange.UzumProductSku.Builder skuBuilder = UzumProductChange.UzumProductSku.newBuilder();
                skuBuilder.setSkuId(String.valueOf(sku.getId()));
                if (sku.getPhoto() != null) {
                    skuBuilder.setPhotoKey(sku.getPhoto().getKey());
                }
                skuBuilder.setPurchasePrice(String.valueOf(sku.getSellPrice()));
                skuBuilder.setFullPrice(String.valueOf(sku.getFullPrice()));
                skuBuilder.setAvailableAmount(sku.getAvailableAmount());

                // Mapping SKU characteristics
                if (sku.getCharacteristicValues() != null) {
                    for (var characteristic : sku.getCharacteristicValues()) {
                        UzumSkuCharacteristic.Builder skuCharacteristicBuilder = UzumSkuCharacteristic.newBuilder();
                        if (characteristic.getCharacteristic() != null) {
                            skuCharacteristicBuilder.setType(characteristic.getCharacteristic().getType());
                        }
                        skuCharacteristicBuilder.setTitle(characteristic.getTitle());
                        skuCharacteristicBuilder.setValue(characteristic.getValue());
                        skuBuilder.addCharacteristics(skuCharacteristicBuilder);
                    }
                }

                productChangeBuilder.addSkus(skuBuilder);
            }
        }

        return productChangeBuilder.build();
    }
}
