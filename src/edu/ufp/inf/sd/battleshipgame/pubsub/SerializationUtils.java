package edu.ufp.inf.sd.battleshipgame.pubsub;

import java.io.*;

final class SerializationUtils {
    private SerializationUtils() {
    }

    static byte[] toBytes(Serializable obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            out.flush();
            return bos.toByteArray();
        }
    }

    static <T> T fromBytes(byte[] bytes, Class<T> type) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            Object o = in.readObject();
            return type.cast(o);
        }
    }
}

