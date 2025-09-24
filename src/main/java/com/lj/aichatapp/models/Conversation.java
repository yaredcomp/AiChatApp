package com.lj.aichatapp.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Conversation {

    private List<Message> messages = new ArrayList<>();

    public Conversation() {
    }

    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void addMessage(Message m) {
        messages.add(m);
    }

    public void clear() {
        messages.clear();
    }
}
