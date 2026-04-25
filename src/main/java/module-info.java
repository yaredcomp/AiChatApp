module edu.du.et.chatapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires java.sql;
    requires com.fasterxml.jackson.databind;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires atlantafx.base;
    requires javafx.web;
    requires com.zaxxer.hikari;
    requires org.slf4j;
    opens edu.du.et.chatapp.app to javafx.fxml;
    opens edu.du.et.chatapp.controllers to javafx.fxml;
    opens edu.du.et.chatapp.models to com.fasterxml.jackson.databind, javafx.fxml;
    opens edu.du.et.chatapp.services.ai to javafx.fxml;
    
    exports edu.du.et.chatapp.app;
    exports edu.du.et.chatapp.controllers;
    exports edu.du.et.chatapp.models;
    exports edu.du.et.chatapp.services;
    exports edu.du.et.chatapp.services.ai;
    exports edu.du.et.chatapp.service.ai.providers;
    exports edu.du.et.chatapp.service.local;
    exports edu.du.et.chatapp.utils;
    exports edu.du.et.chatapp.repositories;
    exports edu.du.et.chatapp.repository.impl;
    exports edu.du.et.chatapp.context;
    exports edu.du.et.chatapp.infrastructure.database;
    exports edu.du.et.chatapp.infrastructure.preferences;
}