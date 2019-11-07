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

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.wunda_blau.search.actions.orbit.OrbitStacTools;
import de.cismet.cids.custom.wunda_blau.search.actions.orbit.StacEntry;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   hell
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class GetOrbitAuthAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(StamperServerAction.class);

    public static final String TASK_NAME = "getOrbitAuth";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        STAC, STAC_OPTIONS, IP,
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... saps) {
        String stac = null;
        String stacOptions = null;
        String ipAddress = null;

        for (final ServerActionParameter sap : saps) {
            if (sap.getKey().equals(PARAMETER_TYPE.STAC.toString())) {
                stac = (String)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.STAC_OPTIONS.toString())) {
                stacOptions = (String)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.IP.toString())) {
                ipAddress = (String)sap.getValue();
            }
        }

        final StacEntry stacEntry = OrbitStacTools.getInstance().getEntry(stac);

        if (stacEntry != null) {
            final long ts = System.currentTimeMillis();
            long offset = 0;
            if ((stacEntry.getIpAddress() != null) && stacEntry.getIpAddress().equals(ipAddress)) {
                // 24h
                offset = 24 * 60 * 60 * 1000;
            } else {
                // 10h
                offset = 10 * 60 * 60 * 1000;
            }

            if ((stacEntry.getTimestamp() + offset) > ts) {
                try {
                    String ret = ServerResourcesLoader.getInstance()
                                .loadText(WundaBlauServerResources.ORBIT_AUTH_JSON.getValue());
                    ret = ret.replaceAll("###socketChannelId###", stacEntry.getSocketChannelId());
                    return ret;
                } catch (Exception ex) {
                    LOG.error("Error during GetOrbitAuthAction", ex);
                }
            } else {
                OrbitStacTools.getInstance().removeStac(stac);
            }
        }
        return "";
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
