package com.hyphenated.card.repos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hyphenated.card.domain.Game;

/**
 * Created by Nitin on 25-10-2015.
 */
public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByStartedFalse();
    List<Game> findByName(String name);
}
