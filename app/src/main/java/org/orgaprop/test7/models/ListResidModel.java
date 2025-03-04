package org.orgaprop.test7.models;

import org.json.JSONException;
import org.json.JSONObject;

public class ListResidModel {

//********* PRIVATES VARIABLES

    private int id = 0;
    private String ref = "";
    private String name = "";
    private String entry = "";
    private String adr = "";
    private String city = "";
    private String agc = "";
    private String grp = "";
    private String ref_sector = "";
    private String ref_admin = "";
    private String ref_agent = "";
    private String ref_contra = "";
    private boolean visited = false;
    private String last = "";
    private String note = "";
    private Integer date = 0;
    private String conf_ctrl = "0;0;1;1";
    private JSONObject proxi = new JSONObject();
    private JSONObject contra = new JSONObject();
    private JSONObject old_ctrl = new JSONObject();
    private JSONObject plan_act = new JSONObject();

//********* STATIC VARIABLES

    private static final String TAG = "ListResidModel";

//********* CONSTRUCTORS

    public ListResidModel() {}

//********* PUBLIC FUNCTIONS

    public int getId() {
        return this.id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public String getRef() {
        return this.ref;
    }
    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getEntry() {
        return this.entry;
    }
    public void setEntry(String entry) {
        this.entry = entry;
    }

    public String getAdress() {
        return this.adr;
    }
    public void setAdresse(String adr) {
        this.adr = adr;
    }

    public String getCity() {
        return this.city;
    }
    public void setCity(String cp, String city) {
        this.city = cp + " " + city;
    }

    public String getAgc() {
        return this.agc;
    }
    public void setAgc(String agc) {
        this.agc = agc;
    }

    public String getGrp() {
        return this.grp;
    }
    public void setGrp(String grp) {
        this.grp = grp;
    }

    public String getRef_sector() {
        return this.ref_sector;
    }
    public void setRef_sector(String ref_sector) {
        this.ref_sector = ref_sector;
    }

    public String getRef_admin() {
        return this.ref_admin;
    }
    public void setRef_admin(String ref_admin) {
        this.ref_admin = ref_admin;
    }

    public String getRef_agent() {
        return this.ref_agent;
    }
    public void setRef_agent(String ref_agent) {
        this.ref_agent = ref_agent;
    }

    public String getRef_contra() {
        return this.ref_contra;
    }
    public void setRef_contra(String ref_contra) {
        this.ref_contra = ref_contra;
    }

    public boolean isVisited() {
        return this.visited;
    }
    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public String getLast() {
        return this.last;
    }
    public void setLast(String last) {
        this.last = last;
    }

    public Integer getDate() {
        return this.date;
    }
    public void setDate(Integer date) {
        this.date = date;
    }
    public void setDate(String date) {
        this.date = Integer.parseInt(date);
    }

    public String getStrConfCtrl() {
        return this.conf_ctrl;
    }
    public void setConfCtrl(String data) {
        this.conf_ctrl = data;
    }

    public JSONObject getProxi() {
        return this.proxi;
    }
    public void setProxi(JSONObject data) {
        this.proxi = data;
    }
    public void setProxi(String data) {
        try{
            this.proxi = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getContra() {
        return this.contra;
    }
    public void setContra(JSONObject data) {
        this.contra = data;
    }
    public void setContra(String data) {
        try{
            this.contra = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getOldCtrl() {
        return this.old_ctrl;
    }
    public void setOldCtrl(JSONObject data) {
        this.old_ctrl = data;
    }
    public void setOldCtrl(String data) {
        try{
            this.old_ctrl = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getPlanActions() {
        return this.plan_act;
    }
    public void setPlanActions(JSONObject data) {
        this.plan_act = data;
    }
    public void setPlanActions(String id, String date, String txt) {
        try {
            this.plan_act.put("id", id);
            this.plan_act.put("date", date);
            this.plan_act.put("txt", txt);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
    }
}
