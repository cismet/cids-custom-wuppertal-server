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

import java.util.Properties;

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
public abstract class MotdStartupHook extends AbstractWundaBlauStartupHook {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            MotdStartupHook.class);

    // Title of the day
    public static final String MOTD_MESSAGE_TOTD = "totd";
    public static final String MOTD_MESSAGE_TOTD_EXTERN = "totd_extern";

    // Message of the day
    public static final String MOTD_MESSAGE_MOTD = "motd";
    public static final String MOTD_MESSAGE_MOTD_EXTERN = "motd_extern";

    //~ Methods ----------------------------------------------------------------

    @Override
    public void domainServerStarted() {
        new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        waitForMetaService();
                        final MotdRetriever retriever = new MotdRetriever(getProperties());
                        if (retriever.init(getDomain())) {
                            retriever.addMotdRetrieverListener(new MotdRetrieverListener() {

                                    @Override
                                    public void totdChanged(final MotdRetrieverListenerEvent event) {
                                        if (event.isExtern()) {
                                            CidsServerMessageManagerImpl.getInstance()
                                                    .publishMessage(
                                                        MOTD_MESSAGE_TOTD_EXTERN,
                                                        event.getContent(),
                                                        true,
                                                        getConnectionContext());
                                        } else {
                                            CidsServerMessageManagerImpl.getInstance()
                                                    .publishMessage(
                                                        MOTD_MESSAGE_TOTD,
                                                        event.getContent(),
                                                        true,
                                                        getConnectionContext());
                                        }
                                    }

                                    @Override
                                    public void motdChanged(final MotdRetrieverListenerEvent event) {
                                        if (event.isExtern()) {
                                            CidsServerMessageManagerImpl.getInstance()
                                                    .publishMessage(
                                                        MOTD_MESSAGE_MOTD_EXTERN,
                                                        event.getContent(),
                                                        false,
                                                        getConnectionContext());
                                        } else {
                                            CidsServerMessageManagerImpl.getInstance()
                                                    .publishMessage(
                                                        MOTD_MESSAGE_MOTD,
                                                        event.getContent(),
                                                        false,
                                                        getConnectionContext());
                                        }
                                    }
                                });
                            retriever.start();
                        }
                    } catch (final Exception ex) {
                        LOG.warn("Error while initializing the MotdRetriever !", ex);
                    }
                }
            }).start();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public abstract Properties getProperties() throws Exception;
    @Override
    public abstract String getDomain();
}
