package org.orgaprop.test7.security.diagnostic;

// Imports Java standard
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// Imports logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Imports internes
import org.orgaprop.test7.security.diagnostic.ResourceInfo;
import org.orgaprop.test7.security.diagnostic.ResourceCloseError;

/**
 * Registre des ressources système avec gestion automatique de leur fermeture.
 * Cette classe est thread-safe et gère la fermeture asynchrone des ressources.
 */
private static class ResourceRegistry {

	private static final Logger logger = LoggerFactory.getLogger(ResourceRegistry.class);

	/** Délai maximum en millisecondes pour la fermeture des ressources */
	private static final long RESOURCE_CLOSE_TIMEOUT_MS = 5000;

	/** Map des ressources actives avec leurs informations associées */
	private static final Map<AutoCloseable, ResourceInfo> resources = new ConcurrentHashMap<>();

	/**
	 * Ferme toutes les ressources enregistrées de manière asynchrone.
	 * La méthode attend que toutes les ressources soient fermées ou que le délai
	 * soit dépassé.
	 * Les erreurs de fermeture sont collectées et traitées ensemble à la fin.
	 *
	 * @throws SecurityException si le thread est interrompu pendant l'attente
	 */
	static void closeAll() {
		List<ResourceCloseError> errors = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(resources.size());

		// Fermeture asynchrone de chaque ressource
		for (ResourceInfo info : resources.values()) {
			CompletableFuture.runAsync(() -> {
				try {
					info.resource.close();
				} catch (Exception e) {
					errors.add(new ResourceCloseError(info, e));
				} finally {
					latch.countDown();
				}
			});
		}

		try {
			// Attente de la fermeture de toutes les ressources
			if (!latch.await(RESOURCE_CLOSE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
				logger.error("Timeout pendant la fermeture des ressources");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SecurityException("Interruption pendant la fermeture des ressources");
		}

		// Traitement des erreurs si nécessaire
		if (!errors.isEmpty()) {
			handleResourceCloseErrors(errors);
		}
	}

}
