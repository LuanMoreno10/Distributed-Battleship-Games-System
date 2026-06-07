package edu.ufp.inf.sd.battleshipgame.pubsub;

import com.rabbitmq.client.*;
import edu.ufp.inf.sd.battleshipgame.model.GameState;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Consumer (client-side) para receber atualizações via RabbitMQ.
 * Cada jogador usa uma fila durável própria e faz bind ao exchange do jogo (fanout).
 */
public class BattleshipGameConsumer implements Closeable {

    private final String exchange;
    private final String queue;

    private final Connection connection;
    private final Channel channel;

    public BattleshipGameConsumer(String gameId, String username) throws Exception {
        Objects.requireNonNull(gameId, "gameId");
        Objects.requireNonNull(username, "username");

        this.exchange = RabbitMqConfig.EXCHANGE_PREFIX + gameId;
        this.queue = RabbitMqConfig.QUEUE_PREFIX + gameId + "." + username;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMqConfig.HOST);

        this.connection = factory.newConnection("battleship-consumer-" + gameId + "-" + username);
        this.channel = connection.createChannel();

        // O exchange tem de existir. Se o broker ainda não o tiver, declaramos (idempotente).
        channel.exchangeDeclare(exchange, BuiltinExchangeType.FANOUT, true);

        // Fila durável (persistente) por jogador
        channel.queueDeclare(queue, true, false, false, null);
        channel.queueBind(queue, exchange, "");

        // Para garantir entrega 1-a-1 e evitar rebalanceamentos inesperados
        channel.basicQos(1);
    }

    public String start(Consumer<GameState> onState) throws Exception {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                GameState state = SerializationUtils.fromBytes(delivery.getBody(), GameState.class);
                onState.accept(state);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                // Se falhar a desserialização, rejeitamos e descartamos para não bloquear
                System.err.println("[RabbitMQ] Erro a processar mensagem: " + e.getMessage() +
                        " (raw=" + new String(delivery.getBody(), StandardCharsets.UTF_8) + ")");
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        };

        CancelCallback cancelCallback = consumerTag -> System.out.println("[RabbitMQ] Consumo cancelado: " + consumerTag);

        return channel.basicConsume(queue, false, deliverCallback, cancelCallback);
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (Exception ignored) {
        }
        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }
}


