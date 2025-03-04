package org.orgaprop.test7.models;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class CellZoneCtrlModel {

//********* PRIVATE VARIABLES

    private String idZone = null;
    private int coef = 0;
    private String text = null;
    private ArrayList<CellElmtCtrlModel> listElements;
    private int view = -1;
    private int position;

//********* STATIC VARIABLES

    private static final String TAG = "CellZoneCtrlModel";

//********* CONSTRUCTORS

    public CellZoneCtrlModel() {
        position = 0;
        idZone = "new";
        listElements = new ArrayList<CellElmtCtrlModel>();
    }
    public CellZoneCtrlModel(int position) {
        this.position = position;
        this.idZone = "new";
        this.listElements = new ArrayList<>();
    }
    public CellZoneCtrlModel(int position, String donn) {
        this.position = position;
        listElements = new ArrayList<CellElmtCtrlModel>();

        init(donn);
    }
    public CellZoneCtrlModel(int position,String idZone, String text, int coef) {
        this.position = position;
        this.idZone = idZone;
        this.text = text;
        this.coef = coef;
        this.listElements = new ArrayList<CellElmtCtrlModel>();
    }
    public CellZoneCtrlModel(int position, String idZone, String text, int coef, ArrayList<CellElmtCtrlModel> listElements) {
        this.position = position;
        this.idZone = idZone;
        this.text = text;
        this.coef = coef;
        this.listElements = listElements;
    }
    public CellZoneCtrlModel(int position, String idZone, String text, int coef, ArrayList<CellElmtCtrlModel> listElements, int view) {
        this.position = position;
        this.idZone = idZone;
        this.text = text;
        this.coef = coef;
        this.listElements = listElements;
        this.view = view;
    }

//********* SURCHARGES

    @Override
    public String toString() {
        return getId() + ": " + getText() + " (coef: " + getCoef() + ") => " + listElements.size() + " élémént(s)";
    }

//********* PRIVATE FUNCTIONS

    private CellZoneCtrlModel init(String donn) {
        // TODO : init from string

        return this;

    }

//********* PUBLIC FUNCTIONS

    public CellZoneCtrlModel setId(String idZone) {
        this.idZone = idZone;

        return this;
    }
    public String getId() {
        return idZone;
    }

    public CellZoneCtrlModel setPosition(int position) {
        this.position = position;

        return this;
    }
    public int getPosition() {
        return position;
    }

    public CellZoneCtrlModel setCoef(int coef) {
        this.coef = coef;

        return this;
    }
    public int getCoef() {
        return coef;
    }

    public CellZoneCtrlModel setText(String text) {
        this.text = text;

        return this;
    }
    public String getText() {
        return text;
    }

    public CellZoneCtrlModel setView(int idView) {
        this.view = idView;

        return this;
    }
    public int getView() {
        return this.view;
    }

    public CellZoneCtrlModel addElement(CellElmtCtrlModel item) {
        listElements.add(item);

        return this;
    }
    public CellZoneCtrlModel addElement(String donn) {
        try {
            StringTokenizer token = new StringTokenizer(donn, "§");

            CellElmtCtrlModel item = new CellElmtCtrlModel();

            item.setId(token.nextToken());
            item.setCoef(Integer.parseInt(token.nextToken()));
            item.setText(token.nextToken());

            while( token.hasMoreTokens() ) {
                try {
                    CellCriterCtrlModel criter = new CellCriterCtrlModel();

                    criter.setId(token.nextToken());
                    criter.setValue(Integer.parseInt(token.nextToken()));
                    criter.setCoef(Integer.parseInt(token.nextToken()));
                    criter.setComment(token.nextToken());
                    criter.setCapture(token.nextToken());
                    criter.setText(token.nextToken());

                    item.addCriter(criter);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            listElements.add(item);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return this;
    }

    public CellElmtCtrlModel getElement(int position) {
        return listElements.get(position);
    }
    public ArrayList<CellElmtCtrlModel> getElements() {
        return listElements;
    }

    public NoteModel note() {
        NoteModel result = new NoteModel();
        float note = 0;
        int max = 0;

        for( CellElmtCtrlModel item : listElements ) {
            NoteModel noteElement = item.note();

            note += noteElement.note * coef;
            max += noteElement.max * coef;
        }

        result.note = note;
        result.max = max;

        return result;
    }

}
