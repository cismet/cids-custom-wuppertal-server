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
    private static final String PROP__REPORTS_DIR = "reportsDirectory";

    private static final String PROP__MAP_FACTORY = "mapFactory";

    private static final String PROP__WEBDAV_URL = "webdavUrl";
    private static final String PROP__WEBDAV_USER = "webdavUser";
    private static final String PROP__WEBDAV_PASSWORD = "webdavPassword";

    private static final String PROP__SECRES_KEY = "secresKey";
    private static final String PROP__SECRES_API = "secresApi";

    private static final String DEFAULT__MAP_FACTORY = "de.cismet.cids.custom.reports.wunda_blau.PfMapFactory";
    private static final String DEFAULT__SRS = "EPSG:25832";
    private static final double DEFAULT__HOME_X1 = 6.7d;
    private static final double DEFAULT__HOME_Y1 = 49.1d;
    private static final double DEFAULT__HOME_X2 = 7.1d;
    private static final double DEFAULT__HOME_Y2 = 49.33d;

    private static final String DEFAULT__SECRES_KEY = "potenzialflaecheReport";
    private static final String DEFAULT__SECRES_API = "http://belis-api.cismet.de";
    private static final String DEFAULT__REPORTS_DIR = "/tmp";

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
        final String homeX1 = getProperties().getProperty("homeX1");
        try {
            return (homeX1 != null) ? Double.parseDouble(homeX1) : DEFAULT__HOME_X1;
        } catch (final Exception ex) {
            LOG.info(String.format("error parsing '%s', returning %f as default homeX1", homeX1, DEFAULT__HOME_X1), ex);
            return DEFAULT__HOME_X1;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Double getHomeY1() {
        final String homeY1 = getProperties().getProperty("homeY1");
        try {
            return (homeY1 != null) ? Double.parseDouble(homeY1) : DEFAULT__HOME_Y1;
        } catch (final Exception ex) {
            LOG.info(String.format("error parsing '%s', returning %f as default homeY1", homeY1, DEFAULT__HOME_Y1), ex);
            return DEFAULT__HOME_Y1;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Double getHomeX2() {
        final String homeX2 = getProperties().getProperty("homeX2");
        try {
            return (homeX2 != null) ? Double.parseDouble(homeX2) : DEFAULT__HOME_X2;
        } catch (final Exception ex) {
            LOG.info(String.format("error parsing '%s', returning %f as default homeX2", homeX2, DEFAULT__HOME_X2), ex);
            return DEFAULT__HOME_X2;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Double getHomeY2() {
        final String homeY2 = getProperties().getProperty("homeY2");
        try {
            return (homeY2 != null) ? Double.parseDouble(homeY2) : DEFAULT__HOME_Y2;
        } catch (final Exception ex) {
            LOG.info(String.format("error parsing '%s', returning %f as default homeY2", homeY2, DEFAULT__HOME_Y2), ex);
            return DEFAULT__HOME_Y2;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getSrs() {
        try {
            return getProperties().getProperty("Srs", DEFAULT__SRS);
        } catch (final Exception ex) {
            LOG.info(String.format("returning %s as default SRS", DEFAULT__SRS), ex);
            return DEFAULT__SRS;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getMapFactory() {
        try {
            return getProperties().getProperty(PROP__MAP_FACTORY, DEFAULT__MAP_FACTORY);
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
    public String getSecresKey() {
        try {
            return getProperties().getProperty(PROP__SECRES_KEY, DEFAULT__SECRES_KEY);
        } catch (final Exception ex) {
            LOG.info(String.format("Property %s not set", PROP__SECRES_KEY), ex);
            return DEFAULT__SECRES_KEY;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getSecresApi() {
        try {
            return getProperties().getProperty(PROP__SECRES_API, DEFAULT__SECRES_API);
        } catch (final Exception ex) {
            LOG.info(String.format("Property %s not set", PROP__SECRES_API), ex);
            return DEFAULT__SECRES_API;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getWebdavUrl() {
        try {
            return getProperties().getProperty(PROP__WEBDAV_URL, null);
        } catch (final Exception ex) {
            LOG.info(String.format("Property %s not set", PROP__WEBDAV_URL), ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getWebdavUser() {
        try {
            return getProperties().getProperty(PROP__WEBDAV_USER, null);
        } catch (final Exception ex) {
            LOG.info(String.format("Property %s not set", PROP__WEBDAV_USER), ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getWebdavPassword() {
        try {
            return getProperties().getProperty(PROP__WEBDAV_PASSWORD, null);
        } catch (final Exception ex) {
            LOG.info(String.format("Property %s not set", PROP__WEBDAV_PASSWORD), ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getReportsDirectory() {
        try {
            return getProperties().getProperty(PROP__REPORTS_DIR, DEFAULT__REPORTS_DIR);
        } catch (final Exception ex) {
            LOG.info(String.format("Property %s not set", PROP__REPORTS_DIR), ex);
            return DEFAULT__REPORTS_DIR;
        }
    }
}
