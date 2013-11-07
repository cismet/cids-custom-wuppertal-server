/**
 * *************************************************
 *
 * cismet GmbH, Saarbruecken, Germany
 * 
* ... and it just works.
 * 
***************************************************
 */
package de.cismet.cids.custom.wunda_blau.search.actions;

import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservation;
import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservationRequest;
import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservationService;
import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import java.util.ArrayList;
import java.util.Collection;

/**
 * DOCUMENT ME!
 *
 * @author daniel
 * @version $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class PointNumberReserverationServerAction implements ServerAction {

    private static final String ANR_SEPERATOR = "_";
    private static final PointNumberReservationService pnrService = PointNumberReservationService.instance();

    /**
     * DOCUMENT ME!
     *
     * @version $Revision$, $Date$
     */
    public enum ACTION_TYPE {

        //~ Enum constants -----------------------------------------------------
        GET_ALL_RESERVATIONS, IS_ANTRAG_EXISTING, DO_RESERVATION, EXTEND_RESERVATION, GET_POINT_NUMBERS, DO_STORNO
    }

    /**
     * DOCUMENT ME!
     *
     * @version $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------
        ACTION, PREFIX, AUFTRAG_NUMMER, NBZ, ANZAHL, STARTWERT, ON1, ON2
    }

    private Collection<String> getAllAntragsNummern() {
        final Collection<PointNumberReservationRequest> requests = pnrService.getAllBenAuftr();
        final ArrayList<String> antragsNummern = new ArrayList<String>();
        if (requests != null) {
            for (PointNumberReservationRequest r : requests) {
                antragsNummern.add(r.getAntragsnummer());
            }
        }
        return antragsNummern;
    }

    private Collection<String> getAllAntragsNummern(final String aPrefix) {
        final String anr = aPrefix + "*";
        final Collection<PointNumberReservationRequest> requests = pnrService.getAllBenAuftrWithWildCard(anr);
        final ArrayList<String> antragsNummern = new ArrayList<String>();
        if (requests != null) {
            for (PointNumberReservationRequest r : requests) {
                antragsNummern.add(r.getAntragsnummer());
            }
        }
        return antragsNummern;
    }

    private boolean isAntragsNummerAlreadyExisting(final String aPrefix, final String aNummer) {
        final String anr = aPrefix + ANR_SEPERATOR + aNummer;
        if (!isAuftragsNummerValid(anr)) {
            return false;
        }
        return pnrService.isAntragsNummerExisting(anr);
    }

    private PointNumberReservationRequest doReservierung(final String aPrefix, final String aNummer, final String nbz, final int anzahl, final int startwert) {
        final String anr = aPrefix + ANR_SEPERATOR + aNummer;
        if (!isAuftragsNummerValid(anr)) {
            return null;
        }

        return pnrService.doReservation(anr, nbz, anzahl, startwert);
    }

    private Collection<PointNumberReservation> getReserviertePunkte(String aPrefix, String aNummer) {
        final String anr = aPrefix + ANR_SEPERATOR + aNummer;
        return null;
    }

    private PointNumberReservationRequest doStorno(String aPrefix, String aNummer, String nbz, int on1, int on2) {
        final String anr = aPrefix + ANR_SEPERATOR + aNummer;
        if (!isAuftragsNummerValid(anr)) {
            return null;
        }
        return pnrService.releaseReservation(anr, nbz, on2, on2);
    }

    private boolean isAuftragsNummerValid(final String requestId) {
        return (requestId.length() <= 50) && requestId.matches("[a-zA-Z0-9_-]*");
    }

    @Override
    public Object execute(Object body, ServerActionParameter... params) {
        ACTION_TYPE method = null;
        String prefix = null;
        String auftragsNummer = null;
        String nbz = null;
        int anzahl = 0;
        int startwert = 0;
        int on1 = 0;
        int on2 = 0;
        for (final ServerActionParameter sap : params) {
            if (sap.getKey().equals(PARAMETER_TYPE.ACTION.toString())) {
                method = (ACTION_TYPE) sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.AUFTRAG_NUMMER.toString())) {
                auftragsNummer = (String) sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.PREFIX.toString())) {
                prefix = (String) sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.NBZ.toString())) {
                nbz = (String) sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.ANZAHL.toString())) {
                anzahl = (Integer) sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.STARTWERT.toString())) {
                startwert = (Integer) sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.ON1.toString())) {
                on1 = (Integer) sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.ON2.toString())) {
                on2 = (Integer) sap.getValue();
            }
        }

        if (method == ACTION_TYPE.DO_RESERVATION) {
            //check if antragsNummer does not exists
            if (prefix != null && auftragsNummer != null && nbz != null && anzahl > 0) {
                if (!isAntragsNummerAlreadyExisting(prefix, auftragsNummer)) {
                    return doReservierung(prefix, auftragsNummer, nbz, anzahl, startwert);
                }
            }
        } else if (method == ACTION_TYPE.DO_STORNO) {
            if (prefix != null && auftragsNummer != null && nbz != null) {
                if (on1 > 0 && on2 > 0 && on1 <= on2) {
                    return doStorno(prefix, auftragsNummer, nbz, on1, on2);
                }
            }
        } else if (method == ACTION_TYPE.EXTEND_RESERVATION) {
            //check if antragsNummer exists
            if (prefix != null && auftragsNummer != null && nbz != null && anzahl > 0) {
                if (isAntragsNummerAlreadyExisting(prefix, auftragsNummer)) {
                    return doReservierung(prefix, auftragsNummer, nbz, anzahl, startwert);
                }
            }

        } else if (method == ACTION_TYPE.GET_ALL_RESERVATIONS) {
            if (prefix == null) {
                return getAllAntragsNummern();
            } else {
                return getAllAntragsNummern(prefix);
            }
        } else if (method == ACTION_TYPE.GET_POINT_NUMBERS) {
            if (prefix != null && auftragsNummer != null) {
                return getReserviertePunkte(prefix, nbz);
            }

        } else if (method == ACTION_TYPE.IS_ANTRAG_EXISTING) {
            if (prefix != null && auftragsNummer != null) {
                return isAntragsNummerAlreadyExisting(prefix, nbz);
            }
        }
        return null;
    }

    @Override
    public String getTaskName() {
        return "pointNumberReservation";
    }

}
