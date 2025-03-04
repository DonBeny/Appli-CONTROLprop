package org.orgaprop.test7.security.diagnostic;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gère l'acquisition et la libération automatique des verrous de ressources.
 * Implémente le pattern "try-with-resources" pour une gestion sûre des verrous.
 * 
 * Exemple d'utilisation:
 * 
 * <pre>
 * try (ResourceHandle handle = new ResourceHandle(lock)) {
 * 	// Code protégé par le verrou
 * } // Le verrou est automatiquement libéré
 * </pre>
 */
public final class ResourceHandle implements AutoCloseable {

	/** Le verrou géré par cette instance */
	private final Lock lock;

	/**
	 * Crée une nouvelle instance et acquiert immédiatement le verrou.
	 *
	 * @param lock Le verrou à gérer
	 * @throws IllegalArgumentException si le verrou est null
	 */
	public ResourceHandle(Lock lock) {
		if (lock == null) {
			throw new IllegalArgumentException("Le verrou ne peut pas être null");
		}
		this.lock = lock;
		lock.lock();
	}

	/**
	 * Libère le verrou lors de la fermeture de la ressource.
	 * Cette méthode est automatiquement appelée à la fin du bloc
	 * try-with-resources.
	 */
	@Override
	public void close() {
		lock.unlock();
	}
}
