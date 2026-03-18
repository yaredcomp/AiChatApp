package com.lj.aichatapp.repository;

import com.lj.aichatapp.models.Prompt;

import java.util.List;

public interface PromptRepository {
    List<Prompt> findAll();
    void save(Prompt prompt);
    void delete(int id);
}