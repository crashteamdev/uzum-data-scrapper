package dev.crashteam.uzumdatascrapper.exception;

public class UzumGqlRequestException extends RuntimeException {

    public UzumGqlRequestException() {
        super();
    }

    public UzumGqlRequestException(String message) {
        super(message);
    }

    public UzumGqlRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
