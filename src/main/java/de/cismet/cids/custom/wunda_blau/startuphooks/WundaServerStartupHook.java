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

import Sirius.server.middleware.interfaces.domainserver.DomainServerStartupHook;

import org.apache.log4j.Logger;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = DomainServerStartupHook.class)
public class WundaServerStartupHook implements DomainServerStartupHook {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(WundaServerStartupHook.class.getName());

    //~ Methods ----------------------------------------------------------------

    @Override
    public void domainServerStarted() {
        loadAllServerResources();
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }

    /**
     * DOCUMENT ME!
     */
    public void loadAllServerResources() {
        boolean error = false;
        for (final WundaBlauServerResources resource : WundaBlauServerResources.values()) {
            try {
                resource.loadServerResources();
            } catch (final Exception ex) {
                LOG.warn("Exception while loading resource from the resources base path.", ex);
                error = true;
            }
        }

        if (error) {
            LOG.error("!!! CAUTION !!! Not all server resources could be loaded !");
        }
    }
}
