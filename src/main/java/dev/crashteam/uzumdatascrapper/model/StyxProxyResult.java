package dev.crashteam.uzumdatascrapper.model;

import lombok.Data;

@Data
public class StyxProxyResult<T> {

    private int code;
    private Integer originalStatus;
    private String url;
    private String httpMethod;
    private T body;

}
