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
package de.cismet.cids.custom.utils.properties;

import lombok.Getter;

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.wunda_blau.search.actions.PotenzialflaecheReportServerAction.PfMapConfiguration;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class PotenzialflaechenMapfactoryProperties {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            PotenzialflaechenMapfactoryProperties.class);

    private static final String DEFAULT_MAP_URL = "";
    private static final String DEFAULT_SRS = "EPSG:25832";
    private static final int DEFAULT_MAP_DPI = 300;
    private static final int DEFAULT_BUFFER = 50;
    private static final int DEFAULT_MAP_WIDTH = 300;
    private static final int DEFAULT_MAP_HEIGHT = 200;
    private static final double DEFAULT_HOME_X1 = 6.7d;
    private static final double DEFAULT_HOME_Y1 = 49.1d;
    private static final double DEFAULT_HOME_X2 = 7.1d;
    private static final double DEFAULT_HOME_Y2 = 49.33d;

    //~ Instance fields --------------------------------------------------------

    private final Properties properties;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionsProperties object.
     *
     * @param  properties  DOCUMENT ME!
     */
    private PotenzialflaechenMapfactoryProperties(final Properties properties) {
        this.properties = properties;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   type  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getMapUrl(final PfMapConfiguration.Type type) {
        try {
            return getProperties().getProperty(String.format("mapUrl_%s", type.name()), DEFAULT_MAP_URL);
        } catch (final Exception ex) {
            LOG.info(String.format("returning %s as default MapURL for %s", DEFAULT_MAP_URL, type), ex);
            return DEFAULT_MAP_URL;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   type  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Integer getMapDPI(final PfMapConfiguration.Type type) {
        try {
            return Integer.parseInt(getProperties().getProperty(String.format("mapDPI_%s", type.name())));
        } catch (final Exception ex) {
            LOG.info(String.format("returning %d as default MapDPI for %s", DEFAULT_MAP_DPI, type), ex);
            return DEFAULT_MAP_DPI;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   type  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Integer getWidth(final PfMapConfiguration.Type type) {
        try {
            return Integer.parseInt(getProperties().getProperty(String.format("mapWidth_%s", type.name())));
        } catch (final Exception ex) {
            LOG.info(String.format("returning %d as default MapWidth for %s", DEFAULT_MAP_WIDTH, type), ex);
            return DEFAULT_MAP_WIDTH;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   type  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Integer getHeight(final PfMapConfiguration.Type type) {
        try {
            return Integer.parseInt(getProperties().getProperty(String.format("mapHeight_%s", type.name())));
        } catch (final Exception ex) {
            LOG.info(String.format("returning %d as default MapHeight for %s", DEFAULT_MAP_HEIGHT, type), ex);
            return DEFAULT_MAP_HEIGHT;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   type  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Integer getBuffer(final PfMapConfiguration.Type type) {
        try {
            return Integer.parseInt(getProperties().getProperty(String.format("buffer_%s", type.name())));
        } catch (final Exception ex) {
            LOG.info(String.format("returning %d as default Buffer for %s", DEFAULT_BUFFER, type), ex);
            return DEFAULT_BUFFER;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Double getHomeX1() {
        try {
            return Double.parseDouble(getProperties().getProperty("homeX1"));
        } catch (final Exception ex) {
            LOG.info(String.format("returning %f as default HomeX1", DEFAULT_HOME_X1), ex);
            return DEFAULT_HOME_X1;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Double getHomeY1() {
        try {
            return Double.parseDouble(getProperties().getProperty("homeY1"));
        } catch (final Exception ex) {
            LOG.info(String.format("returning %f as default HomeY1", DEFAULT_HOME_Y1), ex);
            return DEFAULT_HOME_Y1;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Double getHomeX2() {
        try {
            return Double.parseDouble(getProperties().getProperty("homeX2"));
        } catch (final Exception ex) {
            LOG.info(String.format("returning %f as default HomeX2", DEFAULT_HOME_X2), ex);
            return DEFAULT_HOME_X2;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Double getHomeY2() {
        try {
            return Double.parseDouble(getProperties().getProperty("homeY2"));
        } catch (final Exception ex) {
            LOG.info(String.format("returning %f as default HomeY2", DEFAULT_HOME_Y2), ex);
            return DEFAULT_HOME_Y2;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getSrs() {
        try {
            return getProperties().getProperty("Srs", DEFAULT_SRS);
        } catch (final Exception ex) {
            LOG.info(String.format("returning %s as default SRS", DEFAULT_SRS), ex);
            return DEFAULT_SRS;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static PotenzialflaechenMapfactoryProperties getInstance() {
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

        private static final PotenzialflaechenMapfactoryProperties INSTANCE;

        static {
            PotenzialflaechenMapfactoryProperties instance = null;
            try {
                final Properties properties = ServerResourcesLoader.getInstance()
                            .loadProperties(WundaBlauServerResources.POTENZIALFLAECHEN_MAPFACTORY_PROPERTIES
                                .getValue());
                instance = new PotenzialflaechenMapfactoryProperties(properties);
            } catch (final Exception ex) {
                LOG.error(ex, ex);
            }
            INSTANCE = instance;
        }

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
