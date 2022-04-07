package com.ukf.app2;

import javax.persistence.*;

@Entity
@Table(name = "activity")
public class Activity {
    private long id;
    private String nazov;
    private String datum;
    private String popis;
    @Lob
    @Column(name = "photo", columnDefinition="BLOB")
    private byte[] obr;
    private long user_id;

    public Activity() {
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNazov() {
        return nazov;
    }

    public void setNazov(String nazov) {
        this.nazov = nazov;
    }

    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
    }

    public String getPopis() {
        return popis;
    }

    public void setPopis(String popis) {
        this.popis = popis;
    }

    public byte[] getObr() {
        return obr;
    }

    public void setObr(byte[] obr) {
        this.obr = obr;
    }

    public long getUser_id() {
        return user_id;
    }

    public void setUser_id(long user_id) {
        this.user_id = user_id;
    }
}
