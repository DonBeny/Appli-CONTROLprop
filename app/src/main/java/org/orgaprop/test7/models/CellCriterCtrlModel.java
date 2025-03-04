package org.orgaprop.test7.models;

import java.util.StringTokenizer;

public class CellCriterCtrlModel {

//********* PRIVATES VARIABLES

    private int position = 0;

    private String idCriter = null;
    private int value = 0;
    private int coef = 0;
    private String comment = null;
    private String capture = null;

    private String text = null;
    private String captureUri = "";

    private boolean hasComment = false;
    private boolean hasCapture = false;

//********* STATIC VARIABLES

    private static final String TAG = "CellCriterCtrlModel";

    public static final int CELL_CRITER_CTRL_NC = 0;
    public static final int CELL_CRITER_CTRL_CONFORM = 1;
    public static final int CELL_CRITER_CTRL_NO_CONFORM = -1;

//********* CONSTRUCTORS

    public CellCriterCtrlModel() {
        idCriter = "new";
    }
    public CellCriterCtrlModel(int position) {
        this.position = position;
        this.idCriter = "new";
    }
    public CellCriterCtrlModel(String donn) {
        init(donn);
    }
    public CellCriterCtrlModel(int position, String donn) {
        this.position = position;

        init(donn);
    }
    public CellCriterCtrlModel(String idCriter, String text, int coef) {
        this.idCriter = idCriter;
        this.text = text;
        this.coef = coef;
    }
    public CellCriterCtrlModel(int position, String idCriter, String text, int coef) {
        this.position = position;
        this.idCriter = idCriter;
        this.text = text;
        this.coef = coef;
    }
    public CellCriterCtrlModel(String idCriter, String text, int coef, int value, String comment, String capture) {
        this.idCriter = idCriter;
        this.text = text;
        this.coef = coef;
        this.value = value;
        this.comment = comment;
        this.capture = capture;
    }
    public CellCriterCtrlModel(int position, String idCriter, String text, int coef, int value, String comment, String capture) {
        this.position = position;
        this.idCriter = idCriter;
        this.text = text;
        this.coef = coef;
        this.value = value;
        this.comment = comment;
        this.capture = capture;
    }

//********* SURCHARGES

    @Override
    public String toString() {
        String rslt = getId() + ": " + getText() + " (coef: " + getCoef() + ")";

        if( hasComment() ) {
            rslt += " => comment: " + getComment();
        }
        if( hasCapture() ) {
            rslt += " => capture: " + getCapture();
        }

        return rslt;
    }

//********* PUBLIC FUNCTIONS

    public CellCriterCtrlModel setPosition(int position) {
        this.position = position;

        return this;
    }
    public int getPosition() {
        return position;
    }

    public CellCriterCtrlModel setId(String idCriter) {
        this.idCriter = idCriter;

        return this;
    }
    public String getId() {
        return idCriter;
    }

    public CellCriterCtrlModel setValue(int value) {
        this.value = value;

        return this;
    }
    public int getValue() {
        return value;
    }

    public CellCriterCtrlModel setCoef(int coef) {
        this.coef = coef;

        return this;
    }
    public int getCoef() {
        return coef;
    }

    public CellCriterCtrlModel setComment(String comment) {
        this.comment = comment;
        hasComment = (this.comment.length() > 0);

        return this;
    }
    public String getComment() {
        return comment;
    }

    public CellCriterCtrlModel setCapture(String capture) {
        this.capture = capture;
        hasCapture = (this.capture.length() > 0);

        return this;
    }
    public String getCapture() {
        return capture;
    }

    public void setCaptureUri(String uri) {
        this.captureUri = uri;
    }
    public String getCaptureUri() {
        return captureUri;
    }

    public CellCriterCtrlModel setText(String text) {
        this.text = text;

        return this;
    }
    public String getText() {
        return text;
    }

    public void init(String donn) {
        try {
            StringTokenizer list = new StringTokenizer(donn, "§");

            idCriter = list.nextToken();
            value = Integer.parseInt(list.nextToken());
            coef = Integer.parseInt(list.nextToken());
            comment = list.nextToken();
            capture = list.nextToken();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public boolean hasComment() {
        return hasComment;
    }
    public boolean hasCapture() {
        return hasCapture;
    }

}
