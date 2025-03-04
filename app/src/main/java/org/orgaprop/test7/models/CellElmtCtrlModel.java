package org.orgaprop.test7.models;

import org.orgaprop.test7.controllers.activities.MakeCtrlActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class CellElmtCtrlModel {

//********* PRIVATE VARIABLES

    private int position;
    private String idElement = null;
    private int coef = 0;
    private String text = null;
    private ArrayList<CellCriterCtrlModel> listCriters;

//********* STATIC VARIABLES

    private static final String TAG = "CellElmtCtrlModel";

//********* CONSTRUCTORS

    public CellElmtCtrlModel() {
        idElement = "new";
        listCriters = new ArrayList<CellCriterCtrlModel>();
    }
    public CellElmtCtrlModel(int position) {
        this.position = position;
        this.idElement = "new";
        this.listCriters = new ArrayList<CellCriterCtrlModel>();
    }
    public CellElmtCtrlModel(String donn) {
        listCriters = new ArrayList<CellCriterCtrlModel>();

        init(donn);
    }
    public CellElmtCtrlModel(int position, String donn) {
        this.position = position;
        this.listCriters = new ArrayList<CellCriterCtrlModel>();

        init(donn);
    }
    public CellElmtCtrlModel(String idElement, String text, int coef, ArrayList<CellCriterCtrlModel> listCriters) {
        this.idElement = idElement;
        this.text = text;
        this.coef = coef;
        this.listCriters = listCriters;
    }
    public CellElmtCtrlModel(int position, String idElement, String text, int coef, ArrayList<CellCriterCtrlModel> listCriters) {
        this.position = position;
        this.idElement = idElement;
        this.text = text;
        this.coef = coef;
        this.listCriters = listCriters;
    }

//********* SURCHARGES

    @Override
    public String toString() {
        return getId() + ": " + getText() + " (coef: " + getCoef() + ") => " + listCriters.size() + " critère(s)";
    }

//********* PUBLIC FUNCTIONS

    public CellElmtCtrlModel init(String donn) {
        StringTokenizer token = new StringTokenizer(donn, "§");

        try{
            idElement = token.nextToken().substring(1);
            coef = (int) Integer.parseInt(token.nextToken());

            while( token.hasMoreTokens() ) {
                CellCriterCtrlModel criter = new CellCriterCtrlModel();

                criter.setId(token.nextToken());
                criter.setValue(Integer.parseInt(token.nextToken()));
                criter.setCoef(Integer.parseInt(token.nextToken()));
                criter.setComment(token.nextToken());
                criter.setCapture(token.nextToken());

                listCriters.add(criter);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return this;
    }

    public CellElmtCtrlModel setPosition(int position) {
        this.position = position;

        return this;
    }
    public int getPosition() {
        return position;
    }

    public CellElmtCtrlModel setId(String id) {
        idElement = id;

        return this;
    }
    public String getId() {
        return idElement;
    }

    public CellElmtCtrlModel setCoef(int coef) {
        this.coef = coef;

        return this;
    }
    public int getCoef() {
        return coef;
    }

    public CellElmtCtrlModel setText(String text) {
        this.text = text;

        return this;
    }
    public String getText() {
        return text;
    }

    public CellCriterCtrlModel getCriter(int position) {
        return listCriters.get(position);
    }
    public List<CellCriterCtrlModel> getCriters() {
        return listCriters;
    }

    public CellElmtCtrlModel addCriter(CellCriterCtrlModel criter) {
        listCriters.add(criter);

        return this;
    }
    public CellElmtCtrlModel addCriter(String donn) {
        CellCriterCtrlModel criter = new CellCriterCtrlModel();

        criter.init(donn);

        listCriters.add(criter);

        return this;
    }
    public CellElmtCtrlModel addCriters(ArrayList<CellCriterCtrlModel> list) {
        listCriters.addAll(list);

        return this;
    }
    public CellElmtCtrlModel addCriters(String donn) {
        StringTokenizer token = new StringTokenizer(donn, "§");

        while( token.hasMoreTokens() ) {
            try{
                CellCriterCtrlModel criter = new CellCriterCtrlModel();

                criter.setId(token.nextToken());
                criter.setValue(Integer.parseInt(token.nextToken()));
                criter.setCoef(Integer.parseInt(token.nextToken()));
                criter.setComment(token.nextToken());
                criter.setCapture(token.nextToken());
                criter.setText(token.nextToken());

                addCriter(criter);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return this;
    }

    public NoteModel note() {
        NoteModel result = new NoteModel();
        float note = 0;
        int max = 0;

        for( CellCriterCtrlModel criter : listCriters ) {
            if( criter.getValue() != 0 ) {
                int calc = criter.getValue();

                if( calc < 0 ) calc = 0;

                note += (((calc * criter.getCoef()) + ((calc * criter.getCoef()) * (MakeCtrlActivity.meteo / 10.0))) * coef);
                max += (criter.getCoef() * coef);
            }
        }

        result.note = note;
        result.max = max;

        return result;
    }

}
