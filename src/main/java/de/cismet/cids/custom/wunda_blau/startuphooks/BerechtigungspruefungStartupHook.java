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

import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungHandler;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionsBestellungBerechtigungspruefungHandler;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = DomainServerStartupHook.class)
public class BerechtigungspruefungStartupHook extends AbstractWundaBlauStartupHook {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungStartupHook.class);

    //~ Methods ----------------------------------------------------------------

    @Override
    public void domainServerStarted() {
        new Thread(new Runnable() {

                @Override
                public void run() {
                    final DomainServerImpl metaService = waitForMetaService();

                    final BerechtigungspruefungHandler handler = BerechtigungspruefungHandler.getInstance();
                    handler.initWithConnectionContext(getConnectionContext());
                    handler.setMetaService(metaService);

                    final FormSolutionsBestellungBerechtigungspruefungHandler fsbbh =
                        FormSolutionsBestellungBerechtigungspruefungHandler.getInstance();
                    fsbbh.initWithConnectionContext(getConnectionContext());
                    fsbbh.setMetaService(metaService);

                    handler.sendMessagesForAllOpenFreigaben();
                    handler.sendMessagesForAllOpenAnfragen();
                    handler.deleteOldDateianhangFiles();
                }
            }).start();
    }
}
