package edu.ufp.inf.sd.battleshipgame.Client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import edu.ufp.inf.sd.battleshipgame.GameUI;
import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipFactoryRI;
import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipGameSubjectRI;
import edu.ufp.inf.sd.battleshipgame.rmi.GameInfo;
import edu.ufp.inf.sd.battleshipgame.rmi.LobbySessionRI;

public class ClientApp {

    private static final String SERVICE_NAME = "BattleshipFactory";

    // Nós conhecidos — preenchidos a partir dos args --servers (setenv.sh)
    private static String[][] DEFAULT_SERVERS;

   

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Battleship - Cliente ===");
        System.out.println("A procurar servidor disponível...");

        BattleshipFactoryRI factory = ligarAoMelhorNo(args);

        if (factory == null) {
            System.err.println("Nenhum servidor disponível. Verifica se o servidor está a correr.");
            scanner.close();
            return;
        }

        try {
            factory.connect();
            String[] credenciais = new String[2];
            LobbySessionRI session = autenticar(scanner, factory, credenciais);
            if (session != null) {
                String token = session.getToken();
                menuLobby(scanner, session, token, credenciais);
            }
        } catch (RemoteException e) {
            System.err.println("Erro: " + e.getMessage());
        } finally {
            try { factory.disconnect(); } catch (RemoteException ignored) {}
            scanner.close();
        }
    }

    // -------------------------------------------------------------------------
    // Ligação ao servidor (balanceamento de carga — R5)
    // -------------------------------------------------------------------------

    private static BattleshipFactoryRI ligarAoMelhorNo(String[] args) {

        // parse dos argumentos --servers host:port
        if (args.length > 1 && "--servers".equals(args[0])) {
            DEFAULT_SERVERS = new String[args.length - 1][2];
            for (int i = 1; i < args.length; i++) {
                DEFAULT_SERVERS[i - 1] = args[i].split(":");
            }
        }


        List<BattleshipFactoryRI> servidores = new ArrayList<>();
        List<Integer> cargas = new ArrayList<>();
        
    
        
        for (String[] srv : DEFAULT_SERVERS) {
            try {
                Registry registry = LocateRegistry.getRegistry(srv[0], Integer.parseInt(srv[1]));
                BattleshipFactoryRI f = (BattleshipFactoryRI) registry.lookup(SERVICE_NAME);
                int carga = f.getSessionCount();
                System.out.println("  Nó " + srv[0] + ":" + srv[1] + " disponível (" + carga + " sessões)");
                servidores.add(f);
                cargas.add(carga);
            } catch (RemoteException | NotBoundException e) {
                System.out.println("  Nó " + srv[0] + ":" + srv[1] + " indisponível.");
            }
        }

        if (servidores.isEmpty()) return null;
        int indiceMelhor = cargas.indexOf(Collections.min(cargas));
        return servidores.get(indiceMelhor);
    }


    // -------------------------------------------------------------------------
    // Autenticação
    // -------------------------------------------------------------------------

    private static LobbySessionRI autenticar(Scanner scanner, BattleshipFactoryRI factory,String[] credenciais) {
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
                        LobbySessionRI session = factory.register(username, password);
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
                        LobbySessionRI session = factory.login(username, password);
                        credenciais[0] = username;
                        credenciais[1] = password;
                        System.out.println("Login efetuado! Bem-vindo, " + username + "!");
                        printToken(session);
                        return session;
                    } catch (RemoteException e) {
                        System.out.println("Erro no login: " + e.getMessage() + "\n");
                    }
                }
                case "0" -> {
                   return null; 
                }
                default  -> System.out.println("Opção inválida.\n");
            }
        }
    }

    private static void printToken(LobbySessionRI session) throws RemoteException {
        String token = session.getToken();
        String preview = token.substring(0, Math.min(token.length(), 30)) + "...";
        System.out.println("[JWT] Token de sessão: " + preview + "\n");
    }

    // -------------------------------------------------------------------------
    // Lobby — com failover automático de sessão (R5)
    // -------------------------------------------------------------------------

    private static void menuLobby(Scanner scanner, LobbySessionRI session, String token, String[] credenciais) {
        try {
            while (true) {
                System.out.println("--- Lobby (" + session.getUsername(token) + ") ---");
                System.out.println("1. Listar jogos disponíveis");
                System.out.println("2. Criar novo jogo (e entrar)");
                System.out.println("3. Entrar num jogo existente");
                System.out.println("0. Logout e sair");
                System.out.print("Opção: ");
                String opcao = scanner.nextLine().trim();

                switch (opcao) {
                    case "1" -> listarJogos(session, token);
                    case "2" -> criarEEntrarJogo(scanner, session, token);
                    case "3" -> entrarJogo(scanner, session, token);
                    case "0" -> {
                        try { session.logout(token); } catch (RemoteException ignored) {}
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

            BattleshipFactoryRI backup = ligarAoMelhorNo(new String[0]);
            if (backup == null) {
                System.err.println("[R5] Nenhum servidor disponível. A terminar.");
                return;
            }
            try {
                LobbySessionRI novaSession = backup.login(credenciais[0], credenciais[1]);
                String novoToken = novaSession.getToken();
                System.out.println("[R5] Reconectado! Sessão restaurada para '" + credenciais[0] + "'.\n");
                menuLobby(scanner, novaSession, novoToken, credenciais);
            } catch (RemoteException ex) {
                System.err.println("[R5] Falha na reconexão: " + ex.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Criar jogo e entrar imediatamente
    // -------------------------------------------------------------------------

    private static void listarJogos(LobbySessionRI session, String token) throws RemoteException {
        List<GameInfo> jogos = session.listGames(token);
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

    private static void criarEEntrarJogo(Scanner scanner, LobbySessionRI session, String token) throws RemoteException {
        String mode = perguntarModo(scanner);
        BattleshipGameSubjectRI jogo = session.createGame(token, mode);
        if (jogo == null) {
            System.out.println("Erro ao criar o jogo.\n");
            return;
        }
        String gameId = jogo.getGameId();
        System.out.println("Jogo criado: " + gameId + "  [modo: " + mode + "]");
        System.out.println("Aguarda que o adversário entre...\n");
        abrirJanelaJogo(jogo, gameId, session.getUsername(token), token, "PUBSUB".equals(mode));
    }

    // -------------------------------------------------------------------------
    // Entrar num jogo existente
    // -------------------------------------------------------------------------

    private static void entrarJogo(Scanner scanner, LobbySessionRI session, String token) throws RemoteException {
        listarJogos(session, token);
        System.out.print("ID do jogo: ");
        String gameId = scanner.nextLine().trim();

        BattleshipGameSubjectRI jogo;
        try {
            jogo = session.getProxy(token, gameId);
        } catch (RemoteException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
            return;
        }

        String mode = jogo.getGameMode();
        System.out.println("Este jogo usa modo: " + mode + ". A entrar...\n");
        abrirJanelaJogo(jogo, gameId, session.getUsername(token), token, "PUBSUB".equals(mode));
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

    private static void abrirJanelaJogo(BattleshipGameSubjectRI jogo, String gameId, String username, String token, boolean usePubSub) {
        CountDownLatch latch = new CountDownLatch(1);
        GameUI gameUI = new GameUI(jogo, gameId, username, token, usePubSub);
        gameUI.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) { latch.countDown(); }
        });
        System.out.println("[Jogo em curso na janela gráfica. Fecha a janela para voltar ao lobby.]");
        try { latch.await(); } catch (InterruptedException ignored) {}
        System.out.println("Voltou ao lobby.\n");
    }
}
