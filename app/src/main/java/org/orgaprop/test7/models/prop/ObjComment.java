package org.orgaprop.test7.models.prop;

import java.io.Serializable;

public class ObjComment implements Serializable {

    /*  PRIVATE VARIABLES*/
    private String txt;
    private String img;

    /*  CONSTRUCTORS    */
    public ObjComment() {
        this.txt = "";
        this.img = "";
    }
    public ObjComment(String txt, String img) {
        this.txt = txt;
        this.img = img;
    }

    /*  GETTERS */
    public String getTxt() { return txt; }
    public String getImg() { return img; }

    /*  SETTERS */
    public void setTxt(String txt) { this.txt = txt; }
    public void setImg(String img) { this.img = img; }

}
