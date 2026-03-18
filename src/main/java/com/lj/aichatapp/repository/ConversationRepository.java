package com.lj.aichatapp.repository;

import com.lj.aichatapp.models.Conversation;

import java.util.List;

public interface ConversationRepository {
    Conversation create(String title);
    Conversation findById(int id);
    List<Conversation> findAll();
    void updateTitle(int id, String newTitle);
    void delete(int id);
}