package org.orgaprop.test7.models;

public class CellListZoneModel {

    private String text;
    private String note;

    public CellListZoneModel(String text, String note) {
        this.text = text;
        this.note = note;
    }

    public String getNote() {
        return note;
    }
    public CellListZoneModel setNote(String note) {
        this.note = note;

        return this;
    }

    public String getText() {
        return text;
    }
    public CellListZoneModel setText(String text) {
        this.text = text;

        return this;
    }
}
