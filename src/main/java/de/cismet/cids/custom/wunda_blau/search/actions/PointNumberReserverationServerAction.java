/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.newuser.User;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservation;
import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservationRequest;
import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservationService;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;
import de.cismet.cids.server.connectioncontext.ServerConnectionContext;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class PointNumberReserverationServerAction implements UserAwareServerAction,
    MetaServiceStore,
    ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final String ANR_SEPERATOR = "_";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ACTION_TYPE {

        //~ Enum constants -----------------------------------------------------

        // PROLONG_RESERVATION should be renamed to DO_COMPLETION, because it is what it realy does.
        GET_ALL_RESERVATIONS, IS_ANTRAG_EXISTING, DO_RESERVATION, PROLONG_RESERVATION, GET_POINT_NUMBERS, DO_STORNO,
        DO_PROLONG
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        ACTION, PREFIX, AUFTRAG_NUMMER, NBZ, ANZAHL, STARTWERT, ON1, ON2, POINT_NUMBER, PROLONG_DATE
    }

    //~ Instance fields --------------------------------------------------------

    private MetaService metaService;
    private User user;

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Collection<String> getAllAntragsNummern() {
        final Collection<PointNumberReservationRequest> requests = PointNumberReservationService.instance()
                    .getAllBenAuftr(getProfilKennung());
        final ArrayList<String> antragsNummern = new ArrayList<String>();
        if (requests != null) {
            for (final PointNumberReservationRequest r : requests) {
                antragsNummern.add(r.getAntragsnummer());
            }
        }
        return antragsNummern;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   aPrefix  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Collection<String> getAllAntragsNummern(final String aPrefix) {
        final String anr = aPrefix + "*";
        final Collection<PointNumberReservationRequest> requests = PointNumberReservationService.instance()
                    .getAllBenAuftrWithWildCard(anr, getProfilKennung());
        final ArrayList<String> antragsNummern = new ArrayList<String>();
        if (requests != null) {
            for (final PointNumberReservationRequest r : requests) {
                antragsNummern.add(r.getAntragsnummer());
            }
        }
        return antragsNummern;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   aPrefix  DOCUMENT ME!
     * @param   aNummer  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isAntragsNummerAlreadyExisting(final String aPrefix, final String aNummer) {
        final String anr = aPrefix + ANR_SEPERATOR + aNummer;
        if (!isAuftragsNummerValid(anr)) {
            return false;
        }
        return PointNumberReservationService.instance().isAntragsNummerExisting(anr, getProfilKennung());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   aPrefix    DOCUMENT ME!
     * @param   aNummer    DOCUMENT ME!
     * @param   nbz        DOCUMENT ME!
     * @param   anzahl     DOCUMENT ME!
     * @param   startwert  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private PointNumberReservationRequest doReservierung(final String aPrefix,
            final String aNummer,
            final String nbz,
            final int anzahl,
            final int startwert) {
        final String anr = aPrefix + ANR_SEPERATOR + aNummer;
        if (!isAuftragsNummerValid(anr)) {
            return null;
        }

        return PointNumberReservationService.instance()
                    .doReservation(aPrefix, anr, nbz, anzahl, startwert, getProfilKennung());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   aPrefix  DOCUMENT ME!
     * @param   aNummer  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Collection<PointNumberReservation> getReserviertePunkte(final String aPrefix, final String aNummer) {
        final String anr = aPrefix + ANR_SEPERATOR + aNummer;
        final PointNumberReservationRequest result = PointNumberReservationService.instance()
                    .getAllBenAuftr(anr, getProfilKennung());
        if (result != null) {
            return result.getPointNumbers();
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   aPrefix  DOCUMENT ME!
     * @param   aNummer  DOCUMENT ME!
     * @param   nbz      DOCUMENT ME!
     * @param   on1      DOCUMENT ME!
     * @param   on2      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private PointNumberReservationRequest doStorno(final String aPrefix,
            final String aNummer,
            final String nbz,
            final int on1,
            final int on2) {
        final String anr = aPrefix + ANR_SEPERATOR + aNummer;
        if (!isAuftragsNummerValid(anr)) {
            return null;
        }

        return PointNumberReservationService.instance()
                    .releaseReservation(aPrefix, anr, nbz, on1, on2, getProfilKennung());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getProfilKennung() {
        String profilKennung = "WUNDA_RES";
        try {
            final String conf = ((DomainServerImpl)getMetaService()).getConfigAttr(
                    getUser(),
                    "custom.punktnummernreservierung.profilkennung",
                    getServerConnectionContext());
            if (conf != null) {
                profilKennung = conf;
            }
        } catch (RemoteException ex) {
        }
        return profilKennung;
    }
    /**
     * DOCUMENT ME!
     *
     * @param   aPrefix  DOCUMENT ME!
     * @param   aNummer  DOCUMENT ME!
     * @param   ps       on1 DOCUMENT ME!
     * @param   date     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private PointNumberReservationRequest doVerlaengern(final String aPrefix,
            final String aNummer,
            final Collection<Integer> ps,
            final Date date) {
        final String anr = aPrefix + ANR_SEPERATOR + aNummer;
        if (!isAuftragsNummerValid(anr)) {
            return null;
        }
        return PointNumberReservationService.instance().prolongReservation(aPrefix, anr, ps, date, getProfilKennung());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   requestId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isAuftragsNummerValid(final String requestId) {
        return (requestId.length() <= 50) && requestId.matches("[a-zA-Z0-9_-]*");
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        ACTION_TYPE method = null;
        String prefix = null;
        String auftragsNummer = null;
        String nbz = null;
        int anzahl = 0;
        int startwert = 0;
        int on1 = 0;
        int on2 = 0;
        Date prolongDate = null;
        final Collection<Integer> pointnumbers = new ArrayList<Integer>();
        for (final ServerActionParameter sap : params) {
            if (sap.getKey().equals(PARAMETER_TYPE.ACTION.toString())) {
                method = (ACTION_TYPE)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.AUFTRAG_NUMMER.toString())) {
                auftragsNummer = (String)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.PREFIX.toString())) {
                prefix = (String)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.NBZ.toString())) {
                nbz = (String)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.ANZAHL.toString())) {
                anzahl = (Integer)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.STARTWERT.toString())) {
                startwert = (Integer)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.ON1.toString())) {
                on1 = (Integer)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.ON2.toString())) {
                on2 = (Integer)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.POINT_NUMBER.toString())) {
                pointnumbers.add((Integer)sap.getValue());
            } else if (sap.getKey().equals(PARAMETER_TYPE.PROLONG_DATE.toString())) {
                prolongDate = (Date)sap.getValue();
            }
        }

        if (method == ACTION_TYPE.DO_RESERVATION) {
            // check if antragsNummer does not exists
            if ((prefix != null) && (auftragsNummer != null) && (nbz != null) && (anzahl > 0)) {
                if (!isAntragsNummerAlreadyExisting(prefix, auftragsNummer)) {
                    return doReservierung(prefix, auftragsNummer, nbz, anzahl, startwert);
                } else {
                    // ToDo: LOG the error...
                    throw new IllegalStateException("Antragsnummer " + prefix + ANR_SEPERATOR + auftragsNummer
                                + " existiert bereits");
                }
            }
        } else if (method == ACTION_TYPE.DO_STORNO) {
            if ((prefix != null) && (auftragsNummer != null) && (nbz != null)) {
                if ((on1 > 0) && (on2 > 0) && (on1 <= on2)) {
                    return doStorno(prefix, auftragsNummer, nbz, on1, on2);
                }
            }
        } else if (method == ACTION_TYPE.DO_PROLONG) {
            if ((prefix != null) && (auftragsNummer != null) && (prolongDate != null) && !pointnumbers.isEmpty()) {
                return doVerlaengern(prefix, auftragsNummer, pointnumbers, prolongDate);
            }
        } else if (method == ACTION_TYPE.PROLONG_RESERVATION) {
            // check if antragsNummer exists
            if ((prefix != null) && (auftragsNummer != null) && (nbz != null) && (anzahl > 0)) {
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
            if ((prefix != null) && (auftragsNummer != null)) {
                return getReserviertePunkte(prefix, auftragsNummer);
            }
        } else if (method == ACTION_TYPE.IS_ANTRAG_EXISTING) {
            if ((prefix != null) && (auftragsNummer != null)) {
                return isAntragsNummerAlreadyExisting(prefix, auftragsNummer);
            }
        }
        return null;
    }

    @Override
    public String getTaskName() {
        return "pointNumberReservation";
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }

    @Override
    public void setMetaService(final MetaService metaSevice) {
        this.metaService = metaSevice;
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }

    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return ServerConnectionContext.create(PointNumberReserverationServerAction.class.getSimpleName());
    }
}
