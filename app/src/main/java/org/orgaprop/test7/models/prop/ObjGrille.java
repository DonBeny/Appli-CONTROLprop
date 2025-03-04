package org.orgaprop.test7.models.prop;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ObjGrille implements Serializable {

    /*  PRIVATE VARIABLES   */
    Map<Integer, ObjZone> zoneMap;

    /*  CONSTRUCTORS    */
    public ObjGrille() {
        this.zoneMap = new HashMap<>();
    }

    /*  GETTERS */
    public Map<Integer, ObjZone> getZoneMap() { return zoneMap; }

    /*  SETTERS */
    public void setZoneMap(Map<Integer, ObjZone> zoneMap) { this.zoneMap = zoneMap; }
    public void addZone(ObjZone zone) { this.zoneMap.put(this.zoneMap.size(), zone); }

}
