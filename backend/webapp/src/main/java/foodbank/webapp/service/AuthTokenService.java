package foodbank.webapp.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthTokenService {

    private final Map<String, Integer> activeTokens =
            new ConcurrentHashMap<>();

    public String createToken(Integer userId) {
        String token = UUID.randomUUID().toString();
        activeTokens.put(token, userId);
        return token;
    }

    public boolean isActive(String token) {
        return activeTokens.containsKey(token);
    }

    public boolean revokeToken(String token) {
        return activeTokens.remove(token) != null;
    }
}