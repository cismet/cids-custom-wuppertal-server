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
import java.io.IOException;
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
public class AlkisRestConf extends Properties {

    //~ Instance fields --------------------------------------------------------

    private final Crendetials creds;

    private final String configuration;
    private final String credentialsFile;
    private final String tokenApi;
    private final String aaaWebApi;
    private final Boolean newRestServiceUsed;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ServerAlkisRestConf object.
     *
     * @param   serviceProperties  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public AlkisRestConf(final Properties serviceProperties) throws IOException {
        configuration = serviceProperties.getProperty("CONFIGURATION");
        credentialsFile = serviceProperties.getProperty("CREDENTIALS_FILE");
        tokenApi = serviceProperties.getProperty("TOKEN_API");
        aaaWebApi = serviceProperties.getProperty("AAAWEB_API");
        newRestServiceUsed = serviceProperties.getProperty("NEW_REST_SERVICE_USED", "false").equalsIgnoreCase("true");

        final String crendentialsFile = getCredentialsFile();
        if (crendentialsFile != null) {
            final Properties credProperties = new Properties();
            credProperties.load(new FileInputStream(new File(crendentialsFile)));
            creds = new Crendetials(credProperties);
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
    public static AlkisRestConf getInstance() {
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
    public static AlkisRestConf loadFromDomainServer(final User user,
            final ActionService as,
            final ConnectionContext connectionContext) throws Exception {
        final Properties properties = new Properties();
        properties.load(new StringReader(
                (String)as.executeTask(
                    user,
                    GetServerResourceServerAction.TASK_NAME,
                    WundaBlauServerResources.ALKIS_REST_CONF.getValue(),
                    connectionContext)));
        return new AlkisRestConf(properties);
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final AlkisRestConf INSTANCE;

        static {
            try {
                INSTANCE = new AlkisRestConf(ServerResourcesLoader.getInstance().loadProperties(
                            WundaBlauServerResources.ALKIS_REST_CONF.getValue()));
            } catch (final Exception ex) {
                throw new RuntimeException("Exception while initializing ServerAlkisRestConf", ex);
            }
        }

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    public class Crendetials {

        //~ Instance fields ----------------------------------------------------

        private final String user;
        private final String password;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new AlkisConf object.
         *
         * @param  properties  DOCUMENT ME!
         */
        public Crendetials(final Properties properties) {
            user = properties.getProperty("USER");
            password = properties.getProperty("PASSWORD");
        }
    }
}
