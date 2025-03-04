package org.orgaprop.test7.models;

import org.json.JSONArray;
import org.orgaprop.test7.models.prop.ObjConfig;
import org.orgaprop.test7.models.prop.ObjDateCtrl;
import org.orgaprop.test7.models.prop.ObjGrille;
import org.orgaprop.test7.models.prop.ObjZones;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ObjProp implements Serializable {

    /*  PRIVATE VARIABLES   */
    private ObjConfig objConfig;
    private ObjZones objZones;
    private ObjDateCtrl objDateCtrl;
    private int note;
    private ObjGrille grille;

    /*  CONSTRUCTORS    */
    public ObjProp() {
        this.objConfig = new ObjConfig();
    }

    /*  GETTERS */
    public ObjConfig getObjConfig() { return objConfig; }
    public ObjZones getObjZones() { return objZones; }
    public int getNote() { return note; }
    public ObjDateCtrl getObjDateCtrl() { return objDateCtrl; }
    public ObjGrille getGrille() { return grille; }

    /*  SETTERS */
    public void setObjConfig(ObjConfig config) { this.objConfig = config; }
    public void setObjZones(ObjZones zones) { this.objZones = zones; }
    public void setNote(int note) { this.note = note; }
    public void setObjDateCtrl(ObjDateCtrl date) { this.objDateCtrl = date; }
    public void setGrille(ObjGrille grille) { this.grille = grille; }

}
