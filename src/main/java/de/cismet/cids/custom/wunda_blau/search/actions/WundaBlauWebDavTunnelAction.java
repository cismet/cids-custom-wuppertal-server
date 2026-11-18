/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.actions;

import java.net.URI;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.WebDavTunnelAction;

import de.cismet.cids.utils.PasswordUtils;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * WebDav tunnel for the general webdav directories of WUNDA_BLAU (e.g. Treppen, Baulasten, Vermessungsrisse). The
 * credentials are read from the server resource /webdav/webdav_conf.properties, so the client does not need to know
 * them. The client sends the complete url of the file. Only urls within one of the directories, that are configured
 * with a property starting with "url_", are allowed. Otherwise the client could use the server credentials for any url.
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class WundaBlauWebDavTunnelAction extends WebDavTunnelAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            WundaBlauWebDavTunnelAction.class);
    public static final String TASK_NAME = "WundaBlauWebDavTunnelAction";

    private static final String PROP__USER = "user";
    private static final String PROP__PASSWORD = "password";
    private static final String PROP__URL_PREFIX = "url_";

    //~ Methods ----------------------------------------------------------------

    /**
     * Loads the webdav properties.
     *
     * @return  the webdav properties or null, if they cannot be loaded
     */
    private Properties getProperties() {
        try {
            return ServerResourcesLoader.getInstance()
                        .loadProperties(WundaBlauServerResources.WUNDA_WEBDAV_PROPERTIES.getValue());
        } catch (final Exception ex) {
            LOG.error("WundaBlauWebDavTunnelAction could not load the properties", ex);
            return null;
        }
    }

    @Override
    protected String getUsername() {
        final Properties properties = getProperties();
        return (properties != null) ? properties.getProperty(PROP__USER) : null;
    }

    @Override
    protected String getPassword() {
        final Properties properties = getProperties();
        return (properties != null) ? PasswordUtils.decrypt(properties.getProperty(PROP__PASSWORD)) : null;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final List<URI> allowedDirectories = getAllowedDirectories();

        for (final ServerActionParameter sap : params) {
            final String key = sap.getKey();

            if (key.equals(PARAMETER_TYPE.HEAD.toString()) || key.equals(PARAMETER_TYPE.GET.toString())
                        || key.equals(PARAMETER_TYPE.PUT.toString())
                        || key.equals(PARAMETER_TYPE.DELETE.toString())) {
                final String url = (String)sap.getValue();

                if (!isAllowed(url, allowedDirectories)) {
                    LOG.error("WundaBlauWebDavTunnelAction: url not allowed: " + url);
                    throw new RuntimeException("The url is not within the configured webdav directories");
                }
            }
        }

        return super.execute(body, params);
    }

    /**
     * Determines the directories, that can be used with this action. These are the values of all properties, which
     * start with "url_".
     *
     * @return  the allowed directories with a trailing slash
     */
    private List<URI> getAllowedDirectories() {
        final List<URI> directories = new ArrayList<>();
        final Properties properties = getProperties();

        if (properties != null) {
            for (final String name : properties.stringPropertyNames()) {
                if (name.startsWith(PROP__URL_PREFIX)) {
                    final String value = properties.getProperty(name).trim();

                    try {
                        directories.add(new URI(value.endsWith("/") ? value : (value + "/")).normalize());
                    } catch (final Exception ex) {
                        LOG.error("WundaBlauWebDavTunnelAction: invalid url in property " + name, ex);
                    }
                }
            }
        }

        return directories;
    }

    /**
     * Checks, whether the given url is within one of the given directories.
     *
     * @param   url          the url to check
     * @param   directories  the allowed directories
     *
     * @return  true, if the url is within one of the given directories
     */
    private static boolean isAllowed(final String url, final List<URI> directories) {
        if (url == null) {
            return false;
        }

        final URI uri;
        try {
            uri = new URI(url).normalize();
        } catch (final Exception ex) {
            LOG.warn("WundaBlauWebDavTunnelAction: invalid url " + url, ex);
            return false;
        }

        final String path = uri.getRawPath();

        // user info could redirect the request to another host and encoded dots or slashes could be used by the
        // webdav server to leave the directory
        if ((uri.getRawUserInfo() != null) || (uri.getScheme() == null) || (uri.getHost() == null)
                    || (path == null)
                    || path.contains("/../")
                    || path.endsWith("/..")
                    || path.contains("\\")) {
            return false;
        }

        final String lowerPath = path.toLowerCase(Locale.ROOT);

        if (lowerPath.contains("%2e") || lowerPath.contains("%2f") || lowerPath.contains("%5c")) {
            return false;
        }

        for (final URI directory : directories) {
            if (uri.getScheme().equalsIgnoreCase(directory.getScheme())
                        && uri.getHost().equalsIgnoreCase(directory.getHost())
                        && (uri.getPort() == directory.getPort())
                        && (directory.getRawPath() != null)
                        && path.startsWith(directory.getRawPath())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
