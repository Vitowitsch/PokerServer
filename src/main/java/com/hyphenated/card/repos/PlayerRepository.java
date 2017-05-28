package com.hyphenated.card.repos;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hyphenated.card.domain.Player;

/**
 * Created by Nitin on 05-11-2015.
 */
public interface PlayerRepository extends JpaRepository<Player, String> {


}
