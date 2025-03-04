package org.orgaprop.test7.security.crypto;

import androidx.annotation.NonNull;
import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.Mac;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Fournisseur de services cryptographiques.
 * Gère le chiffrement et déchiffrement des données sensibles.
 */
public class CryptoProvider {
	private static final int KEY_SIZE = 256;
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 16;

	/** Algorithme de chiffrement utilisé */
	private static final String ALGORITHM = "AES";

	/** Mode de chiffrement et padding */
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";

	/** Clé secrète pour le chiffrement */
	private SecretKey secretKey;

	/** Sécurité minimale requise */
	private static final int MIN_KEY_SIZE = 256;
	private static final int MIN_ITERATIONS = 10000;

	/** Constantes pour dérivation de clé */
	private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int SALT_LENGTH = 32;

	/** Clé pour le contrôle d'intégrité */
	private static final String HMAC_ALGORITHM = "HmacSHA256";

	/**
	 * Initialise le fournisseur avec une nouvelle clé AES.
	 * 
	 * @throws Exception si la génération de la clé échoue
	 */
	public CryptoProvider() throws Exception {
		KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
		SecureRandom secureRandom = new SecureRandom();
		keyGen.init(KEY_SIZE, secureRandom);
		secretKey = keyGen.generateKey();
	}

	/**
	 * Chiffre une chaîne de caractères.
	 * Utilise AES/GCM avec IV unique.
	 * 
	 * @param data Données à chiffrer (non null)
	 * @return Chaîne Base64 contenant IV + données chiffrées
	 * @throws Exception                si le chiffrement échoue
	 * @throws IllegalArgumentException si data est null
	 */
	public String encrypt(@NonNull String data) throws Exception {
		if (data == null)
			throw new IllegalArgumentException("Data cannot be null");

		Cipher cipher = Cipher.getInstance(TRANSFORMATION);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		byte[] iv = cipher.getIV();
		byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

		// Combine IV and encrypted data
		byte[] combined = new byte[iv.length + encryptedBytes.length];
		System.arraycopy(iv, 0, combined, 0, iv.length);
		System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

		return Base64.encodeToString(combined, Base64.DEFAULT);
	}

	/**
	 * Déchiffre une chaîne précédemment chiffrée.
	 * Extrait l'IV et déchiffre les données.
	 * 
	 * @param encryptedData Données chiffrées en Base64 (non null)
	 * @return Données déchiffrées
	 * @throws Exception                si le déchiffrement échoue
	 * @throws IllegalArgumentException si encryptedData est null
	 */
	public String decrypt(@NonNull String encryptedData) throws Exception {
		if (encryptedData == null)
			throw new IllegalArgumentException("Encrypted data cannot be null");

		byte[] decoded = Base64.decode(encryptedData, Base64.DEFAULT);

		// Extract IV
		byte[] iv = new byte[GCM_IV_LENGTH]; // GCM IV length
		System.arraycopy(decoded, 0, iv, 0, iv.length);

		// Extract encrypted data
		byte[] encrypted = new byte[decoded.length - iv.length];
		System.arraycopy(decoded, iv.length, encrypted, 0, encrypted.length);

		Cipher cipher = Cipher.getInstance(TRANSFORMATION);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, cipher.getParameters());
		byte[] decryptedBytes = cipher.doFinal(encrypted);

		return new String(decryptedBytes, StandardCharsets.UTF_8);
	}

	/** Gestionnaire de clé */
	private void clearKey() {
		if (secretKey != null) {
			// Effacement sécurisé de la clé
			Arrays.fill(secretKey.getEncoded(), (byte) 0);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			clearKey();
		} finally {
			super.finalize();
		}
	}

	/**
	 * Réinitialise la clé de chiffrement.
	 * Génère une nouvelle clé AES-256.
	 */
	public void reset() {
		try {
			clearKey();
			KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
			SecureRandom secureRandom = new SecureRandom();
			keyGen.init(KEY_SIZE, secureRandom);
			secretKey = keyGen.generateKey();
		} catch (Exception e) {
			throw new SecurityException("Erreur lors de la réinitialisation de la clé", e);
		}
	}

	/**
	 * Vérifie si l'algorithme est supporté avec le niveau de sécurité requis.
	 * 
	 * @throws SecurityException si la sécurité est insuffisante
	 */
	private void checkSecurityLevel() throws NoSuchAlgorithmException {
		if (Cipher.getMaxAllowedKeyLength(ALGORITHM) < MIN_KEY_SIZE) {
			throw new SecurityException("Niveau de sécurité cryptographique insuffisant");
		}
	}

	/**
	 * Vérifie l'intégrité des données chiffrées.
	 * 
	 * @param data Données à vérifier
	 * @param hmac HMAC attendu
	 * @return true si l'intégrité est validée
	 */
	public boolean verifyIntegrity(@NonNull byte[] data, @NonNull byte[] hmac) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(secretKey);
			byte[] calculatedHmac = mac.doFinal(data);
			return Arrays.equals(calculatedHmac, hmac);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Génère un sel aléatoire pour la dérivation de clé.
	 * 
	 * @return Sel cryptographique
	 */
	private byte[] generateSalt() {
		byte[] salt = new byte[SALT_LENGTH];
		new SecureRandom().nextBytes(salt);
		return salt;
	}

	/**
	 * Ajoute une protection contre les attaques temporelles.
	 */
	private boolean constantTimeEquals(byte[] a, byte[] b) {
		if (a.length != b.length) {
			return false;
		}
		int result = 0;
		for (int i = 0; i < a.length; i++) {
			result |= a[i] ^ b[i];
		}
		return result == 0;
	}
}
