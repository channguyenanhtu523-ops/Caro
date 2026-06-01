package com.dmx.caro.common.net;

import com.dmx.caro.common.crypto.CryptoEngine;
import com.dmx.caro.common.crypto.SessionKeys;
import com.dmx.caro.common.protocol.NetworkMessage;
import com.dmx.caro.common.protocol.ProtocolCodec;
import com.dmx.caro.common.protocol.ProtocolException;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;

public final class SecureChannel {
    private final PacketIO packetIO;
    private volatile SessionKeys sessionKeys;

    public SecureChannel(Socket socket) throws IOException {
        this.packetIO = new PacketIO(socket);
    }

    public void setSessionKeys(SessionKeys sessionKeys) {
        this.sessionKeys = sessionKeys;
    }

    public SessionKeys sessionKeys() {
        return sessionKeys;
    }

    public void sendPlain(NetworkMessage message) throws IOException {
        packetIO.writePacket(ProtocolCodec.encode(message));
    }

    public void sendSecure(NetworkMessage message) throws IOException, GeneralSecurityException {
        SessionKeys keys = requireSessionKeys();
        packetIO.writePacket(CryptoEngine.encryptMessage(message, keys));
    }

    public NetworkMessage readMessage() throws IOException, ProtocolException, GeneralSecurityException {
        String packet = packetIO.readPacket();
        if (CryptoEngine.looksLikeSecureEnvelope(packet)) {
            SessionKeys keys = requireSessionKeys();
            return CryptoEngine.decryptMessage(packet, keys);
        }
        return ProtocolCodec.decode(packet);
    }

    private SessionKeys requireSessionKeys() {
        SessionKeys keys = sessionKeys;
        if (keys == null) {
            throw new IllegalStateException("Secure channel is not initialized yet.");
        }
        return keys;
    }
}
