package com.lj.aichatapp.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Simple chat message model.
 */
public class Message {

    private String id;
    private MessageRole role;
    private String content;
    private Instant timestamp;

    public Message() {
        // Jackson
    }

    public Message(MessageRole role, String content) {
        this.id = UUID.randomUUID().toString();
        this.role = role;
        this.content = content;
        this.timestamp = Instant.now();
    }

    // getters & setters
    public String getId() {
        return id;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole r) {
        this.role = r;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String c) {
        this.content = c;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant t) {
        this.timestamp = t;
    }
}
