package edu.ufp.inf.sd.battleshipgame.Server;

import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipFactory;
import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipFactoryImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerApp {
    public static void main(String[] args) {
        try {
            // Cria ou localiza o RMI Registry na porta 1099 (TCP padrão do Java)
            Registry registry;
            try {
                registry = LocateRegistry.getRegistry(1099);
                registry.list(); // Tenta usar para ver se está online
            } catch (Exception e) {
                registry = LocateRegistry.createRegistry(1099);
            }

            // Instancia a Factory
            BattleshipFactory factory = new BattleshipFactoryImpl();

            // Vincula ao Registry
            registry.rebind("BattleshipFactory", factory);

            System.out.println("Servidor RMI de Batalha Naval iniciado com sucesso na porta 1099.");

        } catch (Exception e) {
            System.err.println("Erro na inicialização do ServerApp: " + e.toString());
            e.printStackTrace();
        }
    }
}
