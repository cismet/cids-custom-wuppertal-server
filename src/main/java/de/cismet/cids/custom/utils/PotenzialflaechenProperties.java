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

import lombok.Getter;

import de.cismet.cids.utils.serverresources.DefaultServerResourcePropertiesHandler;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class PotenzialflaechenProperties extends DefaultServerResourcePropertiesHandler {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            PotenzialflaechenProperties.class);

    private static final String PROP__FILE_CACHE_DIRECTORY = "fileCacheDirectory";
    private static final String PROP__MAP_FACTORY = "mapFactory";
    private static final String PROP__SUBREPORT_DIR = "subreportDir";
    private static final String DEFAULT_SRS = "EPSG:25832";

    private static final double DEFAULT_HOME_X1 = 6.7d;
    private static final double DEFAULT_HOME_Y1 = 49.1d;
    private static final double DEFAULT_HOME_X2 = 7.1d;
    private static final double DEFAULT_HOME_Y2 = 49.33d;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PotenzialflaechenProperties object.
     */
    public PotenzialflaechenProperties() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   name  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getReportFile(final String name) {
        try {
            return getProperties().getProperty(String.format("report_%s", name));
        } catch (final Exception ex) {
            LOG.info(String.format("property for report_%s not found", name), ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getPictureCacheDirectory() {
        return getProperties().getProperty(PROP__FILE_CACHE_DIRECTORY, null);
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
    public String getMapFactory() {
        try {
            return getProperties().getProperty(PROP__MAP_FACTORY, null);
        } catch (final Exception ex) {
            LOG.info(String.format("Property %s not set", PROP__MAP_FACTORY), ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getSubreportDir() {
        try {
            return getProperties().getProperty(PROP__SUBREPORT_DIR, null);
        } catch (final Exception ex) {
            LOG.info(String.format("Property %s not set", PROP__SUBREPORT_DIR), ex);
            return null;
        }
    }
}
