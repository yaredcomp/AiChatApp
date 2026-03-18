package com.lj.aichatapp.repository.impl;

import com.lj.aichatapp.infrastructure.database.DatabaseManager;
import com.lj.aichatapp.models.Prompt;
import com.lj.aichatapp.repository.PromptRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PromptRepositoryImpl implements PromptRepository {

    private final DatabaseManager dbManager;

    public PromptRepositoryImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public List<Prompt> findAll() {
        List<Prompt> list = new ArrayList<>();
        String sql = "SELECT id, title, text FROM prompts ORDER BY title ASC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Prompt p = new Prompt();
                p.setId(rs.getInt("id"));
                p.setTitle(rs.getString("title"));
                p.setText(rs.getString("text"));
                list.add(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void save(Prompt prompt) {
        if (prompt.getId() > 0) {
            update(prompt);
        } else {
            insert(prompt);
        }
    }

    private void insert(Prompt prompt) {
        String sql = "INSERT INTO prompts(title, text) VALUES(?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, prompt.getTitle());
            pstmt.setString(2, prompt.getText());
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void update(Prompt prompt) {
        String sql = "UPDATE prompts SET title = ?, text = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, prompt.getTitle());
            pstmt.setString(2, prompt.getText());
            pstmt.setInt(3, prompt.getId());
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM prompts WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}