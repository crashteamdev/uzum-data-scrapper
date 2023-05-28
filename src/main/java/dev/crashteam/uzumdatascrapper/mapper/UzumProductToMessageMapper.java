package dev.crashteam.uzumdatascrapper.mapper;

import dev.crashteam.uzumdatascrapper.model.dto.UzumProductMessage;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumProduct;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
public class UzumProductToMessageMapper {

    public UzumProductMessage productToMessage(UzumProduct.ProductData productData) {

        List<UzumProductMessage.CharacteristicsData> characteristicsData = new ArrayList<>();
        for (UzumProduct.CharacteristicsData characteristic : productData.getCharacteristics()) {
            var messageCharacteristic = new UzumProductMessage.CharacteristicsData();
            messageCharacteristic.setId(characteristic.getId());
            messageCharacteristic.setTitle(characteristic.getTitle());
            List<UzumProductMessage.Characteristic> characteristicsValues = new ArrayList<>();
            for (UzumProduct.Characteristic characteristicValue : characteristic.getValues()) {
                var messageCharacteristicValue = new UzumProductMessage.Characteristic();
                messageCharacteristicValue.setValue(characteristicValue.getValue());
                messageCharacteristicValue.setTitle(characteristicValue.getTitle());
                messageCharacteristicValue.setId(characteristicValue.getId());
                characteristicsValues.add(messageCharacteristicValue);
            }
            messageCharacteristic.setValues(characteristicsValues);
            characteristicsData.add(messageCharacteristic);
        }
        AtomicBoolean isCorrupted = new AtomicBoolean(false);
        List<UzumProductMessage.KeItemSku> skuList = productData.getSkuList()
                .stream()
                .map(sku -> {
                    List<UzumProductMessage.KeItemCharacteristic> characteristics = sku.getCharacteristics()
                            .stream()
                            .map(it -> {
                                var productCharacteristic = productData
                                        .getCharacteristics().get(it.getCharIndex());
                                var characteristicValue = productCharacteristic
                                        .getValues().get(it.getValueIndex());
                                return UzumProductMessage.KeItemCharacteristic.builder()
                                        .type(productCharacteristic.getTitle())
                                        .title(characteristicValue.getTitle())
                                        .value(characteristicValue.getValue()).build();
                            }).toList();
                    UzumProduct.ProductPhoto productPhoto = sku.getCharacteristics().stream()
                            .map(it -> {
                                var productCharacteristic = productData
                                        .getCharacteristics().get(it.getCharIndex());
                                var characteristicValue = productCharacteristic
                                        .getValues().get(it.getValueIndex());
                                var value = characteristicValue.getValue();
                                return productData.getPhotos().stream()
                                        .filter(photo -> photo.getColor() != null)
                                        .filter(photo -> photo.getColor().equals(value))
                                        .findFirst()
                                        .orElse(null);
                            })
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(productData.getPhotos()
                                    .stream().findFirst().orElse(null));
                    if (productPhoto == null) {
                        isCorrupted.set(true);
                    }
                    return UzumProductMessage.KeItemSku.builder()
                            .skuId(sku.getId())
                            .availableAmount(sku.getAvailableAmount())
                            .fullPrice(sku.getFullPrice())
                            .purchasePrice(sku.getPurchasePrice())
                            .characteristics(characteristics)
                            .photoKey(productPhoto != null ? productPhoto.getPhotoKey() : null)
                            .build();
                }).toList();

        return UzumProductMessage.builder()
                .rating(productData.getRating())
                .category(getCategory(productData.getCategory()))
                .orders(productData.getOrdersAmount())
                .productId(productData.getId())
                .reviewsAmount(productData.getReviewsAmount())
                .description(productData.getDescription())
                .tags(productData.getTags())
                .attributes(productData.getAttributes())
                .time(Instant.now().toEpochMilli())
                .title(productData.getTitle())
                .totalAvailableAmount(productData.getTotalAvailableAmount())
                .seller(getSeller(productData))
                .skuList(skuList)
                .characteristics(characteristicsData)
                .isEco(productData.isEco())
                .isAdult(productData.isAdultCategory())
                .isCorrupted(isCorrupted.get())
                .build();

    }

    private UzumProductMessage.ProductCategory getCategory(UzumProduct.ProductCategory productCategory) {
        UzumProductMessage.ProductCategory category = new UzumProductMessage.ProductCategory();
        category.setId(productCategory.getId());
        category.setProductAmount(productCategory.getProductAmount());
        category.setTitle(productCategory.getTitle());
        if (productCategory.getParent() != null) {
            category.setParent(getCategory(productCategory.getParent()));
        }
        return category;
    }

    private UzumProductMessage.KeProductSeller getSeller(UzumProduct.ProductData productData) {
        UzumProduct.ProductSeller productSeller = productData.getSeller();
        if (productSeller != null) {
            List<UzumProductMessage.Contact> contacts = new ArrayList<>();
            for (UzumProduct.Contact contact : productSeller.getContacts()) {
                UzumProductMessage.Contact messageContact = new UzumProductMessage.Contact();
                messageContact.setType(contact.getType());
                messageContact.setValue(contact.getValue());
                contacts.add(messageContact);
            }
            return UzumProductMessage.KeProductSeller.builder()
                    .accountId(productSeller.getSellerAccountId())
                    .id(productSeller.getId())
                    .rating(productSeller.getRating())
                    .registrationDate(productSeller.getRegistrationDate())
                    .reviews(productSeller.getReviews())
                    .sellerLink(productSeller.getLink())
                    .sellerTitle(productSeller.getTitle())
                    .orders(productSeller.getOrders())
                    .contacts(contacts)
                    .build();
        }
        return null;
    }
}
