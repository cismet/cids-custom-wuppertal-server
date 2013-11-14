/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.pointnumberreservation;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class PointNumberReservationRequest implements Serializable {

    //~ Instance fields --------------------------------------------------------

    private String antragsnummer;
    private List<PointNumberReservation> pointNumbers = new ArrayList<PointNumberReservation>();

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
    public List<PointNumberReservation> getPointNumbers() {
        return pointNumbers;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  pointNumbers  DOCUMENT ME!
     */
    public void setPointNumbers(final List<PointNumberReservation> pointNumbers) {
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
