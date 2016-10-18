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

import de.cismet.cids.utils.serverresources.CachedServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class WundaBlauServerResourcesPreloader {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ServerResourcesPreloader object.
     */
    private WundaBlauServerResourcesPreloader() {
        final CachedServerResourcesLoader loader = CachedServerResourcesLoader.getInstance();
        for (final WundaBlauServerResources resource : WundaBlauServerResources.values()) {
            final String resourceValue = resource.getValue();
            switch (resource.getType()) {
                case JASPER_REPORT: {
                    loader.loadJasperReportResource(resourceValue);
                }
                break;
                case TEXT: {
                    loader.loadTextResource(resourceValue);
                }
                break;
                case TRUETYPE_FONT: {
                    loader.loadTruetypeFontResource(resourceValue);
                }
                break;
            }
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static WundaBlauServerResourcesPreloader getInstance() {
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

        private static final WundaBlauServerResourcesPreloader INSTANCE = new WundaBlauServerResourcesPreloader();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
