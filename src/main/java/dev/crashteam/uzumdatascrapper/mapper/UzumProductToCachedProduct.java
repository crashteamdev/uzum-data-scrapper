package dev.crashteam.uzumdatascrapper.mapper;


import dev.crashteam.uzumdatascrapper.model.cache.CachedProductData;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumGQLProductResponse;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumProduct;

import java.util.ArrayList;
import java.util.List;

public class UzumProductToCachedProduct {

    public static CachedProductData toCachedData(UzumProduct.ProductData data) {
        CachedProductData cachedProductData = new CachedProductData();
        cachedProductData.setCharacteristics(data.getCharacteristics());
        cachedProductData.setSkuList(data.getSkuList());
        return cachedProductData;
    }

    public static CachedProductData toGQLCachedData(UzumGQLProductResponse.Product data) {
        CachedProductData cachedProductData = new CachedProductData();
        List<UzumProduct.CharacteristicsData> characteristicsData = new ArrayList<>();
        List<UzumProduct.SkuData> skuData = new ArrayList<>();
        for (UzumGQLProductResponse.Characteristic characteristic : data.getCharacteristics()) {
            UzumProduct.CharacteristicsData mappedCharacter = new UzumProduct.CharacteristicsData();
            mappedCharacter.setId((long) characteristic.getId());
            mappedCharacter.setTitle(characteristic.getTitle());
            List<UzumProduct.Characteristic> mappedValues = getCharacteristics(characteristic);
            mappedCharacter.setValues(mappedValues);
            characteristicsData.add(mappedCharacter);
        }

        for (UzumGQLProductResponse.SkuList sku : data.getSkuList()) {
            UzumProduct.SkuData mappetSkuData = new UzumProduct.SkuData();
            mappetSkuData.setId((long) sku.getId());

            List<UzumProduct.ScuCharacteristic> characteristics = new ArrayList<>();

            for (UzumGQLProductResponse.Characteristic characteristic : data.getCharacteristics()) {
                UzumProduct.ScuCharacteristic scuCharacteristic = new UzumProduct.ScuCharacteristic();
                scuCharacteristic.setValueIndex(characteristic.getId());
            }
            mappetSkuData.setCharacteristics(characteristics);
        }

        cachedProductData.setCharacteristics(characteristicsData);
        //cachedProductData.setSkuList(data.getSkuList());
        return cachedProductData;
    }

    private static List<UzumProduct.Characteristic> getCharacteristics(UzumGQLProductResponse.Characteristic characteristic) {
        List<UzumProduct.Characteristic> mappedValues = new ArrayList<>();
        for (UzumGQLProductResponse.Value characteristicValue : characteristic.getValues()) {
            UzumProduct.Characteristic mappedChar = new UzumProduct.Characteristic();
            mappedChar.setValue(characteristicValue.getValue());
            mappedChar.setId((long) characteristicValue.getId());
            mappedChar.setTitle(characteristic.getTitle());
            mappedValues.add(mappedChar);
        }
        return mappedValues;
    }
}
