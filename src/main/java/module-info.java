module com.lj.aichatapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    opens com.lj.aichatapp.app to javafx.fxml;
    opens com.lj.aichatapp.controllers to javafx.fxml;
    opens com.lj.aichatapp.models to javafx.fxml;
    opens com.lj.aichatapp.services to javafx.fxml;
    
    exports com.lj.aichatapp.app;
    exports com.lj.aichatapp.controllers;
    exports com.lj.aichatapp.models;
    exports com.lj.aichatapp.services;
    exports com.lj.aichatapp.utils;
}