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
import java.util.Timer;
import java.util.TimerTask;

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

    private String poiSignaturUrlPrefix;
    private String poiSignaturUrlSuffix;
    private String poiSignaturDefaultIcon;
    private String fortfuehrungsnachweiseUrlPrefix;
    private String albBaulastUrlPrefix;
    private String albBaulastDocumentPath;

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

        final Timer t = new Timer();
        t.schedule(new TimerTask() {

                @Override
                public void run() {
                    refresh(null);
                }
            }, 60000, 60000);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param  properties  DOCUMENT ME!
     */
    protected synchronized void refresh(Properties properties) {
        try {
            if (properties == null) {
                properties = ServerResourcesLoader.getInstance()
                            .loadProperties(WundaBlauServerResources.URLCONFIG_PROPERTIES.getValue());
            }

            if (properties != null) {
                if ((properties.getProperty("fortfuehrungsnachweise_url_prefix") != null)
                            && !fortfuehrungsnachweiseUrlPrefix.equals(
                                properties.getProperty("fortfuehrungsnachweise_url_prefix"))) {
                    poiSignaturUrlPrefix = properties.getProperty("poi_signatur_url_prefix");
                    poiSignaturUrlSuffix = properties.getProperty("poi_signatur_url_suffix");
                    poiSignaturDefaultIcon = properties.getProperty("poi_signatur_default_icon");
                    fortfuehrungsnachweiseUrlPrefix = properties.getProperty("fortfuehrungsnachweise_url_prefix");
                    albBaulastUrlPrefix = properties.getProperty("baulasten_dokumenten_url_prefix");
                    albBaulastDocumentPath = properties.getProperty("baulasten_dokumenten_pfad");
                }
            }
        } catch (Exception e) {
            // nothing to do
        }
    }

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
