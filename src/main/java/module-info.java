module com.ale.tuney {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires spring.security.crypto;
    requires unirest.java.core;
    requires org.json;
    requires java.net.http;
    requires org.jspecify;

    opens com.ale.tuney to javafx.fxml;
    exports com.ale.tuney;
    exports com.ale.tuney.controller;
    exports com.ale.tuney.model;
    opens com.ale.tuney.controller to javafx.fxml;

}
