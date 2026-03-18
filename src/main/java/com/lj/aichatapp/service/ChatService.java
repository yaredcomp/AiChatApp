package com.lj.aichatapp.service;

import com.lj.aichatapp.models.Conversation;
import com.lj.aichatapp.models.Message;
import com.lj.aichatapp.models.MessageRole;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.repository.ConversationRepository;
import com.lj.aichatapp.repository.MessageRepository;
import com.lj.aichatapp.service.ai.AIServiceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ChatService {

    private final AIServiceManager aiServiceManager;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserPreferences preferences;

    public ChatService(AIServiceManager aiServiceManager, 
                       ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       UserPreferences preferences) {
        this.aiServiceManager = aiServiceManager;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.preferences = preferences;
    }

    public Conversation createNewConversation(String title) {
        return conversationRepository.create(title);
    }

    public List<Conversation> getAllConversations() {
        return conversationRepository.findAll();
    }

    public void loadConversationMessages(Conversation conversation) {
        if (conversation.getId() > 0) {
            List<Message> messages = messageRepository.findByConversationId(conversation.getId());
            conversation.setMessages(messages);
        }
    }

    public void deleteConversation(int id) {
        messageRepository.deleteByConversationId(id);
        conversationRepository.delete(id);
    }

    public void updateConversationTitle(int id, String newTitle) {
        conversationRepository.updateTitle(id, newTitle);
    }

    public Conversation saveOrGetCurrentConversation(Conversation currentConversation, String firstMessage) {
        if (currentConversation.getId() == -1) {
            String title = firstMessage.length() > 30 ? firstMessage.substring(0, 30) + "..." : firstMessage;
            Conversation saved = conversationRepository.create(title);
            currentConversation.setId(saved.getId());
            currentConversation.setTitle(saved.getTitle());
            return currentConversation;
        }
        return currentConversation;
    }

    public CompletableFuture<String> sendMessage(Conversation conversation, String userInput, Consumer<String> onChunkReceived) {
        Message userMessage = new Message(MessageRole.USER, userInput);
        conversation.addMessage(userMessage);

        if (conversation.getId() > 0) {
            messageRepository.save(conversation.getId(), userMessage);
        }

        List<Message> conversationWithSystemPrompt = new ArrayList<>();
        conversationWithSystemPrompt.add(new Message(MessageRole.SYSTEM, preferences.getSystemPrompt()));
        conversationWithSystemPrompt.addAll(conversation.getMessages());

        return aiServiceManager.send(conversationWithSystemPrompt, preferences.getModel(), onChunkReceived);
    }

    public void saveAiResponse(Conversation conversation, String response) {
        Message aiMessage = new Message(MessageRole.ASSISTANT, response);
        conversation.addMessage(aiMessage);

        if (conversation.getId() > 0) {
            messageRepository.save(conversation.getId(), aiMessage);
        }
    }

    public void clearConversation(Conversation conversation) {
        conversation.clear();
        if (conversation.getId() > 0) {
            messageRepository.deleteByConversationId(conversation.getId());
        }
    }
}