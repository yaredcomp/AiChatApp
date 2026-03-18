package com.lj.aichatapp.repository;

import com.lj.aichatapp.models.Message;

import java.util.List;

public interface MessageRepository {
    void save(int conversationId, Message message);
    List<Message> findByConversationId(int conversationId);
    void deleteByConversationId(int conversationId);
}