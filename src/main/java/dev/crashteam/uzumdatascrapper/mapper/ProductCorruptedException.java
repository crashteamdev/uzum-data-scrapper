package dev.crashteam.uzumdatascrapper.mapper;

public class ProductCorruptedException extends RuntimeException {
    public ProductCorruptedException(String message) {
        super(message);
    }
}
