package edu.du.et.chatapp.repositories;

import edu.du.et.chatapp.models.Prompt;

import java.util.List;

public interface PromptRepository {
    List<Prompt> findAll();
    void save(Prompt prompt);
    void delete(int id);
}