package org.orgaprop.test7.security.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.orgaprop.test7.exceptions.BaseException;
import org.orgaprop.test7.exceptions.config.ConfigException;

import java.nio.charset.StandardCharsets;

/**
 * Gère l'authentification et la sauvegarde sécurisée des credentials.
 */
public class AuthManager {

   private static final String TAG = AuthManager.class.getSimpleName();

   private static final String PREFS_NAME = "user_prefs";
   private static final String KEY_USERNAME = "username";
   private static final String KEY_PASSWORD = "password"; // ⚠️ Stockage chiffré recommandé

   private final SharedPreferences sharedPreferences;

   public AuthManager(Context context) {
      this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
   }

   /**
    * Sauvegarde les credentials de l'utilisateur.
    *
    * @param username Nom d'utilisateur.
    * @param password Mot de passe en clair.
    */
   public void saveCredentials(String username, String password) throws BaseException {
      try {
         String encryptedPassword = encryptPassword(password);
         sharedPreferences.edit()
                 .putString(KEY_USERNAME, username)
                 .putString(KEY_PASSWORD, encryptedPassword)
                 .apply();
         Log.d(TAG, "Credentials sauvegardés avec succès");
      } catch (Exception e) {
         Log.e(TAG, "Erreur lors de la sauvegarde des credentials", e);
         throw new BaseException("Impossible de sauvegarder les credentials", ConfigException.AUTH_ERROR);
      }
   }

   /**
    * Récupère le nom d'utilisateur stocké.
    *
    * @return Nom d'utilisateur ou null si non enregistré.
    */
   public String getUsername() {
      return sharedPreferences.getString(KEY_USERNAME, null);
   }

   /**
    * Récupère le mot de passe décrypté.
    *
    * @return Mot de passe en clair ou null.
    */
   public String getDecryptedPassword() {
      String encryptedPassword = sharedPreferences.getString(KEY_PASSWORD, null);
      return encryptedPassword != null ? decryptPassword(encryptedPassword) : null;
   }

   /**
    * Vérifie si les credentials existent.
    *
    * @return true si un utilisateur est enregistré.
    */
   public boolean validateCredentials() {
      return getUsername() != null && getDecryptedPassword() != null;
   }

   /**
    * Supprime les credentials.
    */
   public void clearCredentials() {
      sharedPreferences.edit()
              .remove(KEY_USERNAME)
              .remove(KEY_PASSWORD)
              .apply();
      Log.d(TAG, "Credentials supprimés");
   }

   /**
    * Simule un chiffrement simple pour sécuriser le mot de passe.
    * ⚠️ Pour une vraie sécurité, utiliser Android Keystore ou AES.
    */
   private String encryptPassword(String password) {
      return Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
   }

   /**
    * Déchiffre le mot de passe.
    */
   private String decryptPassword(String encryptedPassword) {
      return new String(Base64.decode(encryptedPassword, Base64.DEFAULT), StandardCharsets.UTF_8);
   }

}

