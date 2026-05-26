package practice1;

import javax.crypto.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Encrypter {
    private final SecretKey secretKey;

    public Encrypter(SecretKey secretKey) { this.secretKey = secretKey; }

    public byte[] encrypt(Packet packet)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Message message = packet.getMessage();
        byte[] messageBytes = message.getMessageContent().getBytes(StandardCharsets.UTF_8);

        ByteBuffer bufferToCipher = ByteBuffer.allocate(8 + messageBytes.length);
        bufferToCipher.putInt(message.getCType());
        bufferToCipher.putInt(message.getBUserId());
        bufferToCipher.put(messageBytes);

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedMessage = cipher.doFinal(bufferToCipher.array());

        int wLen = encryptedMessage.length;
        int bufferSize = 1 + 1 + 8 + 4 + 2 + wLen + 2;
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        buffer.put((byte) 0x13);
        buffer.put(packet.getBSrc());
        buffer.putLong(packet.getBPktId());
        buffer.putInt(wLen);

        byte[] header = new byte[14];
        buffer.get(0, header, 0, 14);
        buffer.putShort(Crc16.calculateCrc(header));

        buffer.put(encryptedMessage);

        byte[] payload = new byte[wLen];
        buffer.get(16, payload, 0, wLen);
        buffer.putShort(Crc16.calculateCrc(payload));

        return buffer.array();
    }
}