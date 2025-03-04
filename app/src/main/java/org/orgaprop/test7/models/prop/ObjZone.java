package org.orgaprop.test7.models.prop;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ObjZone implements Serializable {

    /*  PRIVATE VARIABLES   */
    int id;
    int note;
    Map<Integer, ObjElement> elementMap;

    /*  CONSTRUCTORS    */
    public ObjZone() {
        this.id = 0;
        this.note = -1;
        this.elementMap = new HashMap<>();
    }
    public ObjZone(int id) {
        this.id = id;
        this.note = -1;
        this.elementMap = new HashMap<>();
    }
    public ObjZone(int id, int note) {
        this.id = id;
        this.note = note;
        this.elementMap = new HashMap<>();
    }

    /*  GETTERS */
    public int getId() { return id; }
    public int getNote() { return note; }
    public Map<Integer, ObjElement> getElementMap() { return elementMap; }

    /*  SETTERS */
    public void setId(int id) { this.id = id; }
    public void setNote(int note) { this.note = note; }
    public void setElementMap(Map<Integer, ObjElement> elementMap) { this.elementMap = elementMap; }
    public void addElement(ObjElement element) { this.elementMap.put(this.elementMap.size(), element); }

}
