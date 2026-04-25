package edu.du.et.chatapp.infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.du.et.chatapp.infrastructure.preferences.PreferencesManager;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

  private static DatabaseManager instance;
  private final HikariDataSource dataSource;

  private DatabaseManager() {
    Path dbPath = PreferencesManager.getAppDirectory().resolve("chat_history.db");
    String dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString();

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(dbUrl);
    // SQLite locking can occur with multiple connections, so we keep pool size minimal.
    // However, HikariCP still provides robust connection validation and pooling.
    config.setMaximumPoolSize(1);
    config.setConnectionTestQuery("SELECT 1");
    // HikariCP will close connections automatically when the pool is shut down
    
    this.dataSource = new HikariDataSource(config);
  }

  public static synchronized DatabaseManager getInstance() {
    if (instance == null) {
      instance = new DatabaseManager();
    }
    return instance;
  }

  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  public void initDatabase() {
    PreferencesManager.ensureAppDirectory();

    try (Connection conn = getConnection()) {
      conn.setAutoCommit(false);

      try (Statement stmt = conn.createStatement()) {
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS conversations ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT, "
                + "created_at TEXT)");

        stmt.execute(
            "CREATE TABLE IF NOT EXISTS messages ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "conversation_id INTEGER, "
                + "role TEXT, "
                + "content TEXT, "
                + "timestamp TEXT, "
                + "FOREIGN KEY(conversation_id) REFERENCES conversations(id) ON DELETE CASCADE)");

        stmt.execute(
            "CREATE TABLE IF NOT EXISTS prompts ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT, "
                + "text TEXT)");

        seedInitialPrompts(conn);
        conn.commit();
      } catch (SQLException e) {
        conn.rollback();
        e.printStackTrace();
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void seedInitialPrompts(Connection conn) throws SQLException {
    String sqlCheck = "SELECT COUNT(*) FROM prompts";
    try (Statement checkStmt = conn.createStatement();
        ResultSet rs = checkStmt.executeQuery(sqlCheck)) {

      if (rs.next() && rs.getInt(1) == 0) {
        String[][] initialPrompts = {
          {"Tutor Me", "Act as my personal tutor for [Subject]. I am a [Year/Grade Level] student.\nMy goal is to understand [Specific Topic].\nStart by explaining the core concepts in a simple way. Then, give me a simple example.\nAfter I confirm I understand, ask me a question to test my knowledge.\nWait for my answer before providing feedback.\nLet's start."},
          {"Quiz Me", "You are a quiz master. Create a 5-question multiple-choice quiz for me on the topic of [Specific Topic].\nI am a [Year/Grade Level] student.\nThe difficulty should be [Easy/Medium/Hard].\nProvide the questions one by one. Wait for my answer for each question before moving to the next.\nAt the end of the quiz, give me my score and a brief explanation for any questions I got wrong."},
          {"Explain Like I'm a Beginner", "I am a complete beginner. Explain the concept of [Concept Name] to me.\nUse simple language and analogies. Avoid technical jargon as much as possible.\nBreak down the explanation into small, easy-to-digest paragraphs.\nStart with a high-level overview, then dive into the details.\nProvide a real-world example to help me connect with the concept."},
          {"Project Idea Generator", "I am a student of [Field of Study] looking for a project idea.\nMy interests are [List of Interests].\nMy current skill level in [Programming Language/Technology] is [Beginner/Intermediate/Advanced].\nSuggest 3 unique project ideas that I can build.\nFor each idea, provide:\n1. A brief description.\n2. The key features to implement.\n3. The technologies I could use.\nThe project should be challenging but achievable within [Timeframe, e.g., 2 weeks]."},
          {"Study Planner", "Act as an academic advisor. I need to create a study plan for my upcoming [Exam Name] exam on [Date of Exam].\nThe subjects to cover are:\n- [Subject 1]\n- [Subject 2]\n- [Subject 3]\nI have [Number] days to study. I can study for [Number] hours per day.\nCreate a day-by-day study schedule for me. The schedule should include which subject to study, for how long, and specific topics to cover each day.\nInclude short breaks and revision sessions in the plan."}
        };

        String sqlInsert = "INSERT INTO prompts(title, text) VALUES(?, ?)";
        try (var pstmt = conn.prepareStatement(sqlInsert)) {
          for (String[] prompt : initialPrompts) {
            pstmt.setString(1, prompt[0]);
            pstmt.setString(2, prompt[1]);
            pstmt.addBatch();
          }
          pstmt.executeBatch();
        }
      }
    }
  }

  public void closePool() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
  }
}