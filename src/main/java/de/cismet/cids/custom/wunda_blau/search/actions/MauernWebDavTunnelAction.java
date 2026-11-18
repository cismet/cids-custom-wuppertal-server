/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.actions;

import de.cismet.cids.custom.utils.MauernProperties;
import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.WebDavTunnelAction;

import de.cismet.cids.utils.serverresources.PropertiesServerResource;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * WebDav tunnel for the documents of the MauerDokumenteEditor. The webdav url and the credentials are read from the
 * mauern properties on the server, so the client does not need to know them.
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class MauernWebDavTunnelAction extends WebDavTunnelAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            MauernWebDavTunnelAction.class);
    public static final String TASK_NAME = "MauernWebDavTunnelAction";

    //~ Methods ----------------------------------------------------------------

    /**
     * Loads the mauern properties. The properties are only returned, if a webdav url is configured. Otherwise the
     * client could use the server credentials for any url.
     *
     * @return  the mauern properties or null, if they cannot be loaded or no webdav url is configured
     */
    private MauernProperties getProperties() {
        try {
            final MauernProperties properties = (MauernProperties)ServerResourcesLoader.getInstance()
                        .get((PropertiesServerResource)WundaBlauServerResources.MAUERN_PROPERTIES.getValue());

            if ((properties.getWebdavUrl() == null) || properties.getWebdavUrl().isEmpty()) {
                LOG.error("MauernWebDavTunnelAction: webdavUrl is not configured");
                return null;
            }

            return properties;
        } catch (final Exception ex) {
            LOG.error("MauernWebDavTunnelAction could not load the properties", ex);
            return null;
        }
    }

    @Override
    protected String getUsername() {
        final MauernProperties properties = getProperties();
        return (properties != null) ? properties.getWebdavUser() : null;
    }

    @Override
    protected String getPassword() {
        final MauernProperties properties = getProperties();
        return (properties != null) ? properties.getWebdavPassword() : null;
    }

    @Override
    protected String getWebdavPath() {
        final MauernProperties properties = getProperties();

        if (properties == null) {
            return null;
        }

        final String webdavUrl = properties.getWebdavUrl();

        // the path of the client will be appended. Without a trailing slash, the client could change the host
        return webdavUrl.endsWith("/") ? webdavUrl : (webdavUrl + "/");
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        WebDavTunnelPathValidator.checkRelativePaths(TASK_NAME, params);

        return super.execute(body, params);
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
