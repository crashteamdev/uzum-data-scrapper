package dev.crashteam.uzumdatascrapper.exception;

public class ProxyRequestException extends RuntimeException {

    private int statusCode;

    private Object body;

    public ProxyRequestException() {
        super();
    }

    public ProxyRequestException(String message) {
        super(message);
    }

    public ProxyRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyRequestException(String message, Object body, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object getBody() {
        return body;
    }
}
