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

    //~ Methods ----------------------------------------------------------------

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
    public String getPunktnummern() {
        return punktnummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  punktnummer  DOCUMENT ME!
     */
    public void setPunktnummern(final String punktnummer) {
        this.punktnummer = punktnummer;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof PointNumberReservation) {
            final PointNumberReservation pnr = (PointNumberReservation)obj;
            return this.punktnummer.equals(pnr.getPunktnummern());
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
        return this.getPunktnummern().compareTo(o.getPunktnummern());
    }
}
