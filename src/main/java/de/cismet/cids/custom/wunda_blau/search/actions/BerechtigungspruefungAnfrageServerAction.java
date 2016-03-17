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

import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungHandler;

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
public class BerechtigungspruefungAnfrageServerAction implements UserAwareServerAction, MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungAnfrageServerAction.class);

    public static final String TASK_NAME = "berechtigungspruefungAnfrage";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ParameterType {

        //~ Enum constants -----------------------------------------------------

        ABGEHOLT, DATEINAME, BEGRUENDUNG, BERECHTIGUNGSGRUND, DOWNLOADINFO_JSON
    }

    //~ Instance fields --------------------------------------------------------

    private User user = null;
    private MetaService metaService = null;

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            String dateiname = null;
            String begruendung = null;
            String downloadInfo_json = null;
            String berechtigungsgrund = null;
            String abgeholt = null;
            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(ParameterType.DATEINAME.toString())) {
                        dateiname = (String)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.BEGRUENDUNG.toString())) {
                        begruendung = (String)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.BERECHTIGUNGSGRUND.toString())) {
                        berechtigungsgrund = (String)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.DOWNLOADINFO_JSON.toString())) {
                        downloadInfo_json = (String)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.ABGEHOLT.toString())) {
                        abgeholt = (String)sap.getValue();
                    }
                }
            }

            if (abgeholt != null) {
                BerechtigungspruefungHandler.getInstance().setMetaService(getMetaService());
                BerechtigungspruefungHandler.getInstance().closeAnfrage(user, abgeholt);
            } else {
                final byte[] data = (byte[])body;
                if ((body != null) && !(body instanceof byte[])) {
                    throw new IllegalArgumentException("body has to be a byte array");
                }

                if (downloadInfo_json == null) {
                    throw new IllegalArgumentException("aufruf enthält keine downloadinfo");
                }

                final BerechtigungspruefungDownloadInfo downloadInfo = BerechtigungspruefungHandler.extractDownloadInfo(
                        downloadInfo_json);

                BerechtigungspruefungHandler.getInstance().setMetaService(getMetaService());
                return BerechtigungspruefungHandler.getInstance()
                            .addNewAnfrage(
                                getUser(),
                                downloadInfo,
                                berechtigungsgrund,
                                begruendung,
                                dateiname,
                                data);
            }
        } catch (final Exception ex) {
            LOG.error("error while executing anfrage task", ex);
        }
        return null;
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
