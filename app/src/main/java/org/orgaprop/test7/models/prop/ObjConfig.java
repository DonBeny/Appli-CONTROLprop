package org.orgaprop.test7.models.prop;

import java.io.Serializable;

public class ObjConfig implements Serializable {

    /*  PRIVATE VARIABLES   */
    private boolean visite;
    private boolean meteo;
    private boolean affichage;
    private boolean produits;

    /*  CONSTRUCTORS    */
    public ObjConfig() {
        this.visite = false;
        this.meteo = false;
        this.affichage = true;
        this.produits = true;
    }
    public ObjConfig(boolean visite, boolean meteo, boolean affichage, boolean produits) {
        this.visite = visite;
        this.meteo = meteo;
        this.affichage = affichage;
        this.produits = affichage;
    }

    /*  GETTERS */
    public boolean getVisite() { return visite; }
    public boolean getMeteo() { return meteo; }
    public boolean getAffichage() { return affichage; }
    public boolean getProduits() { return produits; }

    /*  SETTERS */
    public void setVisite(boolean inopinee) { this.visite = inopinee; }
    public void setMeteo(boolean perturbee) { this.meteo = perturbee; }
    public void setAffichage(boolean conforme) { this.affichage = conforme; }
    public void setProduits(boolean conforme) { this.produits = conforme; }

}
