package br.com.fiap.agroconnect_api.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

	private static final int ITERATIONS = 120000;
	private static final int SALT_LENGTH = 16;
	private static final int HASH_LENGTH = 32;
	private final SecureRandom random = new SecureRandom();

	public String hash(String password) {
		byte[] salt = new byte[SALT_LENGTH];
		random.nextBytes(salt);
		byte[] derived = derive(password, salt);
		return "PBKDF2$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(derived);
	}

	public boolean verify(String password, String stored) {
		if (password == null || stored == null) return false;
		try {
			String[] parts = stored.split("\\$", -1);
			if (parts.length != 3 || !"PBKDF2".equals(parts[0])) return false;
			byte[] salt = Base64.getDecoder().decode(parts[1]);
			byte[] expected = Base64.getDecoder().decode(parts[2]);
			if (salt.length != SALT_LENGTH || expected.length != HASH_LENGTH) return false;
			byte[] actual = derive(password, salt);
			boolean matches = MessageDigest.isEqual(actual, expected);
			Arrays.fill(actual, (byte) 0);
			return matches;
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private byte[] derive(String password, byte[] salt) {
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_LENGTH * 8);
		try {
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Não foi possível processar a senha.", exception);
		} finally {
			spec.clearPassword();
		}
	}
}
