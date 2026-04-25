package edu.du.et.chatapp.repositories;

import edu.du.et.chatapp.models.Message;

import java.util.List;

public interface MessageRepository {
    void save(int conversationId, Message message);
    List<Message> findByConversationId(int conversationId);
    void deleteByConversationId(int conversationId);
}