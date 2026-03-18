package com.lj.aichatapp.service;

import com.lj.aichatapp.models.Prompt;
import com.lj.aichatapp.repository.PromptRepository;

import java.util.List;

public class PromptService {

    private final PromptRepository promptRepository;

    public PromptService(PromptRepository promptRepository) {
        this.promptRepository = promptRepository;
    }

    public List<Prompt> getAllPrompts() {
        return promptRepository.findAll();
    }

    public void savePrompt(Prompt prompt) {
        promptRepository.save(prompt);
    }

    public void deletePrompt(int id) {
        promptRepository.delete(id);
    }
}