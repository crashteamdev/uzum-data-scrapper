package dev.crashteam.uzumdatascrapper.mapper;

import dev.crashteam.uzumdatascrapper.model.dto.UzumCategoryMessage;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumCategory;
import dev.crashteam.uzumdatascrapper.model.uzum.UzumGQLResponse;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UzumCategoryToMessageMapper {

    public static UzumCategoryMessage categoryToMessage(UzumCategory.Data data) {
        UzumCategoryMessage categoryMessage = UzumCategoryMessage.builder()
                .id(data.getId())
                .adult(data.isAdult())
                .eco(data.isEco())
                .title(data.getTitle())
                .time(Instant.now().toEpochMilli())
                .build();
        if (!CollectionUtils.isEmpty(data.getChildren())) {
            List<UzumCategoryMessage> childrenCategories = new ArrayList<>();
            for (UzumCategory.Data child : data.getChildren()) {
                childrenCategories.add(categoryToMessage(child));
            }
            categoryMessage.setChildren(childrenCategories);
        }
        return categoryMessage;
    }

    public static UzumCategoryMessage categoryToMessage(UzumGQLResponse.ResponseCategory category, boolean eco) {
        UzumCategoryMessage categoryMessage = UzumCategoryMessage.builder()
                .id(category.getId())
                .adult(category.isAdult())
                .title(category.getTitle())
                .eco(eco)
                .time(Instant.now().toEpochMilli())
                .build();

        return categoryMessage;
    }
}
