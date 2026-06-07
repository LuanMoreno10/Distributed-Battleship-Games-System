package edu.ufp.inf.sd.battleshipgame.pubsub;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import edu.ufp.inf.sd.battleshipgame.model.GameState;

import java.io.Closeable;

/**
 * Publisher (server-side) para propagação assíncrona/persistente via RabbitMQ.
 * Publica snapshots do GameState num exchange do tipo fanout.
 */
public class BattleshipGamePublisher implements Closeable {

    private final String exchange;
    private final Connection connection;
    private final Channel channel;

    public BattleshipGamePublisher(String gameId) throws Exception {
        this.exchange = RabbitMqConfig.EXCHANGE_PREFIX + gameId;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMqConfig.HOST);

        this.connection = factory.newConnection("battleship-publisher-" + gameId);
        this.channel = connection.createChannel();

        // Exchange durável => sobrevive a restart do broker
        channel.exchangeDeclare(exchange, BuiltinExchangeType.FANOUT, true);
    }

    public void publish(GameState state) throws Exception {
        byte[] payload = SerializationUtils.toBytes(state);
        channel.basicPublish(exchange, "", MessageProperties.PERSISTENT_BASIC, payload);
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

