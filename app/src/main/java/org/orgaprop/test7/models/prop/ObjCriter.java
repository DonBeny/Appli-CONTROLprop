package org.orgaprop.test7.models.prop;

import java.io.Serializable;

public class ObjCriter implements Serializable {

    /*  PRIVATE VARIABLES   */
    int id;
    int note;
    ObjComment comment;

    /*  CONSTRUCTORS    */
    public ObjCriter() {
        this.id = 0;
        this.note = 0;
        this.comment = new ObjComment();
    }
    public ObjCriter(int id) {
        this.id = id;
        this.note = 0;
        this.comment = new ObjComment();
    }
    public ObjCriter(int id, int note) {
        this.id = id;
        this.note = note;
        this.comment = new ObjComment();
    }

    /*  GETTERS */
    public int getId() { return id; }
    public int getNote() { return note; }
    public ObjComment getComment() { return comment; }

    /*  SETTERS */
    public void setId(int id) { this.id = id; }
    public void setNote(int note) { this.note = note; }
    public void setComment(ObjComment comment) { this.comment = comment; }
    public void setComment(String txt, String img) {
        this.comment.setTxt(txt);
        this.comment.setImg(img);
    }

}
