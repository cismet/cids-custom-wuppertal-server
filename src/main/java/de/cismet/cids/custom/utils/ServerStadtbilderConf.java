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

import java.util.Properties;

import de.cismet.cids.custom.utils.stadtbilder.StadtbilderConf;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class ServerStadtbilderConf extends StadtbilderConf {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ServerAlkisConf object.
     *
     * @param   properties  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private ServerStadtbilderConf(final Properties properties) throws Exception {
        super(properties);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static ServerStadtbilderConf getInstance() {
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

        private static final ServerStadtbilderConf INSTANCE;

        static {
            try {
                INSTANCE = new ServerStadtbilderConf(ServerResourcesLoader.getInstance().loadProperties(
                            WundaBlauServerResources.STADTBILDER_CONF_PROPERTIES.getValue()));
            } catch (final Exception ex) {
                throw new RuntimeException("Exception while initializing ServerStadtbilderConf", ex);
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
