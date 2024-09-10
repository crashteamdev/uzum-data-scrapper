package dev.crashteam.uzumdatascrapper.mapper;

import com.google.protobuf.Timestamp;
import dev.crashteam.uzum.scrapper.data.v1.UzumProductChange;
import dev.crashteam.uzum.scrapper.data.v1.UzumSkuCharacteristic;
import dev.crashteam.uzum.scrapper.data.v1.UzumValue;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumProduct;
import lombok.val;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
public class UzumProductToMessageMapper {

    public UzumProductChange mapToMessage(UzumProduct.ProductData productData) {
        List<UzumProductChange.UzumProductCharacteristic> characteristicsData = new ArrayList<>();
        for (UzumProduct.CharacteristicsData characteristic : productData.getCharacteristics()) {
            List<UzumValue> characteristicsValues = new ArrayList<>();
            for (UzumProduct.Characteristic characteristicValue : characteristic.getValues()) {
                var uzumValue = UzumValue.newBuilder()
                        .setValue(characteristicValue.getValue())
                        .setTitle(characteristicValue.getTitle())
                        .setId(characteristicValue.getId().toString())
                        .build();
                characteristicsValues.add(uzumValue);
            }
            var productCharacteristic = UzumProductChange.UzumProductCharacteristic.newBuilder()
                    .setId(characteristic.getId())
                    .setTitle(characteristic.getTitle())
                    .addAllValues(characteristicsValues)
                    .build();
            characteristicsData.add(productCharacteristic);
        }
        AtomicBoolean isCorrupted = new AtomicBoolean(false);
        List<UzumProductChange.UzumProductSku> skuList = productData.getSkuList()
                .stream()
                .map(sku -> {
                    List<UzumSkuCharacteristic> characteristics = sku.getCharacteristics()
                            .stream()
                            .map(it -> {
                                var productCharacteristic = productData
                                        .getCharacteristics().get(it.getCharIndex());
                                var characteristicValue = productCharacteristic
                                        .getValues().get(it.getValueIndex());
                                return UzumSkuCharacteristic.newBuilder()
                                        .setType(productCharacteristic.getTitle())
                                        .setTitle(characteristicValue.getTitle())
                                        .setValue(characteristicValue.getValue()).build();
                            }).toList();
                    UzumProduct.ProductPhoto productPhoto = extractProductPhoto(productData, sku);
                    if (productPhoto == null) {
                        isCorrupted.set(true);
                    }
                    UzumProductChange.Restriction restriction;
                    UzumProduct.Restriction skuRestriction = sku.getRestriction();
                    val uzumProductBuilder = UzumProductChange.UzumProductSku.newBuilder()
                            .setSkuId(sku.getId().toString())
                            .setAvailableAmount(sku.getAvailableAmount())
                            .setPurchasePrice(sku.getPurchasePrice())
                            .addAllCharacteristics(characteristics);
                    if (productPhoto != null) {
                        uzumProductBuilder.setPhotoKey(productPhoto.getPhotoKey());
                    }
                    if (skuRestriction != null && skuRestriction.getRestrictedAmount() != null
                            && skuRestriction.getRestrictedAmount() > 0) {
                        restriction = UzumProductChange.Restriction
                                .newBuilder()
                                .setBoughtAmount(skuRestriction.getBoughtAmount())
                                .setRestrictedAmount(skuRestriction.getRestrictedAmount())
                                .build();
                        uzumProductBuilder.setRestriction(restriction);
                    }

                    if (sku.getFullPrice() != null) {
                        uzumProductBuilder.setFullPrice(sku.getFullPrice());
                    }
                    return uzumProductBuilder.build();
                }).toList();
        if (isCorrupted.get()) {
            throw new ProductCorruptedException("Corrupted item. productId=%s".formatted(productData.getId()));
        }

        UzumProductChange.Builder builder = UzumProductChange.newBuilder()
                .setRating(Double.parseDouble(productData.getRating()))
                .setCategory(mapCategory(productData.getCategory()))
                .setOrders(productData.getOrdersAmount())
                .setProductId(productData.getId().toString())
                .setReviewsAmount(productData.getReviewsAmount())
                .addAllTags(productData.getTags())
                .addAllAttributes(productData.getAttributes())
                .setTitle(productData.getTitle())
                .setTotalAvailableAmount(productData.getTotalAvailableAmount())
                .setSeller(mapSeller(productData))
                .addAllSkus(skuList)
                .addAllCharacteristics(characteristicsData)
                .setIsEco(productData.isEco())
                .setIsAdult(productData.isAdultCategory());
        if (productData.getDescription() != null) {
            builder.setDescription(productData.getDescription());
        }
        return builder.build();
    }

    private UzumProduct.ProductPhoto extractProductPhoto(UzumProduct.ProductData product, UzumProduct.SkuData sku) {
        return sku.getCharacteristics().stream()
                .map(it -> {
                    var productCharacteristic = product
                            .getCharacteristics().get(it.getCharIndex());
                    var characteristicValue = productCharacteristic
                            .getValues().get(it.getValueIndex());
                    var value = characteristicValue.getValue();
                    return product.getPhotos().stream()
                            .filter(photo -> photo.getColor() != null)
                            .filter(photo -> photo.getColor().equals(value))
                            .findFirst()
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(product.getPhotos()
                        .stream().findFirst().orElse(null));
    }

    private UzumProductChange.UzumProductCategory mapCategory(UzumProduct.ProductCategory productCategory) {
        UzumProductChange.UzumProductCategory.Builder builder = UzumProductChange.UzumProductCategory.newBuilder()
                .setId(productCategory.getId())
                .setProductAmount(productCategory.getProductAmount())
                .setTitle(productCategory.getTitle());
        if (productCategory.getParent() != null) {
            builder.setParent(mapCategory(productCategory.getParent()));
        }
        return builder.build();
    }

    private UzumProductChange.UzumProductSeller mapSeller(UzumProduct.ProductData productData) {
        UzumProduct.ProductSeller productSeller = productData.getSeller();
        if (productSeller != null) {
            List<UzumProductChange.UzumProductContact> contacts = new ArrayList<>();
            for (UzumProduct.Contact contact : productSeller.getContacts()) {
                var uzumProductContact = UzumProductChange.UzumProductContact.newBuilder()
                        .setType(contact.getType())
                        .setValue(contact.getValue())
                        .build();
                contacts.add(uzumProductContact);
            }
            return UzumProductChange.UzumProductSeller.newBuilder()
                    .setAccountId(productSeller.getSellerAccountId())
                    .setId(productSeller.getId())
                    .setRating(productSeller.getRating())
                    .setRegistrationDate(Timestamp.newBuilder().setSeconds(productSeller.getRegistrationDate()).build())
                    .setReviews(productSeller.getReviews())
                    .setSellerLink(productSeller.getLink())
                    .setSellerTitle(productSeller.getTitle())
                    .setOrders(productSeller.getOrders())
                    .addAllContacts(contacts)
                    .build();
        }
        return null;
    }
}
