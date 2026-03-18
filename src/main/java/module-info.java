module com.lj.aichatapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires java.sql;
    requires com.fasterxml.jackson.databind;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens com.lj.aichatapp.app to javafx.fxml;
    opens com.lj.aichatapp.controllers to javafx.fxml;
    opens com.lj.aichatapp.models to com.fasterxml.jackson.databind, javafx.fxml;
    opens com.lj.aichatapp.service.ai to javafx.fxml;
    
    exports com.lj.aichatapp.app;
    exports com.lj.aichatapp.controllers;
    exports com.lj.aichatapp.models;
    exports com.lj.aichatapp.service;
    exports com.lj.aichatapp.service.ai;
    exports com.lj.aichatapp.service.ai.providers;
    exports com.lj.aichatapp.utils;
    exports com.lj.aichatapp.repository;
    exports com.lj.aichatapp.repository.impl;
    exports com.lj.aichatapp.context;
    exports com.lj.aichatapp.infrastructure.database;
    exports com.lj.aichatapp.infrastructure.preferences;
}