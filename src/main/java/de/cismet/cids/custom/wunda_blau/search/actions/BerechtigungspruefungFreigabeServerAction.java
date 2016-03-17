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

import java.util.Date;

import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungHandler;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class BerechtigungspruefungFreigabeServerAction implements UserAwareServerAction, MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungFreigabeServerAction.class);

    public static final String TASK_NAME = "berechtigungspruefungFreigabe";
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

            final boolean pruefstatus;
            if (MODUS_FREIGABE.equals(modus)) {
                pruefstatus = true;
            } else if (MODUS_STORNO.equals(modus)) {
                pruefstatus = false;
            } else {
                throw new Exception("weder Freigabe noch Storno");
            }

            BerechtigungspruefungHandler.getInstance().setMetaService(metaService);

            synchronized (this) {
                final String schluessel = (String)body;
                final CidsBean pruefungBean = BerechtigungspruefungHandler.getInstance()
                            .loadAnfrageBean(getUser(), schluessel);

                if (pruefungBean.getProperty("pruefstatus") != null) {
                    return false;
                }
                final String userKey = (String)pruefungBean.getProperty("benutzer");

                pruefungBean.setProperty("pruefstatus", pruefstatus);
                pruefungBean.setProperty("pruefer", getUser().getName());
                pruefungBean.setProperty("pruefkommentar", begruendung);
                pruefungBean.setProperty("pruefung_timestamp", new Timestamp(new Date().getTime()));
                getMetaService().updateMetaObject(getUser(), pruefungBean.getMetaObject());

                BerechtigungspruefungHandler.getInstance().sendMessagesForAllOpenFreigaben(userKey, getUser());
            }
        } catch (final Exception ex) {
            LOG.error("error while executing freigabe action", ex);
        }
        return true;
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
}
