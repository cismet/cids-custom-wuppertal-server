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

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = DomainServerStartupHook.class)
public class BerechtigungspruefungStartupHook implements DomainServerStartupHook, ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungStartupHook.class);

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

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
                        final BerechtigungspruefungHandler handler = BerechtigungspruefungHandler.getInstance();
                        handler.initWithConnectionContext(getConnectionContext());
                        handler.setMetaService(metaService);
                        handler.sendMessagesForAllOpenFreigaben(user);
                        handler.sendMessagesForAllOpenAnfragen(user);
                        handler.deleteOldDateianhangFiles(user);
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

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
