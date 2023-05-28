package dev.crashteam.uzumdatascrapper.service;

import dev.crashteam.uzumdatascrapper.model.stream.Message;

@FunctionalInterface
public interface MessagePublisher<T extends Message> {

    Object publish(T message);
}
