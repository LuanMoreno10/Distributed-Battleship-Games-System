package edu.ufp.inf.sd.battleshipgame.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.ufp.inf.sd.battleshipgame.pubsub.BattleshipGamePublisher;

public class BattleshipGameSubjectImpl extends UnicastRemoteObject implements BattleshipGameSubjectRI
 {
    private final String gameId;
    private final String gameMode; // "RMI" ou "PUBSUB"
    private final Map<String, BattleshipGameObserverRI> observersByUser;

    private transient BattleshipGamePublisher publisher;

    private final GameState gameState;

    public BattleshipGameSubjectImpl(String gameId, String gameMode) throws RemoteException {
        super();
        this.gameId = gameId;
        this.gameMode = gameMode != null ? gameMode.toUpperCase() : "RMI";
        this.observersByUser = new LinkedHashMap<>();
        this.gameState = new GameState(gameId);

        // Só arranca o publisher RabbitMQ se o modo escolhido for PUBSUB.
        if ("PUBSUB".equals(this.gameMode)) {
            try {
                this.publisher = new BattleshipGamePublisher(gameId);
                System.out.println("[Servidor] RabbitMQ publisher ativo para " + gameId);
            } catch (Exception e) {
                System.out.println("[Servidor] RabbitMQ não disponível para " + gameId + " — a usar só RMI.");
                this.publisher = null;
            }
        }
    }

    private void validateToken(String token) throws RemoteException {
        if (!JwtUtils.isTokenValid(token)) {
            throw new RemoteException("Token inválido ou expirado.");
        }
    }

    @Override
    public synchronized void attach(String token, String username, BattleshipGameObserverRI observer) throws RemoteException {
        validateToken(token);
        if (username == null || username.isBlank()) {
            throw new RemoteException("Username inválido.");
        }
        if (observer == null) {
            throw new RemoteException("Observer não pode ser null.");
        }
        if (observersByUser.containsKey(username)) {
            throw new RemoteException("O utilizador '" + username + "' já está ligado a este jogo.");
        }
        if (observersByUser.size() >= 2) {
            throw new RemoteException("Game is already full (max 2 players).");
        }

        observersByUser.put(username, observer);
        gameState.getPlayers().add(username);
        gameState.getPlayerStates().put(username, new PlayerState());

        gameState.touch();
        gameState.addEvent("'" + username + "' joined the game.");
        System.out.println("[Servidor] '" + username + "' attached to game " + gameId);

        if (observersByUser.size() == 1) {
            gameState.setPhase(GamePhase.WAITING_FOR_PLAYERS);
            gameState.setCurrentTurn(username);
        } else if (observersByUser.size() == 2) {
            // Com 2 jogadores começamos fase de posicionamento de navios.
            gameState.setPhase(GamePhase.PLACING_SHIPS);
            // mantém currentTurn no primeiro jogador que entrou
        }

        propagateState();
    }

    @Override
    public synchronized void detach(String token, String username) throws RemoteException {
        validateToken(token);
        if (username == null || username.isBlank()) {
            throw new RemoteException("Username inválido.");
        }
        BattleshipGameObserverRI removed = observersByUser.remove(username);
        gameState.getPlayers().remove(username);
        gameState.getPlayerStates().remove(username);

        gameState.touch();
        if (removed != null) {
            gameState.addEvent("'" + username + "' left the game.");
            System.out.println("[Servidor] '" + username + "' detached from game " + gameId);

            // Se o jogo já estava em progresso, o outro jogador ganha.
            if (gameState.getPhase() == GamePhase.IN_PROGRESS || gameState.getPhase() == GamePhase.PLACING_SHIPS) {
                gameState.setPhase(GamePhase.FINISHED);
                String winner = observersByUser.keySet().stream().findFirst().orElse(null);
                gameState.setWinner(winner);
                if (winner != null) {
                    gameState.addEvent("Game finished (opponent disconnected). Winner: " + winner);
                } else {
                    gameState.addEvent("Game finished (all players left). ");
                }
            } else if (observersByUser.isEmpty()) {
                gameState.setPhase(GamePhase.FINISHED);
            } else {
                gameState.setPhase(GamePhase.WAITING_FOR_PLAYERS);
                gameState.setCurrentTurn(observersByUser.keySet().iterator().next());
            }
        }

        propagateState();
    }

    @Override
    public synchronized void setState(String token, GameAction action) throws RemoteException {
        validateToken(token);
        if (action == null) {
            throw new RemoteException("Action não pode ser null.");
        }
        String username = action.getUsername();
        if (!observersByUser.containsKey(username)) {
            throw new RemoteException("Jogador '" + username + "' não está associado a este jogo.");
        }
        if (gameState.getPhase() == GamePhase.WAITING_FOR_PLAYERS) {
            throw new RemoteException("Aguardando por 2 jogadores antes de começar.");
        }
        if (gameState.getPhase() == GamePhase.FINISHED) {
            throw new RemoteException("O jogo já terminou.");
        }
        if (gameState.getCurrentTurn() != null && !gameState.getCurrentTurn().equals(username)) {
            throw new RemoteException("Não é a vez de '" + username + "'. Vez atual: " + gameState.getCurrentTurn());
        }

        gameState.touch();

        switch (action.getType()) {
            case PLACE_SHIP -> handlePlaceShip(username, action);
            case FIRE -> handleFire(username, action);
            case PASS -> handlePass(username);
            default -> throw new RemoteException("Ação não suportada: " + action.getType());
        }

        propagateState();
    }

    @Override
    public synchronized GameState getState(String token) throws RemoteException {
        validateToken(token);
        return gameState;
    }

    private void handlePlaceShip(String username, GameAction action) throws RemoteException {
        if (gameState.getPhase() != GamePhase.PLACING_SHIPS) {
            throw new RemoteException("Não é possível posicionar navios na fase: " + gameState.getPhase());
        }
        if (action.getShipPlacement() == null) {
            throw new RemoteException("ShipPlacement em falta.");
        }

        PlayerState ps = gameState.getPlayerStates().get(username);
        try {
            ps.placeNextShip(action.getShipPlacement());
        } catch (RuntimeException e) {
            throw new RemoteException(e.getMessage());
        }
        gameState.addEvent(username + " placed " + action.getShipPlacement());

        // Se terminou de colocar navios, passa a vez para o outro jogador.
        if (ps.allShipsPlaced()) {
            String next = getOpponent(username);
            if (next != null) {
                gameState.setCurrentTurn(next);
            }

            // Se ambos já colocaram navios, começa o jogo.
            boolean allPlaced = gameState.getPlayers().stream()
                    .allMatch(u -> gameState.getPlayerStates().get(u) != null && gameState.getPlayerStates().get(u).allShipsPlaced());
            if (allPlaced) {
                gameState.setPhase(GamePhase.IN_PROGRESS);
                // primeira jogada: primeiro jogador que entrou
                if (!gameState.getPlayers().isEmpty()) {
                    gameState.setCurrentTurn(gameState.getPlayers().get(0));
                }
                gameState.addEvent("All ships placed. Game started.");
            }
        }
    }

    private void handleFire(String username, GameAction action) throws RemoteException {
        if (gameState.getPhase() != GamePhase.IN_PROGRESS) {
            throw new RemoteException("Não é possível disparar na fase: " + gameState.getPhase());
        }
        if (action.getShot() == null) {
            throw new RemoteException("Shot em falta.");
        }

        String opponent = getOpponent(username);
        if (opponent == null) {
            throw new RemoteException("Ainda não há oponente.");
        }

        PlayerState oppState = gameState.getPlayerStates().get(opponent);
        int r = action.getShot().getTarget().getRow();
        int c = action.getShot().getTarget().getCol();
        if (oppState.getHitsReceived()[r][c]) {
            throw new RemoteException("Posição já foi alvo de tiro: " + action.getShot().getTarget());
        }

        boolean hit;
        try {
            hit = oppState.receiveShot(action.getShot());
        } catch (RuntimeException e) {
            throw new RemoteException(e.getMessage());
        }

        gameState.addEvent(username + " fired at " + action.getShot().getTarget() + " => " + (hit ? "HIT" : "MISS"));

        if (oppState.isAllShipsSunk()) {
            gameState.setPhase(GamePhase.FINISHED);
            gameState.setWinner(username);
            gameState.addEvent("All ships sunk. Winner: " + username);
            gameState.setCurrentTurn(null);
        } else {
            // troca de turno
            gameState.setCurrentTurn(opponent);
        }
    }

    private void handlePass(String username) throws RemoteException {
        if (gameState.getPhase() != GamePhase.IN_PROGRESS) {
            throw new RemoteException("Só é possível passar a vez durante o jogo.");
        }
        String opponent = getOpponent(username);
        if (opponent == null) {
            throw new RemoteException("Ainda não há oponente.");
        }
        gameState.addEvent(username + " passed the turn.");
        gameState.setCurrentTurn(opponent);
    }

    private String getOpponent(String username) {
        List<String> ps = gameState.getPlayers();
        if (ps.size() < 2) return null;
        return ps.get(0).equals(username) ? ps.get(1) : ps.get(0);
    }

    private void propagateState() {
        notifyObserversSafe();
        publishSafe();
        maybeClosePublisher();
    }

    private void maybeClosePublisher() {
        if (publisher == null) return;
        if (gameState.getPhase() == GamePhase.FINISHED) {
            try {
                publisher.close();
            } catch (Exception ignored) {
            } finally {
                publisher = null;
            }
        }
    }

    private void notifyObserversSafe() {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, BattleshipGameObserverRI> entry : observersByUser.entrySet()) {
            BattleshipGameObserverRI observer = entry.getValue();
            try {
                observer.update(gameState);
            } catch (RemoteException e) {
                System.out.println("[Servidor] Observer de '" + entry.getKey() + "' não responde. A remover...");
                toRemove.add(entry.getKey());
            }
        }

        for (String u : toRemove) {
            observersByUser.remove(u);
            gameState.getPlayers().remove(u);
            gameState.getPlayerStates().remove(u);
        }
    }

    private void publishSafe() {
        if (publisher == null) return;
        try {
            publisher.publish(gameState);
        } catch (Exception e) {
            System.err.println("[Servidor] Erro a publicar estado via RabbitMQ (" + gameId + "): " + e.getMessage());
        }
    }

    @Override
    public int getPlayerCount() throws RemoteException {
        return observersByUser.size();
    }

    @Override
    public String getGameId() throws RemoteException {
        return gameId;
    }

    @Override
    public String getGameMode() throws RemoteException {
        return gameMode;
    }
}
