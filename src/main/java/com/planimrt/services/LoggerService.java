package com.planimrt.services;

import org.springframework.stereotype.Service;

@Service
public class LoggerService {

    public void log(String message) {
        System.out.println("[LOG] " + message);
    }

    public void logError(Exception e, String context) {
        System.err.println("[ERROR] " + context + " → " + e.getMessage());
    }
}
