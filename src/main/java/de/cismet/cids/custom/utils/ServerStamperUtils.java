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
package de.cismet.cids.custom.utils;

import Sirius.server.middleware.interfaces.domainserver.ActionService;
import Sirius.server.newuser.User;

import java.io.StringReader;

import java.util.Properties;

import de.cismet.cids.server.actions.GetServerResourceServerAction;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class ServerStamperUtils extends StamperUtils {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ServerStamperUtils object.
     *
     * @param  stamperConf  DOCUMENT ME!
     */
    private ServerStamperUtils(final StamperConf stamperConf) {
        super(stamperConf);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static ServerStamperUtils getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user               DOCUMENT ME!
     * @param   as                 DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static StamperConf loadConfFromActionService(final User user,
            final ActionService as,
            final ConnectionContext connectionContext) throws Exception {
        final Properties properties = new Properties();
        properties.load(new StringReader(
                (String)as.executeTask(
                    user,
                    GetServerResourceServerAction.TASK_NAME,
                    WundaBlauServerResources.STAMPER_CONF_PROPERTIES.getValue(),
                    connectionContext)));
        return new StamperConf(properties);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static StamperConf loadConfFromServerResource() throws Exception {
        final StamperConf stamperConf = new StamperConf(ServerResourcesLoader.getInstance().loadProperties(
                    WundaBlauServerResources.STAMPER_CONF_PROPERTIES.getValue()));
        return stamperConf;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final ServerStamperUtils INSTANCE;

        static {
            try {
                INSTANCE = new ServerStamperUtils(loadConfFromServerResource());
            } catch (final Exception ex) {
                throw new RuntimeException("Exception while initializing ServerStamperUtils", ex);
            }
        }

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
