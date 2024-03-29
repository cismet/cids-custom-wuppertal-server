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

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class PointNumberReservationServerAction implements UserAwareServerAction,
    MetaServiceStore,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final String ANR_SEPERATOR = "_";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Action {

        //~ Enum constants -----------------------------------------------------

        // PROLONG_RESERVATION should be renamed to DO_COMPLETION, because it is what it realy does.
        GET_ALL_RESERVATIONS, IS_ANTRAG_EXISTING, DO_RESERVATION, DO_ADDITION, GET_POINT_NUMBERS, DO_STORNO,
        DO_PROLONGATION
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        ACTION, PREFIX, AUFTRAG_NUMMER, NBZ, ANZAHL, STARTWERT, ON1, ON2, POINT_NUMBER, PROLONG_DATE
    }

    //~ Instance fields --------------------------------------------------------

    private MetaService metaService;
    private User user;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Collection<String> getAllAntragsNummern() {
        final Collection<PointNumberReservationRequest> requests = PointNumberReservationService.instance()
                    .getAllBenAuftr(getProfilKennung());
        final ArrayList<String> antragsNummern = new ArrayList<>();
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
    private Object doReservierung(final String aPrefix,
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
                    .getBenAuftr(anr, getProfilKennung());
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
                    .doReleaseReservation(aPrefix, anr, nbz, on1, on2, getProfilKennung());
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
                    getConnectionContext());
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
    private Object doVerlaengern(final String aPrefix,
            final String aNummer,
            final Collection<Long> ps,
            final Date date) {
        final String anr = aPrefix + ANR_SEPERATOR + aNummer;
        if (!isAuftragsNummerValid(anr)) {
            return null;
        }

        return PointNumberReservationService.instance()
                    .doProlongReservation(aPrefix, anr, ps, date, getProfilKennung());
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
        Action method = null;
        String prefix = null;
        String auftragsNummer = null;
        String nbz = null;
        int anzahl = 0;
        int startwert = 0;
        int on1 = 0;
        int on2 = 0;
        Date prolongDate = null;
        final Collection<Long> pointnumbers = new ArrayList<>();
        for (final ServerActionParameter sap : params) {
            if (sap.getKey().equals(Parameter.ACTION.toString())) {
                method = (Action)sap.getValue();
            } else if (sap.getKey().equals(Parameter.AUFTRAG_NUMMER.toString())) {
                auftragsNummer = (String)sap.getValue();
            } else if (sap.getKey().equals(Parameter.PREFIX.toString())) {
                prefix = (String)sap.getValue();
            } else if (sap.getKey().equals(Parameter.NBZ.toString())) {
                nbz = (String)sap.getValue();
            } else if (sap.getKey().equals(Parameter.ANZAHL.toString())) {
                anzahl = (Integer)sap.getValue();
            } else if (sap.getKey().equals(Parameter.STARTWERT.toString())) {
                startwert = (Integer)sap.getValue();
            } else if (sap.getKey().equals(Parameter.ON1.toString())) {
                on1 = (Integer)sap.getValue();
            } else if (sap.getKey().equals(Parameter.ON2.toString())) {
                on2 = (Integer)sap.getValue();
            } else if (sap.getKey().equals(Parameter.POINT_NUMBER.toString())) {
                pointnumbers.add((Long)sap.getValue());
            } else if (sap.getKey().equals(Parameter.PROLONG_DATE.toString())) {
                prolongDate = (Date)sap.getValue();
            }
        }

        if (method != null) {
            switch (method) {
                case DO_RESERVATION: {
                    // check if antragsNummer does not exists
                    if ((prefix != null) && (auftragsNummer != null) && (nbz != null) && (anzahl > 0)) {
                        if (!isAntragsNummerAlreadyExisting(prefix, auftragsNummer)) {
                            return doReservierung(prefix, auftragsNummer, nbz, anzahl, startwert);
                        } else {
                            // ToDo: LOG the error...
                            throw new IllegalStateException("Antragsnummer " + prefix + ANR_SEPERATOR + auftragsNummer
                                        + " existiert bereits");
                        }
                    } else {
                        return null;
                    }
                }
                case DO_STORNO: {
                    if ((prefix != null) && (auftragsNummer != null) && (nbz != null) && (on1 > 0) && (on2 > 0)
                                && (on1 <= on2)) {
                        return doStorno(prefix, auftragsNummer, nbz, on1, on2);
                    } else {
                        return null;
                    }
                }
                case DO_PROLONGATION: {
                    if ((prefix != null) && (auftragsNummer != null) && (prolongDate != null)
                                && !pointnumbers.isEmpty()) {
                        return doVerlaengern(prefix, auftragsNummer, pointnumbers, prolongDate);
                    } else {
                        return null;
                    }
                }
                case DO_ADDITION: {
                    // check if antragsNummer exists
                    if ((prefix != null) && (auftragsNummer != null) && (nbz != null) && (anzahl > 0)
                                && isAntragsNummerAlreadyExisting(prefix, auftragsNummer)) {
                        return doReservierung(prefix, auftragsNummer, nbz, anzahl, startwert);
                    } else {
                        return null;
                    }
                }
                case GET_ALL_RESERVATIONS: {
                    if (prefix == null) {
                        return getAllAntragsNummern();
                    } else {
                        return getAllAntragsNummern(prefix);
                    }
                }
                case GET_POINT_NUMBERS: {
                    if ((prefix != null) && (auftragsNummer != null)) {
                        return getReserviertePunkte(prefix, auftragsNummer);
                    } else {
                        return null;
                    }
                }
                case IS_ANTRAG_EXISTING: {
                    if ((prefix != null) && (auftragsNummer != null)) {
                        return isAntragsNummerAlreadyExisting(prefix, auftragsNummer);
                    } else {
                        return null;
                    }
                }
                default: {
                    return null;
                }
            }
        } else {
            return null;
        }
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
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
