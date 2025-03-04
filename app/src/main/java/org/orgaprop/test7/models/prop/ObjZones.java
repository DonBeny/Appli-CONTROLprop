package org.orgaprop.test7.models.prop;

import org.json.JSONArray;

import java.io.Serializable;

public class ObjZones implements Serializable {

    /*  PRIVATE VARIABLES   */
    private JSONArray proxi;
    private JSONArray contrat;

    /*  CONSTRUCTORS    */
    public ObjZones() {
        proxi = new JSONArray();
        contrat = new JSONArray();
    }
    public ObjZones(JSONArray proxi, JSONArray contrat) {
        this.proxi = proxi;
        this.contrat = contrat;
    }

    /*  GETTERS */
    public JSONArray getProxi() { return proxi; }
    public JSONArray getContrat() { return contrat; }

    /*  SETTERS */
    public void setProxi(JSONArray proxi) { this.proxi = proxi; }
    public void setContrat(JSONArray contrat) { this.contrat = contrat; }

}
