package com.gbkl.Chess.service;

import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Service
public class UsernameService {
    private final Set<String> usedUsernames = ConcurrentHashMap.newKeySet();
    private final String[] adjectives = {"Swift", "Clever", "Brave", "Mighty", "Royal", "Silent", "Noble", "Wise", "Quick", "Grand"};
    private final String[] chessPieces = {"King", "Queen", "Bishop", "Knight", "Rook", "Pawn"};
    private final Random random = new Random();

    public String generateUniqueUsername() {
        String username;
        do {
            String adjective = adjectives[random.nextInt(adjectives.length)];
            String piece = chessPieces[random.nextInt(chessPieces.length)];
            int number = random.nextInt(1000);
            username = String.format("%s%s%d", adjective, piece, number);
        } while (!addUsername(username));
        
        return username;
    }

    public boolean addUsername(String username) {
        return usedUsernames.add(username);
    }

    public boolean isUsernameTaken(String username) {
        return usedUsernames.contains(username);
    }

    public void releaseUsername(String username) {
        usedUsernames.remove(username);
    }
}
