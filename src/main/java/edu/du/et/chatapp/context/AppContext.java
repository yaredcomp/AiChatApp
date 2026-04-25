package edu.du.et.chatapp.context;

import edu.du.et.chatapp.infrastructure.database.DatabaseManager;
import edu.du.et.chatapp.infrastructure.preferences.PreferencesManager;
import edu.du.et.chatapp.models.UserPreferences;
import edu.du.et.chatapp.repositories.ConversationRepository;
import edu.du.et.chatapp.repositories.MessageRepository;
import edu.du.et.chatapp.repositories.PromptRepository;
import edu.du.et.chatapp.repository.impl.ConversationRepositoryImpl;
import edu.du.et.chatapp.repository.impl.MessageRepositoryImpl;
import edu.du.et.chatapp.repository.impl.PromptRepositoryImpl;
import edu.du.et.chatapp.service.local.LlamaServerManager;
import edu.du.et.chatapp.services.ChatService;
import edu.du.et.chatapp.services.PromptService;
import edu.du.et.chatapp.services.SettingsService;
import edu.du.et.chatapp.services.ai.AIServiceManager;

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
    private final LlamaServerManager llamaServerManager;

    private AppContext() {
        PreferencesManager.ensureAppDirectory();
        this.preferences = PreferencesManager.loadPreferences();

        this.databaseManager = DatabaseManager.getInstance();
        databaseManager.initDatabase();

        this.conversationRepository = new ConversationRepositoryImpl(databaseManager);
        this.messageRepository = new MessageRepositoryImpl(databaseManager);
        this.promptRepository = new PromptRepositoryImpl(databaseManager);

        this.settingsService = new SettingsService(preferences);
        
        this.llamaServerManager = new LlamaServerManager(PreferencesManager.getAppDirectory().toString());
        this.aiServiceManager = new AIServiceManager(preferences, llamaServerManager);
        
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
    
    public LlamaServerManager getLlamaServerManager() {
        return llamaServerManager;
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