package com.lj.aichatapp.repository.impl;

import com.lj.aichatapp.infrastructure.database.DatabaseManager;
import com.lj.aichatapp.models.Conversation;
import com.lj.aichatapp.repository.ConversationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConversationRepositoryImpl implements ConversationRepository {

    private final DatabaseManager dbManager;

    public ConversationRepositoryImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Conversation create(String title) {
        LocalDateTime now = LocalDateTime.now();
        String sql = "INSERT INTO conversations(title, created_at) VALUES(?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, title);
            pstmt.setString(2, now.toString());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Conversation conv = new Conversation();
                    conv.setId(generatedKeys.getInt(1));
                    conv.setTitle(title);
                    conv.setCreatedAt(now);
                    return conv;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Conversation findById(int id) {
        String sql = "SELECT id, title, created_at FROM conversations WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Conversation conv = new Conversation();
                    conv.setId(rs.getInt("id"));
                    conv.setTitle(rs.getString("title"));
                    String createdAt = rs.getString("created_at");
                    if (createdAt != null) {
                        conv.setCreatedAt(LocalDateTime.parse(createdAt));
                    }
                    return conv;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Conversation> findAll() {
        List<Conversation> list = new ArrayList<>();
        String sql = "SELECT id, title, created_at FROM conversations ORDER BY created_at DESC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Conversation c = new Conversation();
                c.setId(rs.getInt("id"));
                c.setTitle(rs.getString("title"));
                String createdAt = rs.getString("created_at");
                if (createdAt != null) {
                    c.setCreatedAt(LocalDateTime.parse(createdAt));
                }
                list.add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void updateTitle(int id, String newTitle) {
        String sql = "UPDATE conversations SET title = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newTitle);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM conversations WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}