/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.actions;

import java.util.Locale;

import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.WebDavTunnelAction;

/**
 * Validates the paths, that a client sends to a webdav tunnel action, which appends them to a configured webdav
 * directory and uses the server credentials.
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
final class WebDavTunnelPathValidator {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            WebDavTunnelPathValidator.class);

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new WebDavTunnelPathValidator object.
     */
    private WebDavTunnelPathValidator() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Checks all paths of the given parameters and throws an exception, if a path could leave the configured webdav
     * directory.
     *
     * @param   actionName  the name of the action (for logging)
     * @param   params      the parameters of the action
     *
     * @throws  RuntimeException  if a path is not allowed
     */
    static void checkRelativePaths(final String actionName, final ServerActionParameter... params) {
        for (final ServerActionParameter sap : params) {
            final String key = sap.getKey();

            if (key.equals(WebDavTunnelAction.PARAMETER_TYPE.HEAD.toString())
                        || key.equals(WebDavTunnelAction.PARAMETER_TYPE.GET.toString())
                        || key.equals(WebDavTunnelAction.PARAMETER_TYPE.PUT.toString())
                        || key.equals(WebDavTunnelAction.PARAMETER_TYPE.DELETE.toString())) {
                final String path = (String)sap.getValue();

                if (!isRelativePathAllowed(path)) {
                    LOG.error(actionName + ": path not allowed: " + path);
                    throw new RuntimeException("The path is not within the configured webdav directory");
                }
            }
        }
    }

    /**
     * Checks, whether the given path stays within the directory, it will be appended to.
     *
     * @param   path  the path relative to the webdav directory
     *
     * @return  true, if the path cannot leave the directory
     */
    static boolean isRelativePathAllowed(final String path) {
        if (path == null) {
            return false;
        }

        final String lowerPath = path.toLowerCase(Locale.ROOT);

        // encoded dots, slashes or backslashes could be decoded by the webdav server
        if (path.contains("\\") || lowerPath.contains("%2e") || lowerPath.contains("%2f")
                    || lowerPath.contains("%5c")) {
            return false;
        }

        for (final String segment : path.split("/", -1)) {
            if (segment.equals("..")) {
                return false;
            }
        }

        return true;
    }
}
