package com.dmx.caro.common.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class PacketIO {
    private static final int MAX_PACKET_BYTES = 1_000_000;

    private final DataInputStream input;
    private final DataOutputStream output;

    public PacketIO(Socket socket) throws IOException {
        this.input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public synchronized void writePacket(String packet) throws IOException {
        byte[] bytes = packet.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_PACKET_BYTES) {
            throw new IOException("Packet exceeds maximum size.");
        }
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

    public String readPacket() throws IOException {
        int length = input.readInt();
        if (length <= 0 || length > MAX_PACKET_BYTES) {
            throw new IOException("Invalid packet length: " + length);
        }

        byte[] bytes = new byte[length];
        input.readFully(bytes);
        if (bytes.length != length) {
            throw new EOFException("Unexpected end of stream while reading packet.");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
