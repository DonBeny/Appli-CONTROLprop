package org.orgaprop.test7.models.prop;

import java.io.Serializable;

public class ObjDateCtrl implements Serializable {

    /*  PRIVATE VARIABLES   */
    private int value;
    private String txt;

    /*  CONSTRUCTORS    */
    public ObjDateCtrl() {
        this.value = 0;
        this.txt = "";
    }
    public ObjDateCtrl(int value, String txt) {
        this.value = value;
        this.txt = txt;
    }

    /*  GETTERS */
    public int getValue() { return value; }
    public String getTxt() { return txt; }

    /*  SETTERS */
    public void setValue(int value) { this.value = value; }
    public void setTxt(String txt) { this.txt = txt; }

}
