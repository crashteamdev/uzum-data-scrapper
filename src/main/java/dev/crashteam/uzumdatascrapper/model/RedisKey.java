package dev.crashteam.uzumdatascrapper.model;

public enum RedisKey {

    UZUM_PRODUCT("UZUM_PRODUCT");

    private final String key;

    RedisKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
