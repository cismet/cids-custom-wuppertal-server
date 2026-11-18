/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.actions;

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.WebDavTunnelAction;

import de.cismet.cids.utils.PasswordUtils;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * WebDav tunnel for the images of the VzkatStandortEditor. The webdav url and the credentials are read from the vzkat
 * properties on the server, so the client does not need to know them. The client sends only the file name.
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class VzkatWebDavTunnelAction extends WebDavTunnelAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            VzkatWebDavTunnelAction.class);
    public static final String TASK_NAME = "VzkatWebDavTunnelAction";

    private static final String PROP__WEBDAV_UPLOAD_URL = "WEBDAV_UPLOAD_URL";
    private static final String PROP__WEBDAV_UPLOAD_USERNAME = "WEBDAV_UPLOAD_USERNAME";
    private static final String PROP__WEBDAV_UPLOAD_PASSWORD = "WEBDAV_UPLOAD_PASSWORD";

    //~ Methods ----------------------------------------------------------------

    /**
     * Loads the vzkat properties. The properties are only returned, if a webdav url is configured. Otherwise the client
     * could use the server credentials for any url.
     *
     * @return  the vzkat properties or null, if they cannot be loaded or no webdav url is configured
     */
    private Properties getProperties() {
        try {
            final Properties properties = ServerResourcesLoader.getInstance()
                        .loadProperties(WundaBlauServerResources.VZKAT_PROPERTIES.getValue());
            final String webdavUrl = properties.getProperty(PROP__WEBDAV_UPLOAD_URL);

            if ((webdavUrl == null) || webdavUrl.trim().isEmpty()) {
                LOG.error("VzkatWebDavTunnelAction: " + PROP__WEBDAV_UPLOAD_URL + " is not configured");
                return null;
            }

            return properties;
        } catch (final Exception ex) {
            LOG.error("VzkatWebDavTunnelAction could not load the properties", ex);
            return null;
        }
    }

    @Override
    protected String getUsername() {
        final Properties properties = getProperties();
        return (properties != null) ? properties.getProperty(PROP__WEBDAV_UPLOAD_USERNAME) : null;
    }

    @Override
    protected String getPassword() {
        final Properties properties = getProperties();
        return (properties != null) ? PasswordUtils.decrypt(properties.getProperty(PROP__WEBDAV_UPLOAD_PASSWORD))
                                    : null;
    }

    @Override
    protected String getWebdavPath() {
        final Properties properties = getProperties();

        if (properties == null) {
            return null;
        }

        final String webdavUrl = properties.getProperty(PROP__WEBDAV_UPLOAD_URL).trim();

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
