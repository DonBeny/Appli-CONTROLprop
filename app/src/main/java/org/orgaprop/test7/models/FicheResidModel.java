package org.orgaprop.test7.models;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class FicheResidModel {

//********* PRIVATE VARIABLES

    private String idRsd;
    private String idCtrl;

    private String refRsd;
    private String nomRsd;
    private String entreeRsd;
    private String adressRsd;

    private String refEncadrant;
    private String refGardien;
    private String refEntretien;
    private CtrlZoneAdapter adapter;
    private ArrayList<CellZoneCtrlModel> listZones;

    private String idPlanAction;
    private String datePlanAction;
    private String textPlanAtion;

    private String commentRsd;

//********* STATIC VARIABLES

    private static final String TAG = "FicheResidModel";

//********* CONSTRUCTORS

    public FicheResidModel() {
        listZones = new ArrayList<>();
    }

//********* PUBLIC FUNCTIONS

    public FicheResidModel setId(String idRsd) {
        this.idRsd = idRsd;

        return this;
    }
    public String getId() {
        return idRsd;
    }

    public FicheResidModel setCtrl(String idCtrl) {
        this.idCtrl = idCtrl;

        return this;
    }
    public String getCtrl() {
        return idCtrl;
    }

    public FicheResidModel setRef(String refRsd) {
        this.refRsd = refRsd;

        return this;
    }
    public String getRef() {
        return refRsd;
    }

    public FicheResidModel setNom(String nomRsd) {
        this.nomRsd = nomRsd;

        return this;
    }
    public String getNom() {
        return nomRsd;
    }

    public FicheResidModel setEntree(String entreeRsd) {
        this.entreeRsd = entreeRsd;

        return this;
    }
    public String getEntree() {
        return entreeRsd;
    }

    public FicheResidModel setAdress(String adressRsd) {
        this.adressRsd = adressRsd;

        return this;
    }
    public String getAdress() {
        return adressRsd;
    }

    public FicheResidModel setEncadrant(String encadrant) {
        refEncadrant = encadrant;

        return this;
    }
    public String getEncadrant() {
        return refEncadrant;
    }

    public FicheResidModel setGardien(String gardien) {
        refGardien = gardien;

        return this;
    }
    public String getGardien() {
        return refGardien;
    }

    public FicheResidModel setAgent(String agent) {
        refEntretien = agent;

        return this;
    }
    public String getAgent() {
        return refEntretien;
    }

    public FicheResidModel setZones(ArrayList<CellZoneCtrlModel> list) {
        listZones = list;

        return this;
    }
    public FicheResidModel setZones(String donn) {
        StringTokenizer token = new StringTokenizer(donn, "§");

        boolean firstZone = true;
        boolean firstElement = true;

        CellZoneCtrlModel zone = new CellZoneCtrlModel();
        CellElmtCtrlModel element = new CellElmtCtrlModel();

        try {
            while( token.hasMoreTokens() ) {
                String strToken = token.nextToken();

                if( strToken.charAt(0) == '$' ) {
                    if (firstZone) {
                        firstZone = false;
                    } else {
                        zone.addElement(element);

                        listZones.add(zone);
                    }

                    zone = new CellZoneCtrlModel();
                    zone.setId(strToken.substring(1))
                            .setCoef(Integer.parseInt(token.nextToken()))
                            .setText(token.nextToken());

                    element = new CellElmtCtrlModel();
                    firstElement = true;
                } else {
                    if( strToken.charAt(0) == '_' ) {
                        if( firstElement ) {
                            firstElement = false;
                        } else {
                            zone.addElement(element);
                        }

                        element = new CellElmtCtrlModel();
                        element.setId(strToken.substring(1))
                                .setCoef(Integer.parseInt(token.nextToken()))
                                .setText(token.nextToken());
                    } else {
                        CellCriterCtrlModel criter = new CellCriterCtrlModel();

                        criter.setId(strToken)
                                .setValue(Integer.parseInt(token.nextToken()))
                                .setCoef(Integer.parseInt(token.nextToken()))
                                .setComment(token.nextToken())
                                .setCapture(token.nextToken())
                                .setText(token.nextToken());

                        element.addCriter(criter);
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return this;
    }
    public CellZoneCtrlModel getZone(int position) {
        return listZones.get(position);
    }
    public ArrayList<CellZoneCtrlModel> getZones() {
        return listZones;
    }

    public FicheResidModel addZone(CellZoneCtrlModel zone) {
        listZones.add(zone);

        return this;
    }
    public FicheResidModel addZone(String donn) {
        try {
            StringTokenizer token = new StringTokenizer(donn, "§");

            boolean first = true;

            CellZoneCtrlModel zone = new CellZoneCtrlModel();
            CellElmtCtrlModel element = new CellElmtCtrlModel();

            zone.setId(token.nextToken());
            zone.setCoef(Integer.parseInt(token.nextToken()));

            while( token.hasMoreTokens() ) {
                String strToken = token.nextToken();

                if( strToken.charAt(0) == '_' ) {
                    if( first ) {
                        first = false;
                    } else {
                        zone.addElement(element);
                    }

                    element = new CellElmtCtrlModel();
                    element.setId(strToken.substring(1))
                            .setCoef(Integer.parseInt(token.nextToken()))
                            .setText(token.nextToken());
                } else {
                    CellCriterCtrlModel criter = new CellCriterCtrlModel();

                    criter.setId(strToken)
                            .setValue(Integer.parseInt(token.nextToken()))
                            .setCoef(Integer.parseInt(token.nextToken()))
                            .setComment(token.nextToken())
                            .setCapture(token.nextToken())
                            .setText(token.nextToken());

                    element.addCriter(criter);
                }
            }

            listZones.add(zone);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return this;
    }

    public FicheResidModel setPlanAction(String id, String date, String text) {
        this.idPlanAction = id;
        this.datePlanAction = date;
        this.textPlanAtion = text;

        return this;
    }
    public FicheResidModel setPlanAction(String date, String text) {
        this.datePlanAction = date;
        this.textPlanAtion = text;

        return this;
    }
    public FicheResidModel setIdPlanAct(String id) {
        this.idPlanAction = id;

        return this;
    }
    public String getPlanAction(String val) {
        String result = "";

        switch (val) {
            case "id": result = this.idPlanAction; break;
            case "date": result = this.datePlanAction; break;
            case "text": result = this.textPlanAtion; break;
        }

        return result;
    }

    public FicheResidModel setCommentRsd(String comment) {
        this.commentRsd = comment;

        return this;
    }
    public String getCommentRsd() {
        return this.commentRsd;
    }

}
