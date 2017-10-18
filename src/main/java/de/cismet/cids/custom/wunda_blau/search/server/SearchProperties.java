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
package de.cismet.cids.custom.wunda_blau.search.server;

import java.util.Properties;

import de.cismet.cids.custom.utils.WuppProxyServerResources;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;
import org.apache.log4j.Logger;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class SearchProperties extends Properties {
    private static final transient Logger LOG = Logger.getLogger(SearchProperties.class);
    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Property {

        //~ Enum constants -----------------------------------------------------

        /**
         * GEOS-Error prevention buffer for intersects operations.
         *
         * <p>Some geometries have overlapping edges which causes GEOS-errors while doing intersects operations.
         * Applying a small buffer over those geometries works as a workaround. This property is for adjusting this
         * buffer (and should ONLY BE USED for this purpose).</p>
         */
        INTERSECTS_BUFFER
    }

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SearchProperties object.
     */
    private SearchProperties() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static SearchProperties getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getIntersectsBuffer() {        
        final Object value = get(Property.INTERSECTS_BUFFER.name());
        return (value != null)?(String)value: "0.001";
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final SearchProperties INSTANCE = new SearchProperties();

        static {
            try {
                INSTANCE.load(ServerResourcesLoader.getInstance().loadStringReader(
                        WuppProxyServerResources.SEARCH_PROPERTIES.getValue()));
            } catch (final Exception ex) {
                LOG.warn("loading server resources failed. the searchproperties should only be used on the server", ex);
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
