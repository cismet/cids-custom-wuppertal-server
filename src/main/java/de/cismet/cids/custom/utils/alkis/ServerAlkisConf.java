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

import lombok.Getter;

import java.io.File;
import java.io.FileInputStream;

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

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
