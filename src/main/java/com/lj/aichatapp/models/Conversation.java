package com.lj.aichatapp.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Conversation {

    private int id;
    private String title;
    private List<Message> messages = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Conversation() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void setMessages(List<Message> messages) {
        this.messages = new ArrayList<>(messages);
    }

    public void addMessage(Message m) {
        messages.add(m);
    }

    public void clear() {
        messages.clear();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPreview() {
        if (messages == null || messages.isEmpty()) {
            return "No messages yet";
        }
        Message lastMsg = messages.get(messages.size() - 1);
        String content = lastMsg.getContent();
        if (content == null || content.isEmpty()) {
            return "...";
        }
        return content.length() > 40 ? content.substring(0, 40) + "..." : content;
    }

    @Override
    public String toString() {
        return title != null ? title : "New Chat";
    }
}
