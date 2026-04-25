package edu.du.et.chatapp.services;

import edu.du.et.chatapp.models.Conversation;
import edu.du.et.chatapp.models.Message;
import edu.du.et.chatapp.models.MessageRole;
import edu.du.et.chatapp.models.UserPreferences;
import edu.du.et.chatapp.repositories.ConversationRepository;
import edu.du.et.chatapp.repositories.MessageRepository;
import edu.du.et.chatapp.services.ai.AIServiceManager;

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
        // Find if the message is already in the conversation (it should be, added by controller)
        // We'll just use the one passed in or ensure it's in the list
        Message userMessage = null;
        List<Message> messages = conversation.getMessages();
        
        // Find the last user message with this content, or create one if missing
        if (!messages.isEmpty() && messages.get(messages.size() - 1).getRole() == MessageRole.USER) {
            userMessage = messages.get(messages.size() - 1);
        } else {
            userMessage = new Message(MessageRole.USER, userInput);
            conversation.addMessage(userMessage);
        }

        if (conversation.getId() > 0) {
            messageRepository.save(conversation.getId(), userMessage);
        }

        // Sliding window: only send the last N messages
        int maxMessages = 20;
        List<Message> allMessages = conversation.getMessages();
        List<Message> slidingWindow;
        
        if (allMessages.size() > maxMessages) {
            slidingWindow = new ArrayList<>(allMessages.subList(allMessages.size() - maxMessages, allMessages.size()));
        } else {
            slidingWindow = new ArrayList<>(allMessages);
        }

        return aiServiceManager.send(slidingWindow, preferences.getModel(), onChunkReceived);
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