/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.newuser.User;

import java.sql.Timestamp;

import java.util.Arrays;
import java.util.Date;

import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungBillingDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungHandler;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;
import de.cismet.cids.server.connectioncontext.ServerConnectionContext;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class BerechtigungspruefungFreigabeServerAction implements UserAwareServerAction,
    MetaServiceStore,
    ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungFreigabeServerAction.class);

    public static final String TASK_NAME = "berechtigungspruefungFreigabe";
    public static final String MODUS_PRUEFUNG = "PRUEFUNG";
    public static final String MODUS_FREIGABE = "FREIGABE";
    public static final String MODUS_STORNO = "STORNO";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ParameterType {

        //~ Enum constants -----------------------------------------------------

        MODUS, KOMMENTAR
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ReturnType {

        //~ Enum constants -----------------------------------------------------

        OK, PENDING, ALREADY
    }

    //~ Instance fields --------------------------------------------------------

    private User user = null;
    private MetaService metaService = null;

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            String begruendung = null;
            String modus = null;
            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(ParameterType.KOMMENTAR.toString())) {
                        begruendung = (String)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.MODUS.toString())) {
                        modus = (String)sap.getValue();
                    }
                }
            }

            final boolean pruefungsAbschluss;
            final Boolean pruefstatus;
            if (MODUS_PRUEFUNG.equals(modus)) {
                pruefstatus = null;
                pruefungsAbschluss = false;
            } else if (MODUS_FREIGABE.equals(modus)) {
                pruefstatus = true;
                pruefungsAbschluss = true;
            } else if (MODUS_STORNO.equals(modus)) {
                pruefstatus = false;
                pruefungsAbschluss = true;
            } else {
                throw new Exception("weder Freigabe noch Storno");
            }

            BerechtigungspruefungHandler.getInstance().setMetaService(metaService);

            synchronized (this) {
                final String pruefer = getUser().getName();
                final String schluessel = (String)body;
                final CidsBean pruefungBean = BerechtigungspruefungHandler.getInstance()
                            .loadAnfrageBean(getUser(), schluessel);

                final BerechtigungspruefungDownloadInfo downloadInfo = BerechtigungspruefungHandler.extractDownloadInfo(
                        (String)pruefungBean.getProperty("downloadinfo_json"));

                if (!Boolean.TRUE.equals(pruefungBean.getProperty("abgeholt"))
                            && (pruefungBean.getProperty("pruefstatus") != null)) {
                    return ReturnType.ALREADY;
//                } else if (pruefungsAbschluss && (pruefungBean.getProperty("pruefer") != null)
//                            && !pruefer.equals(pruefungBean.getProperty("pruefer"))) {
//                    return ReturnType.PENDING;
                }
                final String userKey = (String)pruefungBean.getProperty("benutzer");

                final Timestamp now = new Timestamp(new Date().getTime());
                pruefungBean.setProperty("abgeholt", null);
                pruefungBean.setProperty("pruefer", pruefer);
                pruefungBean.setProperty("pruefstatus", pruefstatus);
                if (pruefungsAbschluss) {
                    pruefungBean.setProperty("freigabe_timestamp", now);
                    pruefungBean.setProperty("pruefkommentar", begruendung);
                } else {
                    pruefungBean.setProperty("pruefung_timestamp", now);
                }

                getMetaService().updateMetaObject(
                    getUser(),
                    pruefungBean.getMetaObject(),
                    getServerConnectionContext());

                if (pruefungsAbschluss) {
                    if (!pruefstatus && (downloadInfo instanceof BerechtigungspruefungBillingDownloadInfo)) { // storno
                        final BerechtigungspruefungBillingDownloadInfo billingDownloadinfo =
                            (BerechtigungspruefungBillingDownloadInfo)downloadInfo;
                        final Integer billingId = billingDownloadinfo.getBillingId();
                        if (billingId != null) {
                            final CidsBean billingBean = BerechtigungspruefungHandler.getInstance()
                                        .loadBillingBean(user, billingId);
                            final CidsBean billingStornogrundBean = BerechtigungspruefungHandler.getInstance()
                                        .loadBillingStornogrundBean(user);

                            try {
                                billingBean.setProperty("storniert", Boolean.TRUE);
                                billingBean.setProperty("stornogrund", billingStornogrundBean);
                                billingBean.setProperty("storniert_durch", getUser().toString());

                                getMetaService().updateMetaObject(
                                    getUser(),
                                    billingBean.getMetaObject(),
                                    getServerConnectionContext());
                            } catch (Exception ex) {
                                LOG.error("Error while setting 'storniert' of billing", ex);
                            }
                        }
                    }

                    BerechtigungspruefungHandler.getInstance()
                            .sendFreigabeMessage(userKey, Arrays.asList(pruefungBean));
                } else {
                    BerechtigungspruefungHandler.getInstance().sendProcessingMessage(schluessel, getUser());
                }
            }
        } catch (final Exception ex) {
            LOG.error("error while executing freigabe action", ex);
        }
        return ReturnType.OK;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
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
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }

    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return ServerConnectionContext.create(getClass().getSimpleName());
    }
}
