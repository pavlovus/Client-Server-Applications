package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolTest {

    private Encrypter encrypter;
    private Decrypter decrypter;

    @BeforeEach
    void setUp() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey sessionKey = keyGenerator.generateKey();

        encrypter = new Encrypter(sessionKey);
        decrypter = new Decrypter(sessionKey);
    }

    @Test
    void shouldCompleteFullRoundTripSuccessfully() throws Exception {
        Packet originalPacket = new Packet((byte) 0x3, 123L,
                new Message(123, 1234, "Це супер секретне повідомлення, не читай пж"));

        byte[] encryptedData = encrypter.encrypt(originalPacket);
        Packet decryptedPacket = decrypter.decrypt(encryptedData);

        assertThat(decryptedPacket).isNotNull();
        assertThat(decryptedPacket.getBSrc()).isEqualTo(originalPacket.getBSrc());
        assertThat(decryptedPacket.getBPktId()).isEqualTo(originalPacket.getBPktId());

        Message decryptedMessage = decryptedPacket.getMessage();
        assertThat(decryptedMessage).isNotNull();
        assertThat(decryptedMessage.getCType()).isEqualTo(originalPacket.getMessage().getCType());
        assertThat(decryptedMessage.getBUserId()).isEqualTo(originalPacket.getMessage().getBUserId());
        assertThat(decryptedMessage.getMessageContent()).isEqualTo(originalPacket.getMessage().getMessageContent());
    }

    @Test
    void shouldHandleLargePayloadsCorrectly() throws Exception {
        StringBuilder largeTextBuilder = new StringBuilder();
        for (int i = 0; i < 10000; i++)  largeTextBuilder.append("Item_").append(i).append("; ");

        Packet originalPacket = new Packet((byte) 0x3, 4L, new Message(1, 2, largeTextBuilder.toString()));

        byte[] encryptedData = encrypter.encrypt(originalPacket);
        Packet decryptedPacket = decrypter.decrypt(encryptedData);

        assertThat(decryptedPacket.getMessage().getMessageContent())
                .isEqualTo(originalPacket.getMessage().getMessageContent());
    }

    @Test
    void shouldHandleEmptyMessages() throws Exception {
        Packet originalPacket = new Packet((byte) 1, 12L, new Message(123, 1234, ""));

        byte[] encryptedData = encrypter.encrypt(originalPacket);
        Packet decryptedPacket = decrypter.decrypt(encryptedData);

        assertThat(decryptedPacket.getMessage().getMessageContent()).isEmpty();
    }
}