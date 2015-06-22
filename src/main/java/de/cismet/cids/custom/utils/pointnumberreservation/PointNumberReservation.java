/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.pointnumberreservation;

import java.io.Serializable;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class PointNumberReservation implements Comparable<PointNumberReservation>, Serializable {

    //~ Instance fields --------------------------------------------------------

    private String ablaufDatum;
    private String punktnummer;
    private String vermessungsstelle;
    private String intervallbeginn;
    private String uuid;

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getIntervallbeginn() {
        return intervallbeginn;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  intervallbeginn  DOCUMENT ME!
     */
    public void setIntervallbeginn(final String intervallbeginn) {
        this.intervallbeginn = intervallbeginn;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  uuid  DOCUMENT ME!
     */
    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getAblaufDatum() {
        return ablaufDatum;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  ablaufDatum  DOCUMENT ME!
     */
    public void setAblaufDatum(final String ablaufDatum) {
        this.ablaufDatum = ablaufDatum;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getPunktnummer() {
        return punktnummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  punktnummer  DOCUMENT ME!
     */
    public void setPunktnummer(final String punktnummer) {
        this.punktnummer = punktnummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getVermessungsstelle() {
        return vermessungsstelle;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  vermessungsstelle  DOCUMENT ME!
     */
    public void setVermessungsstelle(final String vermessungsstelle) {
        this.vermessungsstelle = vermessungsstelle;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getFeatureId() {
        return getUuid() + getIntervallbeginn().replaceAll("-", "").replaceAll(":", "");
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof PointNumberReservation) {
            final PointNumberReservation pnr = (PointNumberReservation)obj;
            return this.punktnummer.equals(pnr.getPunktnummer());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = (23 * hash) + ((this.punktnummer != null) ? this.punktnummer.hashCode() : 0);
        return hash;
    }

    @Override
    public int compareTo(final PointNumberReservation o) {
        return this.getPunktnummer().compareTo(o.getPunktnummer());
    }
}
