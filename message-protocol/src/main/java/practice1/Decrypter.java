package practice1;

import javax.crypto.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Decrypter {
    private final SecretKey secretKey;

    public Decrypter(SecretKey secretKey) { this.secretKey = secretKey; }

    public Packet decrypt(byte[] encryptedPacket)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        ByteBuffer buffer = ByteBuffer.wrap(encryptedPacket);

        byte bMagic = buffer.get();
        if (bMagic != 0x13) throw new IllegalArgumentException("Message without magic number at the beginning");

        byte bSrc = buffer.get();
        long bPktId = buffer.getLong();
        int wLen = buffer.getInt();
        short headerCrc = buffer.getShort();

        if(headerCrc != Crc16.calculateCrc(Arrays.copyOfRange(encryptedPacket, 0, 14)))
            throw new IllegalArgumentException("First crc is incorrect");

        byte[] encryptedMessage = new byte[wLen];
        buffer.get(encryptedMessage);

        short payloadCrc = buffer.getShort();
        if(payloadCrc != Crc16.calculateCrc(encryptedMessage))
            throw new IllegalArgumentException("Second crc is incorrect");

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedMessage = cipher.doFinal(encryptedMessage);

        ByteBuffer decryptedMessageBuffer = ByteBuffer.wrap(decryptedMessage);

        int cType = decryptedMessageBuffer.getInt();
        int bUserId = decryptedMessageBuffer.getInt();
        byte[] decryptedMessageContent = new byte[decryptedMessageBuffer.remaining()];
        decryptedMessageBuffer.get(decryptedMessageContent);

        return new Packet(bSrc, bPktId, new Message(cType, bUserId, new String(decryptedMessageContent, StandardCharsets.UTF_8)));
    }
}