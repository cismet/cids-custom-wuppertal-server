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

import de.cismet.cids.custom.utils.motd.MotdRetriever;
import de.cismet.cids.custom.utils.motd.MotdRetrieverListener;
import de.cismet.cids.custom.utils.motd.MotdRetrieverListenerEvent;

import de.cismet.cids.server.messages.CidsServerMessageManagerImpl;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = DomainServerStartupHook.class)
public class MotdWundaStartupHook implements DomainServerStartupHook {

    //~ Static fields/initializers ---------------------------------------------

    public static final String MOTD_MESSAGE_MOTD = "motd";

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

                    MotdRetriever.getInstance().addMotdRetrieverListener(new MotdRetrieverListener() {

                            @Override
                            public void motdChanged(final MotdRetrieverListenerEvent event) {
                                final String motd = event.getMotd();
                                CidsServerMessageManagerImpl.getInstance().publishMessage(MOTD_MESSAGE_MOTD, motd);
                            }
                        });
                    MotdRetriever.getInstance().start();
                }
            }).start();
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}
