package edu.ufp.inf.sd.battleshipgame.Client;

import edu.ufp.inf.sd.battleshipgame.GameUI;
import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipFactory;
import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipGameSubject;
import edu.ufp.inf.sd.battleshipgame.rmi.GameInfo;
import edu.ufp.inf.sd.battleshipgame.rmi.LobbySession;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class ClientApp {

    private static final String SERVICE_NAME = "BattleshipFactory";

    // Nós conhecidos — escolhe automaticamente o menos carregado (R5)
    private static final String[][] DEFAULT_SERVERS = {
        {"localhost", "1099"},
        {"localhost", "1100"}
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Battleship - Cliente ===");
        System.out.println("A procurar servidor disponível...");

        BattleshipFactory factory = ligarAoMelhorNo(args);
        if (factory == null) {
            System.err.println("Nenhum servidor disponível. Verifica se o servidor está a correr.");
            return;
        }

        // Guarda credenciais para failover automático (R5)
        String[] credenciais = new String[2]; // [username, password]
        LobbySession session = autenticar(scanner, factory, credenciais);
        if (session == null) {
            System.out.println("A encerrar o cliente.");
            return;
        }

        menuLobby(scanner, session, credenciais);
        scanner.close();
    }

    // -------------------------------------------------------------------------
    // Ligação ao servidor (balanceamento de carga — R5)
    // -------------------------------------------------------------------------

    /**
     * Tenta ligar a todos os nós conhecidos e escolhe o com menos sessões ativas.
     * Se só um estiver disponível usa esse — tolerância a falhas (R5).
     */
    private static BattleshipFactory ligarAoMelhorNo(String[] args) {
        // Override manual via --port (para testes de R5)
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equals(args[i])) {
                return tentarLigar("localhost", args[i + 1]);
            }
        }

        BattleshipFactory melhor = null;
        int menorCarga = Integer.MAX_VALUE;

        for (String[] srv : DEFAULT_SERVERS) {
            BattleshipFactory f = tentarLigar(srv[0], srv[1]);
            if (f == null) continue;
            try {
                int carga = f.getSessionCount();
                System.out.println("  Nó " + srv[0] + ":" + srv[1]
                        + " disponível (" + carga + " sessões ativas)");
                if (carga < menorCarga) {
                    menorCarga = carga;
                    melhor = f;
                }
            } catch (RemoteException e) {
                System.out.println("  Nó " + srv[0] + ":" + srv[1] + " não respondeu.");
            }
        }

        if (melhor != null) System.out.println("Ligado ao nó menos carregado.\n");
        return melhor;
    }

    private static BattleshipFactory tentarLigar(String host, String port) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, Integer.parseInt(port));
            BattleshipFactory f = (BattleshipFactory) registry.lookup(SERVICE_NAME);
            f.getSessionCount(); // confirma que está vivo
            return f;
        } catch (Exception e) {
            System.out.println("  Nó " + host + ":" + port + " indisponível.");
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Autenticação
    // -------------------------------------------------------------------------

    private static LobbySession autenticar(Scanner scanner, BattleshipFactory factory,
                                            String[] credenciais) {
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
                        credenciais[0] = username;
                        credenciais[1] = password;
                        System.out.println("Registo e login efetuados! Bem-vindo, " + username + "!");
                        printToken(session);
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
                        credenciais[0] = username;
                        credenciais[1] = password;
                        System.out.println("Login efetuado! Bem-vindo, " + username + "!");
                        printToken(session);
                        return session;
                    } catch (RemoteException e) {
                        System.out.println("Erro no login: " + e.getMessage() + "\n");
                    }
                }
                case "0" -> { return null; }
                default  -> System.out.println("Opção inválida.\n");
            }
        }
    }

    private static void printToken(LobbySession session) throws RemoteException {
        String token = session.getToken();
        String preview = token.substring(0, Math.min(token.length(), 30)) + "...";
        System.out.println("[JWT] Token de sessão: " + preview + "\n");
    }

    // -------------------------------------------------------------------------
    // Lobby — com failover automático de sessão (R5)
    // -------------------------------------------------------------------------

    private static void menuLobby(Scanner scanner, LobbySession session, String[] credenciais) {
        try {
            while (true) {
                System.out.println("--- Lobby (" + session.getUsername() + ") ---");
                System.out.println("1. Listar jogos disponíveis");
                System.out.println("2. Criar novo jogo (e entrar)");
                System.out.println("3. Entrar num jogo existente");
                System.out.println("0. Logout e sair");
                System.out.print("Opção: ");
                String opcao = scanner.nextLine().trim();

                switch (opcao) {
                    case "1" -> listarJogos(session);
                    case "2" -> criarEEntrarJogo(scanner, session);
                    case "3" -> entrarJogo(scanner, session);
                    case "0" -> {
                        try { session.logout(); } catch (RemoteException ignored) {}
                        System.out.println("Sessão terminada. Até logo!\n");
                        return;
                    }
                    default -> System.out.println("Opção inválida.\n");
                }
            }
        } catch (RemoteException e) {
            // Servidor caiu — tentativa de failover automático (R5)
            System.out.println("\n[R5] Servidor indisponível: " + e.getMessage());
            System.out.println("[R5] A procurar servidor de backup...");

            BattleshipFactory backup = ligarAoMelhorNo(new String[0]);
            if (backup == null) {
                System.err.println("[R5] Nenhum servidor disponível. A terminar.");
                return;
            }
            try {
                LobbySession novaSession = backup.login(credenciais[0], credenciais[1]);
                System.out.println("[R5] Reconectado! Sessão restaurada para '" + credenciais[0] + "'.\n");
                menuLobby(scanner, novaSession, credenciais); // continua no backup
            } catch (RemoteException ex) {
                System.err.println("[R5] Falha na reconexão: " + ex.getMessage());
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

    // -------------------------------------------------------------------------
    // Criar jogo e entrar imediatamente
    // -------------------------------------------------------------------------

    private static void criarEEntrarJogo(Scanner scanner, LobbySession session)
            throws RemoteException {
        String mode = perguntarModo(scanner);
        BattleshipGameSubject jogo = session.createGame(mode);
        if (jogo == null) {
            System.out.println("Erro ao criar o jogo.\n");
            return;
        }
        String gameId = jogo.getGameId();
        System.out.println("Jogo criado: " + gameId + "  [modo: " + mode + "]");
        System.out.println("Aguarda que o adversário entre...\n");
        abrirJanelaJogo(jogo, gameId, session.getUsername(), "PUBSUB".equals(mode));
    }

    // -------------------------------------------------------------------------
    // Entrar num jogo existente
    // -------------------------------------------------------------------------

    private static void entrarJogo(Scanner scanner, LobbySession session)
            throws RemoteException {
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

        String mode = jogo.getGameMode();
        System.out.println("Este jogo usa modo: " + mode + ". A entrar...\n");
        abrirJanelaJogo(jogo, gameId, session.getUsername(), "PUBSUB".equals(mode));
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private static String perguntarModo(Scanner scanner) {
        System.out.println("Escolhe o modo de comunicação do jogo:");
        System.out.println("1. Observer / RMI  (síncrono)");
        System.out.println("2. Publish-Subscribe / RabbitMQ  (assíncrono, requer RabbitMQ)");
        System.out.print("Opção [1]: ");
        return "2".equals(scanner.nextLine().trim()) ? "PUBSUB" : "RMI";
    }

    private static void abrirJanelaJogo(BattleshipGameSubject jogo, String gameId,
                                         String username, boolean usePubSub) {
        CountDownLatch latch = new CountDownLatch(1);
        GameUI gameUI = new GameUI(jogo, gameId, username, usePubSub);
        gameUI.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) { latch.countDown(); }
        });
        System.out.println("[Jogo em curso na janela gráfica. Fecha a janela para voltar ao lobby.]");
        try { latch.await(); } catch (InterruptedException ignored) {}
        System.out.println("Voltou ao lobby.\n");
    }
}
