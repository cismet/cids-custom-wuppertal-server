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
package de.cismet.cids.custom.wunda_blau.startuphooks;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.DomainServerStartupHook;
import Sirius.server.newuser.User;
import Sirius.server.newuser.UserGroup;

import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungHandler;

import de.cismet.tools.PropertyReader;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = DomainServerStartupHook.class)
public class BerechtigungspruerungStartupHook implements DomainServerStartupHook {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruerungStartupHook.class);

    private static final String PROPERTIES =
        "/de/cismet/cids/custom/berechtigungspruefung/berechtigungspruefung.properties";

    public static final Integer CIDS_USERID;
    public static final Integer CIDS_GROUPID;

    static {
        Integer cidsUserId = null;
        Integer cidsGroupId = null;

        try {
            final PropertyReader serviceProperties = new PropertyReader(PROPERTIES);

            cidsUserId = Integer.parseInt(serviceProperties.getProperty("CIDS_USERID"));
            cidsGroupId = Integer.parseInt(serviceProperties.getProperty("CIDS_GROUPID"));
        } catch (final Exception ex) {
            LOG.error("error while loading properties", ex);
        }

        CIDS_USERID = cidsUserId;
        CIDS_GROUPID = cidsGroupId;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void domainServerStarted() {
        new Thread(new Runnable() {

                @Override
                public void run() {
                    while (DomainServerImpl.getServerInstance() == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                    }

                    try {
                        final User user = new User(
                                CIDS_USERID,
                                null,
                                getDomain(),
                                new UserGroup(CIDS_GROUPID, null, getDomain()));
                        BerechtigungspruefungHandler.getInstance().setMetaService(DomainServerImpl.getServerInstance());
                        BerechtigungspruefungHandler.getInstance().sendMessagesForAllOpenDownloads(user);
                        BerechtigungspruefungHandler.getInstance().sendMessagesForAllOpenPruefungen(user);
                    } catch (final Exception ex) {
                        LOG.warn("Error while initializing the BerechtigungspruefungHandler !", ex);
                    }
                }
            }).start();
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}
