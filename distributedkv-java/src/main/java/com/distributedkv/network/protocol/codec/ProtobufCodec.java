package com.distributedkv.network.protocol.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.distributedkv.network.protocol.Message;

/**
 * Temporary codec used until MessageProto definitions are restored.
 *
 * <p>Uses Java serialization so Netty client/server scaffolding can compile and run
 * local integration paths. Not intended as the production wire format.
 */
public class ProtobufCodec implements MessageCodec {

    @Override
    public byte[] encode(Message message) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(message);
        }
        return bos.toByteArray();
    }

    @Override
    public Message decode(byte[] bytes) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object obj = ois.readObject();
            if (!(obj instanceof Message)) {
                throw new IllegalArgumentException("Decoded object is not a Message: " +
                        (obj == null ? "null" : obj.getClass().getName()));
            }
            return (Message) obj;
        }
    }

    @Override
    public String getContentType() {
        return "application/x-java-serialized-object";
    }
}
