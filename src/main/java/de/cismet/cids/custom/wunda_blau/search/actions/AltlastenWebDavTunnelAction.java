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

import de.cismet.cids.custom.utils.AltlastenWebDavProperties;
import de.cismet.cids.custom.utils.GrundwassermessstellenProperties;
import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.WebDavTunnelAction;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class AltlastenWebDavTunnelAction extends WebDavTunnelAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            AltlastenWebDavTunnelAction.class);
    public static final String TASK_NAME = "AltlastenWebDavTunnelAction";

    //~ Instance fields --------------------------------------------------------

    private final AltlastenWebDavProperties properties;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GrundwassermessstellenWebDavTunnelAction object.
     */
    public AltlastenWebDavTunnelAction() {
        AltlastenWebDavProperties properties = null;
        try {
            properties = new AltlastenWebDavProperties(ServerResourcesLoader.getInstance().loadProperties(
                        WundaBlauServerResources.ALBO_WEBDAV_PROPERTIES.getValue()));
        } catch (final Exception ex) {
            LOG.info("AltlastenWebDavTunnelAction could not load the properties", ex);
        }
        this.properties = properties;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    protected String getUsername() {
        return (properties != null) ? properties.getWebDavLogin() : null;
    }

    @Override
    protected String getPassword() {
        return (properties != null) ? properties.getWebDavPass() : null;
    }

    @Override
    protected String getWebdavPath() {
        return (properties != null) ? (properties.getWebDavHost() + properties.getWebDavPath()) : null;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
