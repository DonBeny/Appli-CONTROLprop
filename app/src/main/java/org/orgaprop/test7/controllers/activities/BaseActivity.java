package org.orgaprop.test7.controllers.activities;

import android.os.Bundle;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import org.orgaprop.test7.utils.UiUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Activité de base avec les fonctionnalités communes
 */
public abstract class BaseActivity extends AppCompatActivity {

	private Map<String, Object> dataStore = new HashMap<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initializeComponents();
		setupComponents();
	}

	/**
	 * Initialise les composants de l'activité.
	 */
	protected abstract void initializeComponents();

	/**
	 * Configure les composants de l'activité.
	 */
	protected abstract void setupComponents();

	/**
	 * Affiche un message d'erreur.
	 *
	 * @param messageId L'ID du message à afficher.
	 */
	protected void showError(@StringRes int messageId) {
		UiUtils.showToast(this, messageId);
	}

	/**
	 * Affiche ou masque un indicateur de chargement.
	 *
	 * @param show true pour afficher l'indicateur, false pour le masquer.
	 */
	protected void showWait(boolean show) {
		// À implémenter dans les classes filles si nécessaire
	}

	/**
	 * Stocke une donnée dans le dataStore.
	 *
	 * @param key   La clé de la donnée.
	 * @param value La valeur de la donnée.
	 */
	protected void putData(String key, Object value) {
		if (key != null && value != null) {
			dataStore.put(key, value);
		}
	}

	/**
	 * Récupère une donnée du dataStore.
	 *
	 * @param key La clé de la donnée.
	 * @return La valeur de la donnée, ou null si la clé n'existe pas.
	 */
	protected Object getData(String key) {
		return key != null ? dataStore.get(key) : null;
	}

	/**
	 * Supprime une donnée partagée.
	 *
	 * @param key La clé de la donnée.
	 */
	public void removeData(String key) {
		if (key != null) {
			dataStore.remove(key);
		}
	}
}
