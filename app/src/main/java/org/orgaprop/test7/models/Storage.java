package org.orgaprop.test7.models;

import android.content.ContentValues;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.json.JSONException;
import org.json.JSONObject;
import org.orgaprop.test7.databases.PrefDatabase;

@Entity(tableName = PrefDatabase.STORAGE_TABLE_NAME)
public class Storage {

//************ ENTITIES

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = PrefDatabase.STORAGE_COL_ID)
    private long id;

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_RESID)
    @NonNull private Integer resid = 0;

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_DATE)
    @NonNull private Integer date = 0;

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_CONFIG)
    @NonNull private String config = "";

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_TYPE_CTRL)
    @NonNull private String typeCtrl = "";

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_CTRL_TYPE)
    @NonNull private String ctrl_type = "";

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_CTRL_CTRL)
    @NonNull private String ctrl_ctrl = "";

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_CTRL_SIG1)
    @NonNull private String ctrl_sig1 = "";

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_CTRL_SIG2)
    @NonNull private String ctrl_sig2 = "";

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_CTRL_SIG)
    @NonNull private String ctrl_sig = "";

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_PLAN_END)
    @NonNull private Integer plan_end = 0;

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_PLAN_CONTENT)
    @NonNull private String plan_content = "";

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_PLAN_VALIDATE)
    @NonNull private Boolean plan_validate = false;

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_SEND_DEST)
    @NonNull private String send_dest = "";

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_SEND_ID_PLAN)
    @NonNull private Integer send_idPlan = 0;

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_SEND_DATE_CTRL)
    @NonNull private Integer send_dateCtrl = 0;

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_SEND_TYPE_CTRL)
    @NonNull private String send_typeCtrl = "";

    @ColumnInfo(name = PrefDatabase.STORAGE_COL_SEND_SRC)
    @NonNull private String send_src = "";

//************ CONSTANTS

    public static final String STORAGE_PARAM_ID = "id";
    public static final String STORAGE_PARAM_RESID = "resid";
    public static final String STORAGE_PARAM_DATE = "date";
    public static final String STORAGE_PARAM_TYPE = "type";
    public static final String STORAGE_PARAM_CONFIG = "conf";
    public static final String STORAGE_PARAM_CTRL = "ctrl";
    public static final String STORAGE_PARAM_CTRL_TYPE = "type";
    public static final String STORAGE_PARAM_CTRL_GRILLE = "grill";
    public static final String STORAGE_PARAM_CTRL_SIG1 = "sig1";
    public static final String STORAGE_PARAM_CTRL_SIG2 = "sig2";
    public static final String STORAGE_PARAM_CTRL_AGT = "agt";
    public static final String STORAGE_PARAM_PLAN_ACTIONS = "plan";
    public static final String STORAGE_PARAM_PLAN_ECHEANCE = "end";
    public static final String STORAGE_PARAM_PLAN_CONTENT = "txt";
    public static final String STORAGE_PARAM_PLAN_VALIDATE = "close";
    public static final String STORAGE_PARAM_SEND = "send";
    public static final String STORAGE_PARAM_SEND_DEST = "dest";
    public static final String STORAGE_PARAM_SEND_PLAN_ID = "plan";
    public static final String STORAGE_PARAM_SEND_CTRL_DATE = "ctrl";
    public static final String STORAGE_PARAM_SEND_CTRL_TYPE = "type";
    public static final String STORAGE_PARAM_SEND_SRC = "src";

//************ CONSTRUCTORS

    @Ignore
    public Storage() {}
    public Storage(long storageId) {
        this.id = storageId;
    }
    public Storage(int resid) { this.resid = resid; }

//************ GETTERS

    public long getId() { return this.id; }
    public int getResid() { return this.resid; }
    public int getDate() { return this.date; }
    @NonNull public String getConfig() { return this.config; }
    @NonNull public String getTypeCtrl() { return this.typeCtrl; }
    @NonNull public String getCtrl_type() { return this.ctrl_type; }
    @NonNull public String getCtrl_ctrl() { return this.ctrl_ctrl; }
    @NonNull public String getCtrl_sig1() { return this.ctrl_sig1; }
    @NonNull public String getCtrl_sig2() { return this.ctrl_sig2; }
    @NonNull public String getCtrl_sig() { return this.ctrl_sig; }
    public int getPlan_end() { return this.plan_end; }
    @NonNull public String getPlan_content() { return this.plan_content; }
    public boolean getPlan_validate() { return this.plan_validate; }
    @NonNull public String getSend_dest() { return this.send_dest; }
    public int getSend_idPlan() { return this.send_idPlan; }
    public int getSend_dateCtrl() { return this.send_dateCtrl; }
    @NonNull public String getSend_typeCtrl() { return this.send_typeCtrl; }
    @NonNull public String getSend_src() { return this.send_src; }

//************ SETTERS

    public void setId(long id) { this.id = id; }
    public void setResid(int resid) { this.resid = resid; }
    public void setDate(int date) { this.date = date; }
    public void setConfig(@NonNull String config) { this.config = config; }
    public void setTypeCtrl(@NonNull String typeCtrl) { this.typeCtrl = typeCtrl; }
    public void setCtrl_type(@NonNull String type) { this.ctrl_type = type; }
    public void setCtrl_ctrl(@NonNull String grill) { this.ctrl_ctrl = grill; }
    public void setCtrl_sig1(@NonNull String signature) { this.ctrl_sig1 = signature; }
    public void setCtrl_sig2(@NonNull String signature) { this.ctrl_sig2 = signature; }
    public void setCtrl_sig(@NonNull String agent) { this.ctrl_sig = agent; }
    public void setPlan_end(int end) { this.plan_end = end; }
    public void setPlan_content(@NonNull String content) { this.plan_content = content; }
    public void setPlan_validate(boolean validate) { this.plan_validate = validate; }
    public void setSend_dest(@NonNull String dest) { this.send_dest = dest; }
    public void setSend_idPlan(int idPlan) { this.send_idPlan = idPlan; }
    public void setSend_dateCtrl(int dateCtrl) { this.send_dateCtrl = dateCtrl; }
    public void setSend_typeCtrl(@NonNull String type) { this.send_typeCtrl = type; }
    public void setSend_src(@NonNull String src) { this.send_src = src; }

//************  UTILS

    public static Storage fromContentValues(ContentValues values) {
        final Storage storage = new Storage();

        if( values.containsKey("id") ) {
            storage.setId(values.getAsLong("id"));
        }

        if( values.containsKey("resid") ) {
            storage.setResid(values.getAsInteger("resid"));
        }
        if( values.containsKey("rsd") ) {
            storage.setResid(values.getAsInteger("rsd"));
        }

        if( values.containsKey("date") ) {
            storage.setDate(values.getAsInteger("date"));
        }
        if( values.containsKey("ctrl_date") ) {
            storage.setDate(values.getAsInteger("ctrl_date"));
        }
        if( values.containsKey("date_ctrl") ) {
            storage.setDate(values.getAsInteger("date_ctrl"));
        }

        if( values.containsKey("conf") ) {
            storage.setConfig(values.getAsString("conf"));
        }
        if( values.containsKey("config") ) {
            storage.setConfig(values.getAsString("config"));
        }

        if( values.containsKey("type") ) {
            storage.setTypeCtrl(values.getAsString("type"));
            storage.setSend_src(values.getAsString("type"));
        }

        if( values.containsKey("type_ctrl") ) {
            storage.setCtrl_type(values.getAsString("type_ctrl"));
            storage.setSend_typeCtrl(values.getAsString("type_ctrl"));
        }
        if( values.containsKey("ctrl_type") ) {
            storage.setCtrl_type(values.getAsString("ctrl_type"));
            storage.setSend_typeCtrl(values.getAsString("ctrl_type"));
        }

        if( values.containsKey("grill") ) {
            storage.setCtrl_ctrl(values.getAsString("grill"));
        }
        if( values.containsKey("grille") ) {
            storage.setCtrl_ctrl(values.getAsString("grille"));
        }
        if( values.containsKey("ctrl") ) {
            storage.setCtrl_ctrl(values.getAsString("ctrl"));
        }

        if( values.containsKey(("sig1")) ) {
            storage.setCtrl_sig1(values.getAsString("sig1"));
        }
        if( values.containsKey(("sig_controleur")) ) {
            storage.setCtrl_sig1(values.getAsString("sig_controleur"));
        }
        if( values.containsKey(("signature")) ) {
            storage.setCtrl_sig1(values.getAsString("signature"));
        }
        if( values.containsKey(("sign")) ) {
            storage.setCtrl_sig1(values.getAsString("sign"));
        }
        if( values.containsKey(("sig_controleur")) ) {
            storage.setCtrl_sig1(values.getAsString("sig_controleur"));
        }
        if( values.containsKey(("sign_controleur")) ) {
            storage.setCtrl_sig1(values.getAsString("sign_controleur"));
        }
        if( values.containsKey(("signature_controleur")) ) {
            storage.setCtrl_sig1(values.getAsString("signature_controleur"));
        }

        if( values.containsKey(("sig2")) ) {
            storage.setCtrl_sig2(values.getAsString("sig2"));
        }
        if( values.containsKey(("sig_agent")) ) {
            storage.setCtrl_sig2(values.getAsString("sig_agent"));
        }
        if( values.containsKey(("sig_contradictoir")) ) {
            storage.setCtrl_sig2(values.getAsString("sig_contradictoir"));
        }
        if( values.containsKey(("sig_contradictoire")) ) {
            storage.setCtrl_sig2(values.getAsString("sig_contradictoire"));
        }
        if( values.containsKey(("sign_contradictoir")) ) {
            storage.setCtrl_sig2(values.getAsString("sign_contradictoir"));
        }
        if( values.containsKey(("sign_contradictoire")) ) {
            storage.setCtrl_sig2(values.getAsString("sign_contradictoire"));
        }
        if( values.containsKey(("signature_contradictoir")) ) {
            storage.setCtrl_sig2(values.getAsString("signature_contradictoir"));
        }
        if( values.containsKey(("signature_contradictoire")) ) {
            storage.setCtrl_sig2(values.getAsString("signature_contradictoire"));
        }

        if( values.containsKey(("sig")) ) {
            storage.setCtrl_sig(values.getAsString("sig"));
        }
        if( values.containsKey(("agent")) ) {
            storage.setCtrl_sig(values.getAsString("agent"));
        }
        if( values.containsKey(("nom_agent")) ) {
            storage.setCtrl_sig(values.getAsString("nom_agent"));
        }
        if( values.containsKey(("contradicteur")) ) {
            storage.setCtrl_sig(values.getAsString("contradicteur"));
        }
        if( values.containsKey(("contradictoir")) ) {
            storage.setCtrl_sig(values.getAsString("contradictoir"));
        }
        if( values.containsKey(("contradictoire")) ) {
            storage.setCtrl_sig(values.getAsString("contradictoire"));
        }

        if( values.containsKey(("plan_end")) ) {
            storage.setPlan_end(values.getAsInteger("plan_end"));
        }
        if( values.containsKey(("plan_fin")) ) {
            storage.setPlan_end(values.getAsInteger("plan_fin"));
        }
        if( values.containsKey(("echeance")) ) {
            storage.setPlan_end(values.getAsInteger("echeance"));
        }

        if( values.containsKey(("plan_content")) ) {
            storage.setPlan_content(values.getAsString("plan_content"));
        }
        if( values.containsKey(("plan_txt")) ) {
            storage.setPlan_content(values.getAsString("plan_txt"));
        }

        if( values.containsKey(("plan_valid")) ) {
            storage.setPlan_validate(values.getAsBoolean("plan_valid"));
        }
        if( values.containsKey(("plan_validate")) ) {
            storage.setPlan_validate(values.getAsBoolean("plan_validate"));
        }

        if( values.containsKey(("send_dest")) ) {
            storage.setSend_dest(values.getAsString("send_dest"));
        }
        if( values.containsKey(("dest_send")) ) {
            storage.setSend_dest(values.getAsString("dest_send"));
        }
        if( values.containsKey(("dest")) ) {
            storage.setSend_dest(values.getAsString("dest"));
        }
        if( values.containsKey(("destinataire")) ) {
            storage.setSend_dest(values.getAsString("destinataire"));
        }

        if( values.containsKey(("send_idPlan")) ) {
            storage.setSend_idPlan(values.getAsInteger("send_idPlan"));
        }
        if( values.containsKey(("send_plan")) ) {
            storage.setSend_idPlan(values.getAsInteger("send_plan"));
        }

        if( values.containsKey(("send_dateCtrl")) ) {
            storage.setSend_dateCtrl(values.getAsInteger("send_dateCtrl"));
        }
        if( values.containsKey(("send_date")) ) {
            storage.setSend_dateCtrl(values.getAsInteger("send_date"));
        }

        return storage;
    }
    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        JSONObject objCtrl = new JSONObject();
        JSONObject objPlan = new JSONObject();
        JSONObject objSend = new JSONObject();

        try {
            result.put("rsd", this.getResid());
            result.put("date", this.getDate());
            result.put("type", this.getTypeCtrl());
            result.put("conf", this.getConfig());

            objCtrl.put("type", this.getCtrl_type());
            objCtrl.put("grill", this.getCtrl_ctrl());
            objCtrl.put("sig1", this.getCtrl_sig1());
            objCtrl.put("sig2", this.getCtrl_sig2());
            objCtrl.put("agt", this.getCtrl_sig());
            result.put("ctrl", objCtrl);

            objPlan.put("end", this.getPlan_end());
            objPlan.put("txt", this.getPlan_content());
            objPlan.put("close", this.getPlan_validate());
            result.put("plan", objPlan);

            objSend.put("dest", this.getSend_dest());
            objSend.put("plan", this.getSend_idPlan());
            objSend.put("ctrl", this.getSend_dateCtrl());
            objSend.put("type", this.getSend_typeCtrl());
            objSend.put("src", this.getSend_src());
            result.put("send", objSend);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

}
