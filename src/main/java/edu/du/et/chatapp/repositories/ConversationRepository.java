package edu.du.et.chatapp.repositories;

import edu.du.et.chatapp.models.Conversation;

import java.util.List;

public interface ConversationRepository {
    Conversation create(String title);
    Conversation findById(int id);
    List<Conversation> findAll();
    void updateTitle(int id, String newTitle);
    void delete(int id);
}