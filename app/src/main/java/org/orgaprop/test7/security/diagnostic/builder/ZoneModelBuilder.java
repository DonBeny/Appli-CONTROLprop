package org.orgaprop.test7.security.diagnostic.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;

/**
 * Builder pour construire les modèles de zone de manière fluide.
 */
public class ZoneModelBuilder {
	private static final Logger logger = LoggerFactory.getLogger(ZoneModelBuilder.class);

	private final CellZoneCtrlModel model;

	public ZoneModelBuilder(int position) {
		this.model = new CellZoneCtrlModel(position);
	}

	public ZoneModelBuilder withId(String id) {
		model.setId(id);
		return this;
	}

	public ZoneModelBuilder withCoefficient(int coef) {
		model.setCoef(coef);
		return this;
	}

	public ZoneModelBuilder withText(String text) {
		model.setText(text);
		return this;
	}

	public CellZoneCtrlModel build() {
		validateModel();
		return model;
	}
}
