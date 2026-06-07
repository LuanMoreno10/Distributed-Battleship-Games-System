package edu.ufp.inf.sd.battleshipgame.pubsub;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import edu.ufp.inf.sd.battleshipgame.model.GameCommand;

import java.io.Closeable;

public class GameActionPublisher implements Closeable {

    private final String queue;
    private final Connection connection;
    private final Channel channel;

    public GameActionPublisher(String gameId) throws Exception {
        this.queue = RabbitMqConfig.COMMANDS_QUEUE_PREFIX + gameId;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMqConfig.HOST);

        this.connection = factory.newConnection("battleship-action-publisher-" + gameId);
        this.channel = connection.createChannel();
        channel.queueDeclare(queue, true, false, false, null);
    }

    public void publish(GameCommand command) throws Exception {
        channel.basicPublish("", queue, MessageProperties.PERSISTENT_BASIC, SerializationUtils.toBytes(command));
    }

    @Override
    public void close() {
        try { channel.close(); } catch (Exception ignored) {}
        try { connection.close(); } catch (Exception ignored) {}
    }
}
