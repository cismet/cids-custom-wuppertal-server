/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.pointnumberreservation;

import org.apache.log4j.Logger;

import java.io.Serializable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenTaskException;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class PointNumberReservationRequest implements Serializable {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(PointNumberReservationRequest.class);
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private static final DateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd");

    //~ Instance fields --------------------------------------------------------

    private String antragsnummer;
    private List<PointNumberReservation> pointNumbers = new ArrayList<>();
    private boolean successfull;
    private String protokoll;
    private List<String> errorMessages;
    private String rawResult;

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getRawResult() {
        return rawResult;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  rawResult  DOCUMENT ME!
     */
    public void setRawResult(final String rawResult) {
        this.rawResult = rawResult;
    }

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

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String createTxtProtokoll() {
        final StringBuilder contentBuilder = new StringBuilder();

        boolean isFreigabeMode = false;
        if (isSuccessfull() && (getPointNumbers() != null)) {
            for (final PointNumberReservation pnr : getPointNumbers()) {
                if ((pnr.getAblaufDatum() == null) || pnr.getAblaufDatum().isEmpty()) {
                    isFreigabeMode = true;
                    break;
                }
            }
        }

        String header = "Antragsnummer: " + getAntragsnummer() + " erstellt am: ";
        final GregorianCalendar cal = new GregorianCalendar();
        header += DATE_FORMAT.format(cal.getTime());
        header += " Anzahl ";
        if (isFreigabeMode) {
            header += "freigegebener";
        } else {
            header += "reservierter";
        }

        header += " Punktnummern: " + getPointNumbers().size();
        contentBuilder.append(header);
        contentBuilder.append("\r\n");
        if (isFreigabeMode) {
            contentBuilder.append("freigegebene Punktnummern");
        } else {
            contentBuilder.append("reservierte Punktnummern (gültig bis)");
        }
        contentBuilder.append("\r\n");
        contentBuilder.append("\r\n");

        for (final PointNumberReservation pnr : getPointNumbers()) {
            contentBuilder.append(pnr.getPunktnummer());
            if (!isFreigabeMode) {
                contentBuilder.append(" (");
                try {
                    contentBuilder.append(DATE_FORMAT.format(DATE_PARSER.parse(pnr.getAblaufDatum())));
                } catch (ParseException ex) {
                    LOG.info(
                        "Could not parse the expiration date of a reservation. Using the string representation return by server");
                    contentBuilder.append(pnr.getAblaufDatum());
                }
                contentBuilder.append(")");
            }
            contentBuilder.append("\r\n");
        }

        return contentBuilder.toString();
    }
}
