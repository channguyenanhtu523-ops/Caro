package com.dmx.caro.common.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NetworkMessage {
    private final MessageType type;
    private final Map<String, String> fields;

    private NetworkMessage(MessageType type, Map<String, String> fields) {
        this.type = type;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public static Builder type(MessageType type) {
        return new Builder(type);
    }

    public MessageType type() {
        return type;
    }

    public Map<String, String> fields() {
        return fields;
    }

    public String required(String name) {
        String value = fields.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + name);
        }
        return value;
    }

    public String optional(String name, String defaultValue) {
        String value = fields.get(name);
        return value == null ? defaultValue : value;
    }

    public int requiredInt(String name) {
        return Integer.parseInt(required(name));
    }

    public int optionalInt(String name, int defaultValue) {
        String value = fields.get(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    public long optionalLong(String name, long defaultValue) {
        String value = fields.get(name);
        return value == null || value.isBlank() ? defaultValue : Long.parseLong(value);
    }

    public boolean requiredBoolean(String name) {
        return Boolean.parseBoolean(required(name));
    }

    public static final class Builder {
        private final MessageType type;
        private final Map<String, String> fields = new LinkedHashMap<>();

        private Builder(MessageType type) {
            this.type = type;
        }

        public Builder field(String name, Object value) {
            if (value != null) {
                fields.put(name, String.valueOf(value));
            }
            return this;
        }

        public NetworkMessage build() {
            return new NetworkMessage(type, fields);
        }
    }
}
