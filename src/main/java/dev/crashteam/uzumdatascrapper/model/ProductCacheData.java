package dev.crashteam.uzumdatascrapper.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductCacheData {

    private Long productId;
    private LocalDateTime createTime;
}
