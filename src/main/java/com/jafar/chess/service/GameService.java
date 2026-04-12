package com.jafar.chess.service;


import com.github.bhlangonijr.chesslib.Board;
import com.jafar.chess.model.MatchResult;
import com.jafar.chess.repository.GameRepository;
import com.jafar.chess.shared.EndReason;
import com.jafar.chess.shared.GameResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import com.jafar.chess.model.Game;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private static final String GAME_CACHE_KEY_PREFIX = "chess:game:";
    private static final String DIRTY_GAME_SET_KEY = "chess:games:dirty";

    private static class PendingUser {
        private final String userId;

        private PendingUser(String userId) {
            this.userId = userId;
        }
    }

    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private final Map<String, Board> boards = new ConcurrentHashMap<>();
    private final Map<String, String> opponents = new ConcurrentHashMap<>();
    private final Map<String, String> userToGame = new ConcurrentHashMap<>();
    private final Set<String> ratingsAppliedGameIds = ConcurrentHashMap.newKeySet();

    @Autowired(required = false)
    private GameRepository gameRepository;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private UserService userService;

    @Value("${app.game.cache.ttl-seconds:21600}")
    private long gameCacheTtlSeconds;

    private final ObjectMapper cacheObjectMapper = new ObjectMapper();

    private PendingUser pendingUser;

    public synchronized MatchResult requestMatch(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        if (pendingUser == null) {
            pendingUser = new PendingUser(userId);
            return MatchResult.builder()
                    .status(MatchResult.Status.WAITING)
                    .userId(userId)
                    .build();
        }

        if (pendingUser.userId.equals(userId)) {
            return MatchResult.builder()
                    .status(MatchResult.Status.WAITING)
                    .userId(userId)
                    .build();
        }

        String opponentUserId = pendingUser.userId;
        pendingUser = null;

        String gameId = UUID.randomUUID().toString();

        Board board = new Board();


        boolean pendingIsWhite = ThreadLocalRandom.current().nextBoolean();
        String whiteUserId = pendingIsWhite ? opponentUserId : userId;
        String blackUserId = pendingIsWhite ? userId : opponentUserId;

        Game game = Game.builder()
                .gameId(gameId)
                .whiteUserId(whiteUserId)
                .blackUserId(blackUserId)
                .moveCount(0)
                .startTime(LocalDateTime.now())
                .fen(board.getFen())
                .build();

        games.put(gameId, game);
        boards.put(gameId, board);
        indexParticipants(game);
        persistGame(game);
        cacheGame(game, false);

        return MatchResult.builder()
                .status(MatchResult.Status.MATCHED)
                .userId(userId)
                .opponentUserId(opponentUserId)
                .gameId(gameId)
                .build();
    }

    public synchronized void clearPendingIfMatches(String userId) {
        if (pendingUser != null && pendingUser.userId.equals(userId)) {
            pendingUser = null;
        }
    }

    public String findOpponent(String userId) {
        String opponent = opponents.get(userId);
        if (opponent != null) {
            return opponent;
        }
        Game activeGame = findLatestActiveByUserId(userId).orElse(null);
        if (activeGame == null) {
            return null;
        }
        indexParticipants(activeGame);
        return activeGame.findOpponent(userId);
    }

    public String findGameId(String userId) {
        String mappedGameId = userToGame.get(userId);
        if (mappedGameId != null) {
            return mappedGameId;
        }
        Game activeGame = findLatestActiveByUserId(userId).orElse(null);
        if (activeGame == null) {
            return null;
        }
        indexParticipants(activeGame);
        return activeGame.getGameId();
    }


    public synchronized Game getGame(String gameId){
        if (gameId == null || gameId.isBlank()) {
            return null;
        }

        Game liveGame = games.get(gameId);
        if (liveGame != null) {
            return liveGame;
        }

        Game cachedGame = getGameFromCache(gameId);
        if (cachedGame != null) {
            games.put(gameId, cachedGame);
            indexParticipants(cachedGame);
            return cachedGame;
        }

        if (gameRepository == null) {
            return null;
        }

        Optional<Game> fromDb = gameRepository.findById(gameId);
        fromDb.ifPresent(game -> {
            games.put(gameId, game);
            indexParticipants(game);
            cacheGame(game, false);
        });
        return fromDb.orElse(null);
    }

    public synchronized Board getBoard(String gameId) {
        if (gameId == null || gameId.isBlank()) {
            return null;
        }

        Board board = boards.get(gameId);
        if (board != null) {
            return board;
        }

        Game game = getGame(gameId);
        if (game == null) {
            return null;
        }

        Board restored = new Board();
        if (game.getFen() != null && !game.getFen().isBlank()) {
            restored.loadFromFen(game.getFen());
        }
        boards.put(gameId, restored);
        return restored;
    }

    public synchronized Game findActiveGameByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }

        String gameId = findGameId(userId);
        if (gameId == null || gameId.isBlank()) {
            throw new RuntimeException("No active game found for user");
        }
        Game game = getGame(gameId);
        if (game == null) {
            userToGame.remove(userId);
            throw new RuntimeException("No active game found for user");
        }
        if (!game.isParticipant(userId) || game.isGameOver()) {
            throw new RuntimeException("No active game found for user");
        }

        return game;
    }

    public synchronized UserGamesPage findGamesByUserId(String userId, int page, int size) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        List<Game> userGames;
        if (gameRepository != null) {
            userGames = gameRepository.findByWhiteUserIdOrBlackUserIdOrderByStartTimeDescGameIdDesc(userId, userId);
        } else {
            userGames = games.values().stream()
                    .filter(game -> game != null && game.isParticipant(userId))
                    .sorted(Comparator
                            .comparing(Game::getStartTime, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Game::getGameId, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }

        int totalElements = userGames.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        long requestedOffset = (long) page * size;
        int fromIndex = requestedOffset >= totalElements ? totalElements : (int) requestedOffset;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<Game> pageGames = userGames.subList(fromIndex, toIndex);
        boolean hasNext = toIndex < totalElements;

        return new UserGamesPage(page, size, totalElements, totalPages, hasNext, pageGames);
    }

    public synchronized Game offerDraw(String userId, String gameId) {
        Game game = requireActiveParticipantGame(userId, gameId, "DRAW_OFFER");
        if (game.isDrawOffered()) {
            throw new IllegalArgumentException("A draw offer is already pending");
        }
        game.offerDraw(userId);
        stageGameUpdate(game);
        return game;
    }

    public synchronized Game acceptDraw(String userId, String gameId) {
        Game game = requireActiveParticipantGame(userId, gameId, "DRAW_ACCEPT");
        String offeredBy = game.getDrawOfferedByUserId();
        if (offeredBy == null) {
            throw new IllegalArgumentException("No draw offer to accept");
        }
        if (offeredBy.equals(userId)) {
            throw new IllegalArgumentException("You cannot accept your own draw offer");
        }
        game.conclude(GameResult.DRAW, EndReason.MUTUAL_AGREEMENT, null);
        stageGameUpdate(game);
        applyRatingsIfGameOver(game);
        return game;
    }

    public synchronized Game declineDraw(String userId, String gameId) {
        Game game = requireActiveParticipantGame(userId, gameId, "DRAW_DECLINE");
        String offeredBy = game.getDrawOfferedByUserId();
        if (offeredBy == null) {
            throw new IllegalArgumentException("No draw offer to decline");
        }
        if (offeredBy.equals(userId)) {
            throw new IllegalArgumentException("You cannot decline your own draw offer");
        }
        game.retractDrawOffer();
        stageGameUpdate(game);
        return game;
    }

    public synchronized Game resign(String userId, String gameId) {
        Game game = requireActiveParticipantGame(userId, gameId, "RESIGN");
        String winnerUserId = game.findOpponent(userId);
        game.conclude(GameResult.RESIGNATION, EndReason.RESIGNATION, winnerUserId);
        stageGameUpdate(game);
        applyRatingsIfGameOver(game);
        return game;
    }

    public synchronized void stageLiveUpdate(Game game, Board board) {
        if (game == null) {
            return;
        }
        if (board != null) {
            game.updateFen(board.getFen());
            boards.put(game.getGameId(), board);
        }
        stageGameUpdate(game);
    }

    public synchronized void applyRatingsIfGameOver(Game game) {
        if (game == null || !game.isGameOver()) {
            return;
        }
        flushGameImmediately(game);
        if (!ratingsAppliedGameIds.add(game.getGameId())) {
            return;
        }
        if (userService == null) {
            return;
        }
        userService.updateRatingsAndStatsForGame(game);
    }

    public synchronized void clearDrawOffer(String gameId) {
        Game game = games.get(gameId);
        if (game == null || game.isGameOver()) {
            return;
        }
        if (game.isDrawOffered()) {
            game.retractDrawOffer();
            stageGameUpdate(game);
        }
    }

    private Game requireActiveParticipantGame(String userId, String gameId, String action) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required for " + action);
        }
        if (gameId == null || gameId.isBlank()) {
            throw new IllegalArgumentException("gameId is required for " + action);
        }
        Game game = getGame(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found");
        }
        if (!game.isParticipant(userId)) {
            throw new IllegalArgumentException("User is not part of this game");
        }
        if (game.isGameOver()) {
            throw new IllegalArgumentException("Game is already over");
        }
        return game;
    }

    public synchronized void removeUser(String userId) {
        if (userId == null) {
            return;
        }

        clearPendingIfMatches(userId);

        String opponentUserId = opponents.remove(userId);
        String gameId = userToGame.remove(userId);

        if (opponentUserId != null) {
            opponents.remove(opponentUserId);
            userToGame.remove(opponentUserId);
        }

        if (gameId != null) {
            Game game = games.get(gameId);
            if (game != null && !game.isGameOver()) {
                String winnerUserId = game.findOpponent(userId);
                game.conclude(GameResult.ABANDONED, EndReason.ABANDONED, winnerUserId);
                stageGameUpdate(game);
                applyRatingsIfGameOver(game);
            }
            boards.remove(gameId);
        }
    }

    @Scheduled(fixedDelayString = "${app.game.persistence.flush-interval-ms:3000}")
    public synchronized void flushDirtyGamesToDatabase() {
        if (gameRepository == null || stringRedisTemplate == null) {
            return;
        }

        Set<String> dirtyGameIds = stringRedisTemplate.opsForSet().members(DIRTY_GAME_SET_KEY);
        if (dirtyGameIds == null || dirtyGameIds.isEmpty()) {
            return;
        }

        for (String gameId : dirtyGameIds) {
            Game game = getGameFromCache(gameId);
            if (game == null) {
                game = games.get(gameId);
            }
            if (game == null) {
                stringRedisTemplate.opsForSet().remove(DIRTY_GAME_SET_KEY, gameId);
                continue;
            }
            persistGame(game);
            stringRedisTemplate.opsForSet().remove(DIRTY_GAME_SET_KEY, gameId);
        }
    }

    private void stageGameUpdate(Game game) {
        if (game == null || game.getGameId() == null || game.getGameId().isBlank()) {
            return;
        }
        games.put(game.getGameId(), game);
        indexParticipants(game);
        cacheGame(game, true);
    }

    private void persistGame(Game game) {
        if (gameRepository == null || game == null) {
            return;
        }
        gameRepository.save(game);
    }

    private void flushGameImmediately(Game game) {
        if (game == null || game.getGameId() == null) {
            return;
        }
        persistGame(game);
        cacheGame(game, false);
        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForSet().remove(DIRTY_GAME_SET_KEY, game.getGameId());
        }
    }

    private void cacheGame(Game game, boolean markDirty) {
        if (stringRedisTemplate == null || game == null || game.getGameId() == null || game.getGameId().isBlank()) {
            return;
        }

        String cacheKey = gameCacheKey(game.getGameId());
        try {
            String serialized = cacheObjectMapper.writeValueAsString(game);
            stringRedisTemplate.opsForValue().set(cacheKey, serialized);
        } catch (Exception ignored) {
            return;
        }

        long ttlSeconds = gameCacheTtlSeconds <= 0 ? 21600 : gameCacheTtlSeconds;
        stringRedisTemplate.expire(cacheKey, Duration.ofSeconds(ttlSeconds));

        if (markDirty && stringRedisTemplate != null) {
            stringRedisTemplate.opsForSet().add(DIRTY_GAME_SET_KEY, game.getGameId());
        }
    }

    private Game getGameFromCache(String gameId) {
        if (stringRedisTemplate == null || gameId == null || gameId.isBlank()) {
            return null;
        }
        String serialized = stringRedisTemplate.opsForValue().get(gameCacheKey(gameId));
        if (serialized == null || serialized.isBlank()) {
            return null;
        }
        try {
            return cacheObjectMapper.readValue(serialized, Game.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String gameCacheKey(String gameId) {
        return GAME_CACHE_KEY_PREFIX + gameId;
    }

    private Optional<Game> findLatestActiveByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        if (gameRepository != null) {
            return gameRepository.findLatestActiveByUserId(userId);
        }

        return games.values().stream()
                .filter(game -> game != null && !game.isGameOver() && game.isParticipant(userId))
                .sorted(Comparator
                        .comparing(Game::getStartTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Game::getGameId, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst();
    }

    private void indexParticipants(Game game) {
        if (game == null || game.getGameId() == null || game.getGameId().isBlank()) {
            return;
        }

        String whiteUserId = game.getWhiteUserId();
        String blackUserId = game.getBlackUserId();
        if (whiteUserId == null || blackUserId == null) {
            return;
        }

        opponents.put(whiteUserId, blackUserId);
        opponents.put(blackUserId, whiteUserId);

        if (!game.isGameOver()) {
            userToGame.put(whiteUserId, game.getGameId());
            userToGame.put(blackUserId, game.getGameId());
            return;
        }

        userToGame.remove(whiteUserId, game.getGameId());
        userToGame.remove(blackUserId, game.getGameId());
    }

    public record UserGamesPage(
            int page,
            int size,
            int totalElements,
            int totalPages,
            boolean hasNext,
            List<Game> games) {
    }
}
