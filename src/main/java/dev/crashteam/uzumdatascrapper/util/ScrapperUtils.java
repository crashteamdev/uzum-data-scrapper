package dev.crashteam.uzumdatascrapper.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ScrapperUtils {

    public static <T> List<List<T>> getBatches(List<T> collection, int batchSize) {
        return IntStream.iterate(0, i -> i < collection.size(), i -> i + batchSize)
                .mapToObj(i -> collection.subList(i, Math.min(i + batchSize, collection.size())))
                .collect(Collectors.toList());
    }
}
