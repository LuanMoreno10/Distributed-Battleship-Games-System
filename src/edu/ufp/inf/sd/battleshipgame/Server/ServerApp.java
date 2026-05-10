package edu.ufp.inf.sd.battleshipgame.Server;

import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipFactory;
import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipFactoryImpl;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;

public class ServerApp {
    public static void main(String[] args) {
        boolean pubSubEnabled = false;
        for (String a : args) {
            if ("--pubsub".equalsIgnoreCase(a)) {
                pubSubEnabled = true;
            }
            if ("--help".equalsIgnoreCase(a) || "-h".equalsIgnoreCase(a)) {
                System.out.println("Uso: ServerApp [--pubsub]\n" +
                        "  --pubsub  ativa propagação adicional via RabbitMQ (Publish/Subscribe)");
                return;
            }
        }

        try {
            // O registry tem de ser criado no mesmo JVM para que as classes sejam encontradas.
            // Ligar a um registry externo (rmiregistry) causaria ClassNotFoundException
            // porque esse processo não tem as classes no seu classpath.
            Registry registry = LocateRegistry.createRegistry(1099);

            BattleshipFactory factory = new BattleshipFactoryImpl(pubSubEnabled);
            registry.rebind("BattleshipFactory", factory);

            System.out.println("Servidor RMI iniciado na porta 1099. À espera de clientes..." +
                    (pubSubEnabled ? " (Pub/Sub RabbitMQ ON)" : ""));

        } catch (RemoteException e) {
            if (e instanceof ExportException) {
                System.err.println("Porta 1099 já está em uso. Termina o processo anterior e tenta novamente.");
            } else {
                System.err.println("Erro RMI na inicialização do ServerApp: " + e.getMessage());
            }
        }
    }
}
