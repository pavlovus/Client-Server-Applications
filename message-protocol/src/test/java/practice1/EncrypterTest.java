package practice1;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncrypterTest {
    private Encrypter SUT;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = "PavloVusPassword".getBytes(StandardCharsets.UTF_8);
        secretKey = new SecretKeySpec(keyBytes, "AES");
        SUT = new Encrypter(secretKey);
    }

    @Test
    void shouldEncryptPacket() throws Exception {
        Packet packet = new Packet((byte) 0x1, 1L, new Message(1, 1, "Pavlo"));

        byte[] result = SUT.encrypt(packet);

        assertThat(result).isNotNull();

        ByteBuffer buffer = ByteBuffer.wrap(result);

        assertThat(buffer.get()).isEqualTo((byte) 0x13);
        assertThat(buffer.get()).isEqualTo((byte) 0x1);
        assertThat(buffer.getLong()).isEqualTo(1L);

        int wLen = buffer.getInt();
        assertThat(wLen).isGreaterThan(0);

        assertThat(result.length).isEqualTo(14 + 2 + wLen + 2);
        assertThat(Hex.encodeHexString(result)).isEqualTo("13010000000000000001000000105e2c5c7a6cbfc7faec415422d70757c3e46c6c23");
    }

    @Test
    void shouldEncryptPayloadAndItCanBeDecryptedBeCipher() throws Exception {
        int expectedType = 2;
        int expectedUserId = 2;
        String expectedText = "Secret Data";
        Message msg = new Message(expectedType, expectedUserId, expectedText);
        Packet packet = new Packet((byte) 0x2, 2L, msg);

        byte[] result = SUT.encrypt(packet);

        ByteBuffer buffer = ByteBuffer.wrap(result);
        buffer.position(14);
        buffer.getShort();

        int wLen = ByteBuffer.wrap(result).getInt(10);

        byte[] encryptedPayload = new byte[wLen];
        buffer.get(encryptedPayload);

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedPayload);

        ByteBuffer decryptedBuffer = ByteBuffer.wrap(decryptedBytes);
        assertThat(decryptedBuffer.getInt()).isEqualTo(expectedType);
        assertThat(decryptedBuffer.getInt()).isEqualTo(expectedUserId);

        byte[] textBytes = new byte[decryptedBuffer.remaining()];
        decryptedBuffer.get(textBytes);
        assertThat(new String(textBytes, StandardCharsets.UTF_8)).isEqualTo(expectedText);
    }

    @Test
    void shouldHandleEmptyMessageString() throws Exception {
        Packet packet = new Packet((byte) 0x3, 3L, new Message(3, 3, ""));

        byte[] result = SUT.encrypt(packet);

        assertThat(Hex.encodeHexString(result)).isEqualTo("1303000000000000000300000010dc52a194efa06ecbff69482818f1efed22dd59ab");
        int wLen = ByteBuffer.wrap(result).getInt(10);
        assertThat(wLen).isGreaterThanOrEqualTo(16);
    }

    @Test
    void shouldHandleCyrillic() throws Exception {
        Packet packet = new Packet((byte) 0x4, 4L, new Message(4, 4, "Павло"));

        byte[] result = SUT.encrypt(packet);

        assertThat(Hex.encodeHexString(result)).isEqualTo("13040000000000000004000000204fecc8c57542867226d2b68172a01097d3ec2d6e2404dc9ea9c32c70c6b15233d62814fe");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenMessageContentIsNull() {
        Packet packet = new Packet((byte) 0x5, 5L, new Message(5, 5, null));

        assertThatThrownBy(() -> SUT.encrypt(packet)).isInstanceOf(NullPointerException.class);
    }
}