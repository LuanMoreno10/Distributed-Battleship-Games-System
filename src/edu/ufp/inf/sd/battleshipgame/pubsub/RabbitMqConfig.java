package edu.ufp.inf.sd.battleshipgame.pubsub;

public final class RabbitMqConfig {
    private RabbitMqConfig() {
    }

    public static final String HOST = "localhost";

    /** Prefixo dos exchanges por jogo. Exchange real: PREFIX + gameId */
    public static final String EXCHANGE_PREFIX = "battleship.";

    /** Prefixo de filas (uma por jogador). Queue real: PREFIX + gameId + "." + username */
    public static final String QUEUE_PREFIX = "battleship.";
}

