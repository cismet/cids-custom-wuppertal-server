/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.utils;

import lombok.Getter;
import lombok.Setter;

import java.util.Properties;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   srichter
 * @version  $Revision$, $Date$
 */
@Getter
@Setter
public class StaticProperties {

    //~ Instance fields --------------------------------------------------------

    private final String poiSignaturUrlPrefix;
    private final String poiSignaturUrlSuffix;
    private final String poiSignaturDefaultIcon;
    private final String fortfuehrungsnachweiseUrlPrefix;
    private final String albBaulastUrlPrefix;
    private final String albBaulastDocumentPath;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new StaticProperties object.
     *
     * @param  properties  DOCUMENT ME!
     */
    protected StaticProperties(final Properties properties) {
        poiSignaturUrlPrefix = properties.getProperty("poi_signatur_url_prefix");
        poiSignaturUrlSuffix = properties.getProperty("poi_signatur_url_suffix");
        poiSignaturDefaultIcon = properties.getProperty("poi_signatur_default_icon");
        fortfuehrungsnachweiseUrlPrefix = properties.getProperty("fortfuehrungsnachweise_url_prefix");
        albBaulastUrlPrefix = properties.getProperty("baulasten_dokumenten_url_prefix");
        albBaulastDocumentPath = properties.getProperty("baulasten_dokumenten_pfad");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static StaticProperties getInstance() {
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

        private static final StaticProperties INSTANCE;

        static {
            try {
                INSTANCE = new StaticProperties(ServerResourcesLoader.getInstance().loadProperties(
                            WundaBlauServerResources.URLCONFIG_PROPERTIES.getValue()));
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
