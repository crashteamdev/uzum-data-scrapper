package dev.crashteam.uzumdatascrapper.model.cache;

import dev.crashteam.uzumdatascrapper.model.uzum.UzumProduct;
import lombok.Data;

import java.util.List;

@Data
public class CachedProductData {
    private List<UzumProduct.CharacteristicsData> characteristics;
    private List<UzumProduct.SkuData> skuList;
}
