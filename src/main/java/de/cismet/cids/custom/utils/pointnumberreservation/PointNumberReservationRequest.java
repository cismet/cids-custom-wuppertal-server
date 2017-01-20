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
    private boolean successfull;
    private String protokoll;
    private List<String> errorMessages;

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

    /**
     * DOCUMENT ME!
     *
     * @param  wasSuccessFull  DOCUMENT ME!
     */
    public void setSuccessful(final boolean wasSuccessFull) {
        this.successfull = wasSuccessFull;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  protokoll  DOCUMENT ME!
     */
    public void setProtokoll(final String protokoll) {
        this.protokoll = protokoll;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isSuccessfull() {
        return successfull;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getProtokoll() {
        return protokoll;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<String> getErrorMessages() {
        return errorMessages;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  errorMessages  DOCUMENT ME!
     */
    public void setErrorMessages(final List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }
}
