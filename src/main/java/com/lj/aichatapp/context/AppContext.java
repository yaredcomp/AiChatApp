package com.lj.aichatapp.context;

import com.lj.aichatapp.infrastructure.database.DatabaseManager;
import com.lj.aichatapp.infrastructure.preferences.PreferencesManager;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.repository.ConversationRepository;
import com.lj.aichatapp.repository.MessageRepository;
import com.lj.aichatapp.repository.PromptRepository;
import com.lj.aichatapp.repository.impl.ConversationRepositoryImpl;
import com.lj.aichatapp.repository.impl.MessageRepositoryImpl;
import com.lj.aichatapp.repository.impl.PromptRepositoryImpl;
import com.lj.aichatapp.service.ChatService;
import com.lj.aichatapp.service.PromptService;
import com.lj.aichatapp.service.SettingsService;
import com.lj.aichatapp.service.ai.AIServiceManager;

public class AppContext {

    private static AppContext instance;

    private final UserPreferences preferences;
    private final DatabaseManager databaseManager;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final PromptRepository promptRepository;
    private final SettingsService settingsService;
    private final AIServiceManager aiServiceManager;
    private final ChatService chatService;
    private final PromptService promptService;

    private AppContext() {
        PreferencesManager.ensureAppDirectory();
        this.preferences = PreferencesManager.loadPreferences();

        this.databaseManager = DatabaseManager.getInstance();
        databaseManager.initDatabase();

        this.conversationRepository = new ConversationRepositoryImpl(databaseManager);
        this.messageRepository = new MessageRepositoryImpl(databaseManager);
        this.promptRepository = new PromptRepositoryImpl(databaseManager);

        this.settingsService = new SettingsService(preferences);
        this.aiServiceManager = new AIServiceManager(preferences);
        this.chatService = new ChatService(aiServiceManager, conversationRepository, messageRepository, preferences);
        this.promptService = new PromptService(promptRepository);
    }

    public static synchronized AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = null;
    }

    public UserPreferences getPreferences() {
        return preferences;
    }

    public void reloadPreferences() {
        PreferencesManager.savePreferences(preferences);
    }

    public ConversationRepository getConversationRepository() {
        return conversationRepository;
    }

    public MessageRepository getMessageRepository() {
        return messageRepository;
    }

    public PromptRepository getPromptRepository() {
        return promptRepository;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public AIServiceManager getAiServiceManager() {
        return aiServiceManager;
    }

    public ChatService getChatService() {
        return chatService;
    }

    public PromptService getPromptService() {
        return promptService;
    }

    public void updatePreferences(UserPreferences newPrefs) {
        preferences.setTheme(newPrefs.getTheme());
        preferences.setFontSize(newPrefs.getFontSize());
        preferences.setFontFamily(newPrefs.getFontFamily());
        preferences.setProvider(newPrefs.getProvider());
        preferences.setModel(newPrefs.getModel());
        preferences.setOllamaHost(newPrefs.getOllamaHost());
        preferences.setProviderKeys(newPrefs.getProviderKeys());
        preferences.setCustomModels(newPrefs.getCustomModels());
        PreferencesManager.savePreferences(preferences);
    }
}