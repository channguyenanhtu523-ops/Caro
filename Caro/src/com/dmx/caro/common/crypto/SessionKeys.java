package com.dmx.caro.common.crypto;

import javax.crypto.SecretKey;

public record SessionKeys(SecretKey aesKey, SecretKey hmacKey) {
}
