package dev.saisrin.cage;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Converter
public class SecretConverter implements AttributeConverter<String, String> {
  private static byte[] key;

  @Value("${cage.encryption-key}")
  public void setKey(String value) {
    key = Base64.getDecoder().decode(value);
    if (key.length != 32)
      throw new IllegalArgumentException("DATA_ENCRYPTION_KEY must be base64 of 32 random bytes");
  }

  public String convertToDatabaseColumn(String value) {
    if (value == null) return null;
    try {
      byte[] iv = new byte[12];
      new SecureRandom().nextBytes(iv);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
      return Base64.getEncoder().encodeToString(iv)
          + ":"
          + Base64.getEncoder().encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Sensitive value encryption failed", e);
    }
  }

  public String convertToEntityAttribute(String value) {
    if (value == null) return null;
    try {
      String[] parts = value.split(":", 2);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(key, "AES"),
          new GCMParameterSpec(128, Base64.getDecoder().decode(parts[0])));
      return new String(c.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Sensitive value decryption failed", e);
    }
  }
}
