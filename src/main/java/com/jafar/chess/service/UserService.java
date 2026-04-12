package com.jafar.chess.service;

import com.jafar.chess.model.Game;
import com.jafar.chess.model.User;
import com.jafar.chess.shared.GameResult;
import com.jafar.chess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int DEFAULT_RATING = 1200;
    private static final int PROVISIONAL_K_FACTOR = 32;
    private static final int ESTABLISHED_K_FACTOR = 16;
    private static final int ESTABLISHED_GAMES_THRESHOLD = 30;

    private final UserRepository repository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;


    private  String getRandomUserName(){
        return "guest_" + UUID.randomUUID().toString().substring(0, 6);
    }

    public User createGuestUser(){
        LocalDateTime now = LocalDateTime.now();
        User newUser = User.builder()
                .isGuest(true)
                .id(UUID.randomUUID().toString())
                .username(getRandomUserName())
                .lastLoginAt(now)
                .build();
        return repository.save(newUser);
    }

    public User createUser(String email, String name){
        LocalDateTime now = LocalDateTime.now();
        User newUser = User.builder()
                .isGuest(false)
                .id(UUID.randomUUID().toString())
                .username(getRandomUserName())
                .email(email)
                .name(name)
                .lastLoginAt(now)
                .build();
        return  repository.save(newUser);
    }

    public User findOrCreateGoogleUser(String googleId, String email, String name, String picture) {
        if (googleId == null || googleId.isBlank()) {
            throw new IllegalArgumentException("googleId is required");
        }

        return repository.findByGoogleId(googleId)
                .orElseGet(() -> {
                    User existingByEmail = null;
                    if (email != null && !email.isBlank()) {
                        existingByEmail = repository.findByEmail(email).orElse(null);
                    }

                    if (existingByEmail != null) {
                        if (existingByEmail.getGoogleId() == null || existingByEmail.getGoogleId().isBlank()) {
                            existingByEmail.setGoogleId(googleId);
                        }
                        if (name != null && !name.isBlank()) {
                            existingByEmail.setName(name);
                        }
                        if (picture != null && !picture.isBlank()) {
                            existingByEmail.setPicture(picture);
                        }
                        existingByEmail.setLastLoginAt(LocalDateTime.now());
                        return repository.save(existingByEmail);
                    }

                    User newUser = User.builder()
                            .isGuest(false)
                            .id(UUID.randomUUID().toString())
                            .username(getRandomUserName())
                            .email(email)
                            .name(name)
                            .picture(picture)
                            .googleId(googleId)
                            .lastLoginAt(LocalDateTime.now())
                            .build();
                    return repository.save(newUser);
                });
    }

    @Transactional
    public void updateRatingsAndStatsForGame(Game game) {
        if (game == null || !game.isGameOver()) {
            return;
        }

        String whiteUserId = game.getWhiteUserId();
        String blackUserId = game.getBlackUserId();
        if (whiteUserId == null || blackUserId == null) {
            return;
        }

        User white = repository.findById(whiteUserId).orElse(null);
        User black = repository.findById(blackUserId).orElse(null);
        if (white == null || black == null) {
            return;
        }

        MatchScores scores = resolveMatchScores(game.getResult(), game.getWinnerUserId(), whiteUserId, blackUserId);

        int whiteRating = normalizeRating(white.getRating());
        int blackRating = normalizeRating(black.getRating());

        int whiteK = resolveKFactor(white.getGamesPlayed());
        int blackK = resolveKFactor(black.getGamesPlayed());

        double expectedWhite = expectedScore(whiteRating, blackRating);
        double expectedBlack = expectedScore(blackRating, whiteRating);

        int whiteDelta = (int) Math.round(whiteK * (scores.whiteScore - expectedWhite));
        int blackDelta = (int) Math.round(blackK * (scores.blackScore - expectedBlack));

        white.setRating(whiteRating + whiteDelta);
        black.setRating(blackRating + blackDelta);

        updateCounters(white, scores.whiteScore);
        updateCounters(black, scores.blackScore);

        repository.save(white);
        repository.save(black);
    }

    public User getUser(String userId){
        return repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public List<User> getAllUsers(){
        return repository.findAll();
    }

    public User updateUsername(String userId, String newUsername){
        User user = getUser(userId); // reuse method above

        user.setUsername(newUsername);

        return repository.save(user);
    }

    public User updateUser(String userId, String name, String username) {
        User user = getUser(userId);

        if (name != null) {
            String normalizedName = name.trim();
            if (normalizedName.isEmpty()) {
                throw new IllegalArgumentException("name cannot be blank");
            }
            user.setName(normalizedName);
        }

        if (username != null) {
            String normalizedUsername = username.trim();
            if (normalizedUsername.isEmpty()) {
                throw new IllegalArgumentException("username cannot be blank");
            }

            repository.findByUsername(normalizedUsername)
                    .filter(existing -> !Objects.equals(existing.getId(), userId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("username is already taken");
                    });

            user.setUsername(normalizedUsername);
        }

        return repository.save(user);
    }

    public User updateTestUserStatus(String userId, boolean isGuest){
        User user = getUser(userId);

        user.setGuest(isGuest);

        return repository.save(user);
    }

    public void deleteUser(String userId){
        User user = getUser(userId);
        deletePhotoIfLocal(user.getPicture());
        repository.deleteById(userId);
    }

    public User updateProfilePhoto(String userId, MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException("photo is required");
        }

        String contentType = photo.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("photo must be an image");
        }

        User user = getUser(userId);

        String extension = resolveExtension(photo.getOriginalFilename(), contentType);
        String fileName = UUID.randomUUID() + extension;

        Path uploadRoot = getUploadRoot();
        Path target = uploadRoot.resolve(fileName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid file name");
        }

        try {
            Files.createDirectories(uploadRoot);
            Files.copy(photo.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store profile photo", e);
        }

        deletePhotoIfLocal(user.getPicture());
        user.setPicture("/api/user/photo/" + fileName);
        return repository.save(user);
    }

    public Resource loadProfilePhoto(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        Path uploadRoot = getUploadRoot();
        Path filePath = uploadRoot.resolve(fileName).normalize();
        if (!filePath.startsWith(uploadRoot) || !Files.exists(filePath)) {
            throw new RuntimeException("Photo not found");
        }

        try {
            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Photo not found", e);
        }
    }

    public String resolvePhotoContentType(String fileName) {
        try {
            String detected = Files.probeContentType(getUploadRoot().resolve(fileName));
            return detected == null ? "application/octet-stream" : detected;
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    public boolean userExists(String userId){
        return repository.existsById(userId);
    }

    @Transactional
    public void markUserLoggedIn(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        repository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            repository.save(user);
        });
    }

    private void updateCounters(User user, double score) {
        user.setGamesPlayed(normalizeCount(user.getGamesPlayed()) + 1);
        if (score == 1.0d) {
            user.setGamesWon(normalizeCount(user.getGamesWon()) + 1);
            return;
        }
        if (score == 0.0d) {
            user.setGamesLost(normalizeCount(user.getGamesLost()) + 1);
            return;
        }
        user.setGamesDraw(normalizeCount(user.getGamesDraw()) + 1);
    }

    private MatchScores resolveMatchScores(GameResult result, String winnerUserId, String whiteUserId, String blackUserId) {
        if (result == null || result == GameResult.IN_PROGRESS) {
            return MatchScores.draw();
        }
        if (result == GameResult.DRAW) {
            return MatchScores.draw();
        }
        if (winnerUserId == null || winnerUserId.isBlank()) {
            return MatchScores.draw();
        }
        if (winnerUserId.equals(whiteUserId)) {
            return MatchScores.whiteWin();
        }
        if (winnerUserId.equals(blackUserId)) {
            return MatchScores.blackWin();
        }
        return MatchScores.draw();
    }

    private double expectedScore(int playerRating, int opponentRating) {
        return 1.0d / (1.0d + Math.pow(10.0d, (opponentRating - playerRating) / 400.0d));
    }

    private int resolveKFactor(Integer gamesPlayed) {
        return normalizeCount(gamesPlayed) < ESTABLISHED_GAMES_THRESHOLD
                ? PROVISIONAL_K_FACTOR
                : ESTABLISHED_K_FACTOR;
    }

    private int normalizeRating(Integer rating) {
        return rating == null ? DEFAULT_RATING : rating;
    }

    private int normalizeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private record MatchScores(double whiteScore, double blackScore) {
        private static MatchScores whiteWin() {
            return new MatchScores(1.0d, 0.0d);
        }

        private static MatchScores blackWin() {
            return new MatchScores(0.0d, 1.0d);
        }

        private static MatchScores draw() {
            return new MatchScores(0.5d, 0.5d);
        }
    }

    private Path getUploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0 && dot < originalFilename.length() - 1) {
                return "." + originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".img";
        };
    }

    private void deletePhotoIfLocal(String pictureUrl) {
        if (pictureUrl == null || !pictureUrl.startsWith("/api/user/photo/")) {
            return;
        }
        String fileName = pictureUrl.substring("/api/user/photo/".length());
        if (fileName.isBlank() || fileName.contains("..")) {
            return;
        }
        try {
            Files.deleteIfExists(getUploadRoot().resolve(fileName).normalize());
        } catch (IOException ignored) {
            // non-critical cleanup
        }
    }
}
