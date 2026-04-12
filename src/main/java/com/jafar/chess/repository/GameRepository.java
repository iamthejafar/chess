package com.jafar.chess.repository;

import com.jafar.chess.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, String> {

    List<Game> findByWhiteUserIdOrBlackUserIdOrderByStartTimeDescGameIdDesc(String whiteUserId, String blackUserId);

    @Query("""
            select g from Game g
            where (g.whiteUserId = :userId or g.blackUserId = :userId)
              and g.gameOver = false
            order by g.startTime desc, g.gameId desc
            """)
    List<Game> findActiveGamesByUserId(@Param("userId") String userId);

    default Optional<Game> findLatestActiveByUserId(String userId) {
        return findActiveGamesByUserId(userId).stream().findFirst();
    }
}

