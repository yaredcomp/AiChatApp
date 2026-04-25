package edu.du.et.chatapp.services;

import edu.du.et.chatapp.models.Prompt;
import edu.du.et.chatapp.repositories.PromptRepository;

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