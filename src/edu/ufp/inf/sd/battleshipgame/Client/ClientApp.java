package edu.ufp.inf.sd.battleshipgame.Client;

import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipFactory;
import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipGameObserver;
import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipGameSubject;
import edu.ufp.inf.sd.battleshipgame.rmi.Coordinate;
import edu.ufp.inf.sd.battleshipgame.rmi.GameAction;
import edu.ufp.inf.sd.battleshipgame.rmi.GameInfo;
import edu.ufp.inf.sd.battleshipgame.rmi.GameState;
import edu.ufp.inf.sd.battleshipgame.rmi.Orientation;
import edu.ufp.inf.sd.battleshipgame.rmi.PlayerState;
import edu.ufp.inf.sd.battleshipgame.rmi.ShipPlacement;
import edu.ufp.inf.sd.battleshipgame.rmi.Shot;
import edu.ufp.inf.sd.battleshipgame.rmi.LobbySession;

import edu.ufp.inf.sd.battleshipgame.pubsub.BattleshipGameConsumer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Scanner;

public class ClientApp {

    private static final String HOST = "localhost";
    private static final int PORT = 1099;
    private static final String SERVICE_NAME = "BattleshipFactory";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Battleship - Cliente ===");
        System.out.println("A ligar ao servidor " + HOST + ":" + PORT + "...");

        BattleshipFactory factory;
        try {
            Registry registry = LocateRegistry.getRegistry(HOST, PORT);
            factory = (BattleshipFactory) registry.lookup(SERVICE_NAME);
            System.out.println("Ligado com sucesso!\n");
        } catch (Exception e) {
            System.err.println("Erro ao ligar ao servidor: " + e.getMessage());
            return;
        }

        LobbySession session = autenticar(scanner, factory);
        if (session == null) {
            System.out.println("A encerrar o cliente.");
            return;
        }

        menuLobby(scanner, session);

        scanner.close();
    }

    private static LobbySession autenticar(Scanner scanner, BattleshipFactory factory) {
        while (true) {
            System.out.println("--- Autenticação ---");
            System.out.println("1. Registar");
            System.out.println("2. Login");
            System.out.println("0. Sair");
            System.out.print("Opção: ");
            String opcao = scanner.nextLine().trim();

            switch (opcao) {
                case "1" -> {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    System.out.print("Password: ");
                    String password = scanner.nextLine().trim();
                    try {
                        LobbySession session = factory.register(username, password);
                        System.out.println("Registo e login efetuados com sucesso! Bem-vindo, " + username + "!\n");
                        return session;
                    } catch (RemoteException e) {
                        System.out.println("Erro no registo: " + e.getMessage() + "\n");
                    }
                }
                case "2" -> {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    System.out.print("Password: ");
                    String password = scanner.nextLine().trim();
                    try {
                        LobbySession session = factory.login(username, password);
                        System.out.println("Login efetuado com sucesso! Bem-vindo, " + username + "!\n");
                        return session;
                    } catch (RemoteException e) {
                        System.out.println("Erro no login: " + e.getMessage() + "\n");
                    }
                }
                case "0" -> {
                    return null;
                }
                default -> System.out.println("Opção inválida.\n");
            }
        }
    }

    private static void menuLobby(Scanner scanner, LobbySession session) {
        while (true) {
            try {
                System.out.println("--- Lobby (" + session.getUsername() + ") ---");
                System.out.println("1. Listar jogos disponíveis");
                System.out.println("2. Criar novo jogo");
                System.out.println("3. Entrar num jogo existente");
                System.out.println("0. Sair");
                System.out.print("Opção: ");
                String opcao = scanner.nextLine().trim();

                switch (opcao) {
                    case "1" -> listarJogos(session);
                    case "2" -> criarJogo(session);
                    case "3" -> entrarJogo(scanner, session);
                    case "0" -> {
                        System.out.println("A sair do lobby...");
                        return;
                    }
                    default -> System.out.println("Opção inválida.\n");
                }
            } catch (RemoteException e) {
                System.err.println("Erro de comunicação com o servidor: " + e.getMessage());
                return;
            }
        }
    }

    private static void listarJogos(LobbySession session) throws RemoteException {
        List<GameInfo> jogos = session.listGames();
        if (jogos.isEmpty()) {
            System.out.println("Não há jogos disponíveis.\n");
        } else {
            System.out.println("Jogos disponíveis:");
            jogos.forEach(info -> System.out.println("  - " + info));
            System.out.println();
        }
    }

    private static void criarJogo(LobbySession session) throws RemoteException {
        BattleshipGameSubject jogo = session.createGame();
        if (jogo != null) {
            System.out.println("Jogo criado com sucesso! A aguardar oponente...\n");
        } else {
            System.out.println("Erro ao criar o jogo.\n");
        }
    }

    private static void entrarJogo(Scanner scanner, LobbySession session) throws RemoteException {
        listarJogos(session);
        System.out.print("ID do jogo: ");
        String gameId = scanner.nextLine().trim();
        BattleshipGameSubject jogo;
        try {
            jogo = session.getProxy(gameId);
        } catch (RemoteException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
            return;
        }

        String username = session.getUsername();

        System.out.println("Entrou no jogo '" + gameId + "' com sucesso!");
        System.out.println("Escolhe modo de atualização:");
        System.out.println("1. Observer (RMI callbacks, síncrono/transiente)");
        System.out.println("2. Publish/Subscribe (RabbitMQ, assíncrono/persistente)");
        System.out.print("Opção: ");
        String mode = scanner.nextLine().trim();

        BattleshipGameConsumer consumer = null;
        BattleshipGameObserver observer;
        try {
            if ("2".equals(mode)) {
                consumer = new BattleshipGameConsumer(gameId, username);
                BattleshipGameConsumer finalConsumer = consumer;
                new Thread(() -> {
                    try {
                        finalConsumer.start(state -> {
                            List<String> log = state.getEventLog();
                            String last = log.isEmpty() ? "(sem eventos)" : log.get(log.size() - 1);
                            System.out.println("\n[PubSub UPDATE] " + last);
                            System.out.println("              phase=" + state.getPhase() + ", turn=" + state.getCurrentTurn() + ", winner=" + state.getWinner());
                            System.out.print("> ");
                        });
                    } catch (Exception ex) {
                        System.err.println("[RabbitMQ] Erro no consumer: " + ex.getMessage());
                    }
                }, "rabbit-consumer").start();
                observer = new NullObserver();
            } else {
                observer = new ClientGameObserverImpl();
            }
        } catch (Exception e) {
            System.err.println("Falha ao iniciar mecanismo de updates: " + e.getMessage());
            observer = new ClientGameObserverImpl();
        }

        jogo.attach(username, observer);
        System.out.println("Aguardando oponente / a iniciar jogo...\n");

        try {
            menuJogo(scanner, jogo, username);
        } finally {
            try {
                jogo.detach(username);
            } catch (Exception ignored) {
            }
            if (consumer != null) {
                try {
                    consumer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void menuJogo(Scanner scanner, BattleshipGameSubject jogo, String username) throws RemoteException {
        while (true) {
            System.out.println("--- Jogo (" + username + ") ---");
            System.out.println("1. Ver estado");
            System.out.println("2. Posicionar próximo navio");
            System.out.println("3. Disparar tiro");
            System.out.println("4. Passar a vez");
            System.out.println("0. Sair do jogo");
            System.out.print("> ");

            String opcao = scanner.nextLine().trim();
            switch (opcao) {
                case "1" -> printState(jogo.getState(), username);
                case "2" -> doPlaceShip(scanner, jogo, username);
                case "3" -> doFire(scanner, jogo, username);
                case "4" -> jogo.setState(GameAction.pass(username));
                case "0" -> {
                    System.out.println("A sair do jogo...\n");
                    return;
                }
                default -> System.out.println("Opção inválida.\n");
            }
        }
    }

    private static void doPlaceShip(Scanner scanner, BattleshipGameSubject jogo, String username) throws RemoteException {
        GameState state = jogo.getState();
        PlayerState ps = state.getPlayerStates().get(username);
        if (ps == null) {
            System.out.println("Ainda não está associado ao jogo.\n");
            return;
        }
        int len = ps.nextShipLength();
        if (len <= 0) {
            System.out.println("Já posicionaste todos os navios.\n");
            return;
        }

        System.out.println("Comprimento do próximo navio: " + len);
        System.out.print("Coordenada inicial (ex: A1): ");
        Coordinate start = parseCoord(scanner.nextLine().trim());
        System.out.print("Orientação (H/V): ");
        String o = scanner.nextLine().trim().toUpperCase();
        Orientation orientation = o.startsWith("V") ? Orientation.VERTICAL : Orientation.HORIZONTAL;

        ShipPlacement placement = new ShipPlacement(start, len, orientation);
        jogo.setState(GameAction.placeShip(username, placement));
    }

    private static void doFire(Scanner scanner, BattleshipGameSubject jogo, String username) throws RemoteException {
        System.out.print("Alvo (ex: B7): ");
        Coordinate target = parseCoord(scanner.nextLine().trim());
        jogo.setState(GameAction.fire(username, new Shot(target)));
    }

    private static void printState(GameState state, String username) {
        System.out.println("Estado: gameId=" + state.getGameId() + ", phase=" + state.getPhase() +
                ", players=" + state.getPlayers() + ", turn=" + state.getCurrentTurn() + ", winner=" + state.getWinner());
        PlayerState ps = state.getPlayerStates().get(username);
        if (ps != null) {
            System.out.println("Navios por posicionar: " + ps.shipsRemainingToPlace());
        }
        List<String> log = state.getEventLog();
        int from = Math.max(0, log.size() - 5);
        System.out.println("Últimos eventos:");
        for (int i = from; i < log.size(); i++) {
            System.out.println("  - " + log.get(i));
        }
        System.out.println();
    }

    private static Coordinate parseCoord(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("Coordenada vazia.");
        }
        s = s.trim().toUpperCase();
        char rowCh = s.charAt(0);
        if (rowCh < 'A' || rowCh > 'J') {
            throw new IllegalArgumentException("Linha inválida (A-J): " + s);
        }
        int row = rowCh - 'A';
        int col;
        try {
            col = Integer.parseInt(s.substring(1)) - 1;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Coluna inválida: " + s);
        }
        if (col < 0 || col >= 10) {
            throw new IllegalArgumentException("Coluna inválida (1-10): " + s);
        }
        return new Coordinate(row, col);
    }

    /** Observer vazio para o modo Pub/Sub (associação por attach continua a ser necessária). */
    private static class NullObserver extends UnicastRemoteObject implements BattleshipGameObserver {
        protected NullObserver() throws RemoteException {
            super();
        }

        @Override
        public void update(GameState state) throws RemoteException {
            // no-op
        }
    }
}
