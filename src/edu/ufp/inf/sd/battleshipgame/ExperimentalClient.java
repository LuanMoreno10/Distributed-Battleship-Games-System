package edu.ufp.inf.sd.battleshipgame;

import java.io.IOException;

/**
 * Stub da implementação original por sockets.
 * Substituída pela arquitetura RMI (ClientApp).
 */
public class ExperimentalClient {

    public ExperimentalClient(int port) throws IOException {
        throw new UnsupportedOperationException(
                "Comunicação por sockets desativada. Use ClientApp (RMI).");
    }

    public String clientIn() throws IOException {
        throw new UnsupportedOperationException();
    }

    public void clientOut(String msg) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void endClient() throws IOException {
        throw new UnsupportedOperationException();
    }
}
