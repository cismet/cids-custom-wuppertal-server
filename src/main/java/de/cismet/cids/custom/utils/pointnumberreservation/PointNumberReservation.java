/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.pointnumberreservation;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class PointNumberReservation {

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
}
