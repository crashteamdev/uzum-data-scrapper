package dev.crashteam.uzumdatascrapper.mapper;


import dev.crashteam.uzumdatascrapper.model.cache.CachedProductData;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumProduct;

public class UzumProductToCachedProduct {

    public static CachedProductData toCachedData(UzumProduct.ProductData data) {
        CachedProductData cachedProductData = new CachedProductData();
        cachedProductData.setCharacteristics(data.getCharacteristics());
        cachedProductData.setSkuList(data.getSkuList());
        return cachedProductData;
    }
}
