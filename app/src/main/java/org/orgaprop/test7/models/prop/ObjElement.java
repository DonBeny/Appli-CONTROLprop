package org.orgaprop.test7.models.prop;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ObjElement implements Serializable {

    /*  PRIVATE VARIABLES   */
    int id;
    int note;
    private Map<Integer, ObjCriter> criterMap;

    /*  CONSTRUCTORS    */
    public ObjElement() {
        this.id = 0;
        this.note = -1;
        this.criterMap = new HashMap<>();
    }
    public ObjElement(int id) {
        this.id = id;
        this.note = -1;
        this.criterMap = new HashMap<>();
    }
    public ObjElement(int id, int note) {
        this.id = id;
        this.note = note;
        this.criterMap = new HashMap<>();
    }

    /*  GETTERS */
    public int getId() { return id; }
    public int getNote() { return note; }
    public Map<Integer, ObjCriter> getCriterMap() { return criterMap; }

    /*  SETTERS */
    public void setId(int id) { this.id = id; }
    public void setNote(int note) { this.note = note; }
    public void setCriterMap(Map<Integer, ObjCriter> criterMap) { this.criterMap = criterMap; }
    public void addCriter(ObjCriter criter) { this.criterMap.put(this.criterMap.size(), criter); }

}
