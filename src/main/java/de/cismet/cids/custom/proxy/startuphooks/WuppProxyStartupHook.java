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
package de.cismet.cids.custom.proxy.startuphooks;

import Sirius.server.middleware.impls.proxy.ProxyStartupHook;
import de.cismet.cids.custom.utils.WuppProxyServerResources;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;
import org.apache.log4j.Logger;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ProxyStartupHook.class)
public class WuppProxyStartupHook implements ProxyStartupHook {

    private static final Logger LOG = Logger.getLogger(WuppProxyStartupHook.class.getName());

    
    //~ Methods ----------------------------------------------------------------

    @Override
    public void proxyStarted() {
        loadAllServerResources();
    }
    
    public void loadAllServerResources() {
        boolean error = false;
        for (final WuppProxyServerResources wuppServerResources : WuppProxyServerResources.values()) {
            try {
                ServerResourcesLoader.getInstance().load(wuppServerResources.getValue());
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
