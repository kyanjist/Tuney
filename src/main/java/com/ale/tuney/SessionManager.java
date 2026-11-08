package com.ale.tuney;

import com.ale.tuney.model.User;
import com.ale.tuney.dao.UserDAO;
import java.io.*;
import java.util.Properties;

public class SessionManager {
    private static User currentUser = null;
    private static final String SESSION_FILE = System.getProperty("user.home") + File.separator + ".tuney_session";

    public static User getCurrentUser() { return currentUser; }

    public static void setCurrentUser(User user) {
        currentUser = user;
        if (user != null) {
            saveSession(user.getUsername());
        } else {
            clearSession();
        }
    }

    public static void logout() {
        currentUser = null;
        clearSession();
    }

    private static void saveSession(String username) {
        try {
            Properties props = new Properties();
            props.setProperty("username", username);
            try (FileOutputStream out = new FileOutputStream(SESSION_FILE)) {
                props.store(out, "Tuney Session");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void clearSession() {
        File file = new File(SESSION_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    public static boolean tryAutoLogin() {
        File file = new File(SESSION_FILE);
        if (!file.exists()) {
            return false;
        }
        try {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            }
            String username = props.getProperty("username");
            if (username != null && !username.isBlank()) {
                UserDAO userDAO = new UserDAO();
                User user = userDAO.findByUsername(username);
                if (user != null) {
                    currentUser = user;
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
