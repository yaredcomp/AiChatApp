package com.lj.aichatapp.repository.impl;

import com.lj.aichatapp.infrastructure.database.DatabaseManager;
import com.lj.aichatapp.models.Message;
import com.lj.aichatapp.models.MessageRole;
import com.lj.aichatapp.repository.MessageRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageRepositoryImpl implements MessageRepository {

    private final DatabaseManager dbManager;

    public MessageRepositoryImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void save(int conversationId, Message message) {
        String sql = "INSERT INTO messages(conversation_id, role, content, timestamp) VALUES(?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, conversationId);
            pstmt.setString(2, message.getRole().name());
            pstmt.setString(3, message.getContent());
            pstmt.setString(4, LocalDateTime.now().toString());
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Message> findByConversationId(int conversationId) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT role, content FROM messages WHERE conversation_id = ? ORDER BY id ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, conversationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MessageRole role = MessageRole.valueOf(rs.getString("role"));
                    String content = rs.getString("content");
                    list.add(new Message(role, content));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void deleteByConversationId(int conversationId) {
        String sql = "DELETE FROM messages WHERE conversation_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, conversationId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}