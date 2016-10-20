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

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.utils.serverresources.CachedServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class ServerAlkisConf extends AlkisConf {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ServerAlkisConf object.
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private ServerAlkisConf() throws Exception {
        super(CachedServerResourcesLoader.getInstance().getPropertiesResource(
                WundaBlauServerResources.ALKIS_CONF.getValue()));
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
                INSTANCE = new ServerAlkisConf();
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
