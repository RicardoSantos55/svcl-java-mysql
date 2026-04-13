package com.svcl.app;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionStore {
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, String> sessions = new ConcurrentHashMap<String, String>();

    public String create(String username) {
        byte[] buffer = new byte[24];
        secureRandom.nextBytes(buffer);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
        sessions.put(token, username);
        return token;
    }

    public String getUsername(String token) {
        return token == null ? null : sessions.get(token);
    }

    public void destroy(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
