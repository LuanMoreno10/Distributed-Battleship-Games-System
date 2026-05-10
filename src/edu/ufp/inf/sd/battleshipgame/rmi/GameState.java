package edu.ufp.inf.sd.battleshipgame.rmi;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Snapshot do estado do jogo. É enviado por cópia (Serializable) para clientes.
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String gameId;
    private GamePhase phase = GamePhase.WAITING_FOR_PLAYERS;

    private final List<String> players = new ArrayList<>(2);
    private final Map<String, PlayerState> playerStates = new LinkedHashMap<>();

    private String currentTurn;
    private String winner;

    private final List<String> eventLog = new ArrayList<>();
    private Instant lastUpdated = Instant.now();

    public GameState(String gameId) {
        this.gameId = gameId;
        eventLog.add("[" + gameId + "] Game created.");
    }

    public String getGameId() {
        return gameId;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public List<String> getPlayers() {
        return players;
    }

    public Map<String, PlayerState> getPlayerStates() {
        return playerStates;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public List<String> getEventLog() {
        return eventLog;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void touch() {
        this.lastUpdated = Instant.now();
    }

    public void addEvent(String event) {
        eventLog.add("[" + lastUpdated + "] " + event);
    }

    @Override
    public String toString() {
        return "GameState{" +
                "gameId='" + gameId + '\'' +
                ", phase=" + phase +
                ", players=" + players +
                ", currentTurn='" + currentTurn + '\'' +
                ", winner='" + winner + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", events=" + eventLog.size() +
                '}';
    }
}

