package org.example;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class DecrypterTest {
    private Decrypter SUT;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = "PavloVusPassword".getBytes(StandardCharsets.UTF_8);
        secretKey = new SecretKeySpec(keyBytes, "AES");
        SUT = new Decrypter(secretKey);
    }

    @Test
    public void shouldDecryptValidPacket() throws Exception {
        Packet packet = SUT.decrypt(Hex.decodeHex("13010000000000000001000000105e2c5c7a6cbfc7faec415422d70757c3e46c6c23"));

        assertThat(packet)
                .returns((byte) 0x1, Packet::getBSrc)
                .returns(1L, Packet::getBPktId);
        assertThat(packet.getMessage())
                .isNotNull()
                .returns(1, Message::getCType)
                .returns(1, Message::getBUserId)
                .returns("Pavlo", Message::getMessageContent);
    }

    @Test
    void shouldThrowExceptionWhenMagicByteIsIncorrect() throws Exception {
        //Змінив 13 на 14 на початку
        assertThatThrownBy(() -> SUT.decrypt(Hex.decodeHex("14010000000000000001000000105e2c5c7a6cbfc7faec415422d70757c3e46c6c23")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("magic number");
    }

    @Test
    void shouldThrowExceptionWhenHeaderCrcIsIncorrect() throws Exception {
        // Змінив другий байт з 01 на 02, що мало б зламати crc хедеру
        assertThatThrownBy(() -> SUT.decrypt(Hex.decodeHex("13020000000000000001000000105e2c5c7a6cbfc7faec415422d70757c3e46c6c23")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("First crc is incorrect");
    }

    @Test
    void shouldThrowExceptionWhenPayloadCrcIsIncorrect() throws Exception {
        // змінив 5c на початку корисного навантаження на 6c
        assertThatThrownBy(() -> SUT.decrypt(Hex.decodeHex("13010000000000000001000000105e2c6c7a6cbfc7faec415422d70757c3e46c6c23")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Second crc is incorrect");
    }

    @Test
    void shouldThrowExceptionWhenDecryptedWithWrongKey() throws Exception {
        // Створюю інший екземпляр Decrypter з неправильним ключем
        SecretKey wrongKey = new SecretKeySpec("6767676767676767".getBytes(), "AES");
        Decrypter wrongDecrypter = new Decrypter(wrongKey);

        assertThatThrownBy(() -> wrongDecrypter.decrypt(Hex.decodeHex("13010000000000000001000000105e2c5c7a6cbfc7faec415422d70757c3e46c6c23")))
                .isInstanceOf(BadPaddingException.class);
    }
}