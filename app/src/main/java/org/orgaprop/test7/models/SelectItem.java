package org.orgaprop.test7.models;

import java.io.Serializable;

public class SelectItem implements Serializable {

    private int id;
    private String ref;
    private String name;
    private String entry;
    private String address;
    private String postalCode;
    private String city;
    private String last;
    private boolean isVisited;
    private int idAgency;
    private String nameAgency;
    private int idGroup;
    private String nameGroup;
    private String comment;
    private ObjProp objProp;

    public SelectItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public SelectItem(int id, int agency, int group, String ref, String name, String entry, String address, String postalCode, String city, String last, boolean isVisited, String comment) {
        this.id = id;
        this.idAgency = agency;
        this.idGroup = group;
        this.ref = ref;
        this.name = name;
        this.entry = entry;
        this.address = address;
        this.postalCode = postalCode;
        this.city = city;
        this.last = last;
        this.isVisited = isVisited;
        this.comment = comment;
        this.objProp = new ObjProp();
    }

    public int getId() { return id; }
    public int getIdAgency() { return idAgency; }
    public String getNameAgency() { return nameAgency; }
    public int getIdGroup() { return idGroup; }
    public String getNameGroup() { return nameGroup; }
    public String getRef() { return ref; }
    public String getName() { return name; }
    public String getEntry() { return entry; }
    public String getAddress() { return address; }
    public String getPostalCode() { return postalCode; }
    public String getCity() { return city; }
    public String getLast() { return last; }
    public boolean getIsVisited() { return isVisited; }
    public String getComment() { return comment; }
    public ObjProp getObjProp() { return objProp; }

    public void setIdAgency(int agency) { this.idAgency = agency; }
    public void setNameAgency(String name) { this.nameAgency = name; }
    public void setIdGroup(int group) { this.idGroup = group; }
    public void setNameGroup(String name) { this.nameGroup = name; }
    public void setRef(String ref) { this.ref = ref; }
    public void setName(String name) { this.name = name;}
    public void setEntry(String entry) { this.entry = entry;}
    public void setAddress(String address) { this.address = address; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setCity(String city) { this.city = city; }
    public void setLast(String last) { this.last = last; }
    public void setIsVisited(boolean isVisited) { this.isVisited = isVisited; }
    public void setComment(String comment) { this.comment = comment; }
    public void setObjProp(ObjProp objProp) { this.objProp = objProp; }

}
