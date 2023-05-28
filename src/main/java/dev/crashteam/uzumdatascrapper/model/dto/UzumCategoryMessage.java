package dev.crashteam.uzumdatascrapper.model.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class UzumCategoryMessage implements Serializable {

    private Long id;
    private Boolean adult;
    private Boolean eco;
    private String title;
    private List<UzumCategoryMessage> children;
    private Long time;
}
