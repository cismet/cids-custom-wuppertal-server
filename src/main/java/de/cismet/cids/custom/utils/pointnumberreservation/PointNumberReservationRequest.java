/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.pointnumberreservation;

import java.util.ArrayList;
import java.util.Collection;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class PointNumberReservationRequest {

    //~ Instance fields --------------------------------------------------------

    private String antragsnummer;
    private Collection<PointNumberReservation> pointNumbers = new ArrayList<PointNumberReservation>();

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getAntragsnummer() {
        return antragsnummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  antragsnummer  DOCUMENT ME!
     */
    public void setAntragsnummer(final String antragsnummer) {
        this.antragsnummer = antragsnummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<PointNumberReservation> getPointNumbers() {
        return pointNumbers;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  pointNumbers  DOCUMENT ME!
     */
    public void setPointNumbers(final Collection<PointNumberReservation> pointNumbers) {
        this.pointNumbers = pointNumbers;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  pnr  DOCUMENT ME!
     */
    public void addPointNumberReservation(final PointNumberReservation pnr) {
        pointNumbers.add(pnr);
    }
}
