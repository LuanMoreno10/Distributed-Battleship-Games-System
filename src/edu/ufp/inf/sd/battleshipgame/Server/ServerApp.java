package edu.ufp.inf.sd.battleshipgame.Server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.concurrent.CountDownLatch;

import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipFactoryImpl;
import edu.ufp.inf.sd.battleshipgame.rmi.BattleshipFactoryPeerRI;

/**
 * Ponto de entrada do servidor.
 *
 * Uso:
 *   ServerApp [--port 1099] [--pubsub] [--peer localhost:1100]
 *
 * Para correr 2 nós (R5):
 *   Terminal 1: ServerApp --port 1099 --peer localhost:1100
 *   Terminal 2: ServerApp --port 1100 --peer localhost:1099
 */
public class ServerApp {

    private static final String SERVICE_NAME  = "BattleshipFactory";
    private static final String PEER_NAME     = "BattleshipFactoryPeer";
    private static final int    DEFAULT_PORT  = 1099;
    private static final int    PEER_RETRY_MS = 3000;
    private static final int    PEER_RETRIES  = 10;

    public static void main(String[] args) {
        int    port        = DEFAULT_PORT;
        String peerAddress = null;   // "host:port"

        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "--port" -> port        = Integer.parseInt(args[++i]);
                case "--peer" -> peerAddress = args[++i];
            }
        }

        String nodeId = "Node-" + port;

        try {
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(port);
            } catch (ExportException e) {
                System.err.println("[" + nodeId + "] Porta " + port + " já em uso.");
                return;
            }

            BattleshipFactoryImpl factory = new BattleshipFactoryImpl(nodeId);

            // Registar como serviço principal (para clientes)
            registry.rebind(SERVICE_NAME, factory);
            
            // Registar também como peer (para sincronização entre nós)
            registry.rebind(PEER_NAME, factory);

            System.out.println("[" + nodeId + "] Servidor RMI iniciado na porta " + port + "."
                    + (peerAddress != null ? " A tentar ligar ao peer: " + peerAddress : ""));

            // Se foi especificado um peer, ligamos num thread separado (R5)
            if (peerAddress != null) {
                final String addr = peerAddress;
                Thread peerThread = new Thread(() -> connectToPeer(factory, addr, nodeId), "peer-connector");
                peerThread.setDaemon(true);
                peerThread.start();
            }

            // Bloqueia o thread principal para manter o servidor vivo.
            // Ctrl+C interrompe o processo normalmente.
            System.out.println("[" + nodeId + "] Pronto. Ctrl+C para parar.");
            new CountDownLatch(1).await();

        } catch (RemoteException e) {
            System.err.println("[" + nodeId + "] Erro RMI: " + e.getMessage());
        } catch (InterruptedException ignored) {
            // Ctrl+C — encerramento normal
        }
    }

    /**
     * Tenta ligar ao peer com retries.
     * Se o peer ainda não estiver ativo, espera e volta a tentar (R5 — tolerância a falhas).
     */
    private static void connectToPeer(BattleshipFactoryImpl factory, String peerAddress, String nodeId) {
        String[] parts = peerAddress.split(":");
        String peerHost = parts[0];
        int    peerPort = Integer.parseInt(parts[1]);

        for (int attempt = 1; attempt <= PEER_RETRIES; attempt++) {
            try {
                Registry peerRegistry = LocateRegistry.getRegistry(peerHost, peerPort);
                BattleshipFactoryPeerRI peer = (BattleshipFactoryPeerRI) peerRegistry.lookup(PEER_NAME);
                peer.ping(); // confirmar que está vivo
                factory.setPeer(peer);
                System.out.println("[" + nodeId + "] Ligado ao peer " + peerAddress + " (tentativa " + attempt + ").");
                // Apresenta-se ao peer e sincroniza utilizadores nos dois sentidos
                peer.registerAsPeer(factory, factory.getUsers());

                // Monitorizar o peer: se cair, limpar a referência e tentar reconectar
                monitorPeer(factory, peerHost, peerPort, nodeId);
                return;

            } catch (Exception e) {
                System.out.println("[" + nodeId + "] Peer " + peerAddress
                        + " ainda não disponível (tentativa " + attempt + "/" + PEER_RETRIES + ").");
                try { Thread.sleep(PEER_RETRY_MS); } catch (InterruptedException ignored) {}
            }
        }
        System.out.println("[" + nodeId + "] Peer " + peerAddress
                + " não foi alcançado. A funcionar em modo autónomo.");
    }

    /**
     * Monitoriza o peer periodicamente.
     * Se detetar falha, limpa a referência e tenta reconectar (R5 — tolerância a falhas).
     */
    private static void monitorPeer(BattleshipFactoryImpl factory, String peerHost, int peerPort, String nodeId) {
        while (true) {
            try { Thread.sleep(5000); } catch (InterruptedException ignored) { return; }

            if (!factory.hasPeer()) {
                // Peer caiu; tentar reconectar
                System.out.println("[" + nodeId + "] Peer caiu. A tentar reconectar...");
                connectToPeer(factory, peerHost + ":" + peerPort, nodeId);
                return;
            }

            try {
                factory.getPeerRef().ping();
            } catch (Exception e) {
                System.out.println("[" + nodeId + "] Peer não responde. A marcar como inativo.");
                factory.setPeer(null);
            }
        }
    }
}
