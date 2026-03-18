package com.lj.aichatapp.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageRole {
    USER,
    ASSISTANT,
    SYSTEM;

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static MessageRole fromValue(String value) {
        return MessageRole.valueOf(value.toUpperCase());
    }
}
