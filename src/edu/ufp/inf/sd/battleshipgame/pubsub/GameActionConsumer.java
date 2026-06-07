package edu.ufp.inf.sd.battleshipgame.pubsub;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import edu.ufp.inf.sd.battleshipgame.model.GameCommand;

import java.io.Closeable;
import java.util.function.Consumer;

public class GameActionConsumer implements Closeable {

    private final String queue;
    private final Connection connection;
    private final Channel channel;

    public GameActionConsumer(String gameId, Consumer<GameCommand> handler) throws Exception {
        this.queue = RabbitMqConfig.COMMANDS_QUEUE_PREFIX + gameId;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMqConfig.HOST);

        this.connection = factory.newConnection("battleship-action-consumer-" + gameId);
        this.channel = connection.createChannel();
        channel.queueDeclare(queue, true, false, false, null);
        channel.basicQos(1);

        channel.basicConsume(queue, false, (tag, delivery) -> {
            try {
                GameCommand cmd = SerializationUtils.fromBytes(delivery.getBody(), GameCommand.class);
                handler.accept(cmd);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                System.err.println("[RabbitMQ] Erro a processar comando: " + e.getMessage());
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        }, tag -> {});
    }

    @Override
    public void close() {
        try { channel.close(); } catch (Exception ignored) {}
        try { connection.close(); } catch (Exception ignored) {}
    }
}
