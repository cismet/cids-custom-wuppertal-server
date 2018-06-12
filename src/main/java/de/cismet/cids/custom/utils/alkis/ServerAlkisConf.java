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
package de.cismet.cids.custom.utils.alkis;

import Sirius.server.middleware.interfaces.domainserver.ActionService;
import Sirius.server.newuser.User;

import lombok.Getter;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.server.actions.GetServerResourceServerAction;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class ServerAlkisConf extends AlkisConf {

    //~ Instance fields --------------------------------------------------------

    private final AlkisCreds creds;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ServerAlkisConf object.
     *
     * @param   properties  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private ServerAlkisConf(final Properties properties) throws Exception {
        super(properties);

        final String crendentialsFile = getCredentialsFile();
        if (crendentialsFile != null) {
            final Properties credProperties = new Properties();
            credProperties.load(new FileInputStream(new File(crendentialsFile)));
            creds = new AlkisCreds(credProperties);
        } else {
            creds = null;
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static ServerAlkisConf getInstance() {
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
    public static ServerAlkisConf loadFromDomainServer(final User user,
            final ActionService as,
            final ConnectionContext connectionContext) throws Exception {
        final Properties properties = new Properties();
        properties.load(new StringReader(
                (String)as.executeTask(
                    user,
                    GetServerResourceServerAction.TASK_NAME,
                    WundaBlauServerResources.ALKIS_CONF.getValue(),
                    connectionContext)));
        return new ServerAlkisConf(properties);
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final ServerAlkisConf INSTANCE;

        static {
            try {
                INSTANCE = new ServerAlkisConf(ServerResourcesLoader.getInstance().loadProperties(
                            WundaBlauServerResources.ALKIS_CONF.getValue()));
            } catch (final Exception ex) {
                throw new RuntimeException("Exception while initializing ServerAlkisConf", ex);
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
