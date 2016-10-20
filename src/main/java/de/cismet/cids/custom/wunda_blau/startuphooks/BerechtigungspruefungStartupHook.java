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
import Sirius.server.newuser.UserServer;

import java.rmi.Naming;

import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungHandler;
import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungProperties;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = DomainServerStartupHook.class)
public class BerechtigungspruefungStartupHook implements DomainServerStartupHook {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungStartupHook.class);

    //~ Methods ----------------------------------------------------------------

    @Override
    public void domainServerStarted() {
        new Thread(new Runnable() {

                @Override
                public void run() {
                    DomainServerImpl metaService = null;
                    while (metaService == null) {
                        metaService = DomainServerImpl.getServerInstance();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                    }

                    try {
                        final Object userServer = Naming.lookup("rmi://localhost/userServer");
                        final User user = ((UserServer)userServer).getUser(
                                null,
                                null,
                                "WUNDA_BLAU",
                                BerechtigungspruefungProperties.getInstance().getCidsLogin(),
                                BerechtigungspruefungProperties.getInstance().getCidsPassword());
                        BerechtigungspruefungHandler.getInstance().setMetaService(metaService);
                        BerechtigungspruefungHandler.getInstance().sendMessagesForAllOpenFreigaben(user);
                        BerechtigungspruefungHandler.getInstance().sendMessagesForAllOpenAnfragen(user);
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
