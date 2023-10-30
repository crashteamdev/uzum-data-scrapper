package dev.crashteam.uzumdatascrapper.exception;

public class ProductRequestException extends RuntimeException {

    public ProductRequestException() {
    }

    public ProductRequestException(String message) {
        super(message);
    }
}
