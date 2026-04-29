package edu.ufp.inf.sd.battleshipgame.Client;

import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipFactory;
import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipGameSubject;
import edu.ufp.inf.sd.battleshipgame.rmi.GameInfo;
import edu.ufp.inf.sd.battleshipgame.rmi.LobbySession;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
        BattleshipGameSubject jogo = session.getProxy(gameId);
        if (jogo == null) {
            System.out.println("Jogo '" + gameId + "' não encontrado.\n");
        } else {
            System.out.println("Entrou no jogo '" + gameId + "' com sucesso!\n");
        }
    }
}
