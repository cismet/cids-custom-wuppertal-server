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

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.vermessungsunterlagen.GrundwassermessstellenProperties;

import de.cismet.cids.server.actions.ServerAction;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class GrundwassermessstellenWebDavTunnelAction extends WebDavTunnelAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            GrundwassermessstellenWebDavTunnelAction.class);
    private static GrundwassermessstellenProperties PROPERTIES;

    public static final String TASK_NAME = "GrundwassermessstellenWebDavTunnelAction";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GrundwassermessstellenWebDavTunnelAction object.
     */
    public GrundwassermessstellenWebDavTunnelAction() {
        super(
            getProperties().getWebDavLogin(),
            getProperties().getWebDavPass(),
            getProperties().getWebDavHost()
                    + getProperties().getWebDavPath());
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static GrundwassermessstellenProperties getProperties() {
        if (PROPERTIES == null) {
            try {
                PROPERTIES = new GrundwassermessstellenProperties(ServerResourcesLoader.getInstance().loadProperties(
                            WundaBlauServerResources.GRUNDWASSERMESSSTELLEN_PROPERTIES.getValue()));
            } catch (final Exception ex) {
                LOG.error("GrundwassermessstellenWebDavTunnelAction could not load the properties", ex);
            }
        }
        return PROPERTIES;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
