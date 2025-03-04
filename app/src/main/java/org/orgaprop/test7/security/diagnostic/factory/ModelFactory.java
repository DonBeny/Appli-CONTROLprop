package org.orgaprop.test7.security.diagnostic.factory;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabrique pour créer les modèles de diagnostic.
 * Implémente le pattern Factory pour la création des différents modèles.
 * Cette classe est thread-safe et gère la création des modèles à partir de
 * structures JSON.
 */
public class ModelFactory {

	/** Logger pour cette classe */
	private static final Logger logger = LoggerFactory.getLogger(ModelFactory.class);

	/**
	 * Crée un modèle de zone de contrôle à partir d'une structure JSON.
	 * Le modèle est initialisé avec les propriétés suivantes :
	 * <ul>
	 * <li>id : identifiant unique de la zone</li>
	 * <li>coef : coefficient de la zone</li>
	 * <li>txt : texte descriptif de la zone</li>
	 * </ul>
	 *
	 * @param structure Structure JSON contenant les données du modèle
	 * @param position  Position de la zone dans la séquence
	 * @return Nouveau modèle de zone initialisé
	 * @throws ModelCreationException si une erreur survient pendant la création
	 * @throws NullPointerException   si structure est null
	 */
	public CellZoneCtrlModel createZoneModel(JSONObject structure, int position) {
		CellZoneCtrlModel model = new CellZoneCtrlModel(position);
		try {
			model.setId(structure.getString("id"));
			model.setCoef(Integer.parseInt(structure.getString("coef")));
			model.setText(structure.getString("txt"));
		} catch (Exception e) {
			logger.error("Erreur création modèle zone", e);
			throw new ModelCreationException("Erreur création zone", e);
		}
		return model;
	}

}
