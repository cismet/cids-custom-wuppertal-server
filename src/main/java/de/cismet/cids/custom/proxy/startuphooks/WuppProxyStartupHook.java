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

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ProxyStartupHook.class)
public class WuppProxyStartupHook implements ProxyStartupHook {

    //~ Methods ----------------------------------------------------------------

    @Override
    public void proxyStarted() {
    }
}
