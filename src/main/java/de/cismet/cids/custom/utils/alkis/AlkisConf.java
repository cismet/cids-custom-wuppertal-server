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

import java.util.Properties;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class AlkisConf {

    //~ Instance fields --------------------------------------------------------

    public final String SERVER;
    public final String SERVICE;
    public final String USER;
    public final String PASSWORD;

    public final String CATALOG_SERVICE;
    public final String INFO_SERVICE;
    public final String SEARCH_SERVICE;
    public final String SRS_GEOM;
    public final String SRS_SERVICE;
    public final String MAP_CALL_STRING;
    public final double GEO_BUFFER;
    public final double GEO_BUFFER_MULTIPLIER;
    public final String EINZEL_NACHWEIS_SERVICE;
    public final String LISTEN_NACHWEIS_SERVICE;
    public final String LIEGENSCHAFTSKARTE_SERVICE;
    public final String NIVP_HOST;
    public final String NIVP_PREFIX;
    public final String APMAPS_HOST;
    public final String APMAPS_ETRS_HOST;
    public final String APMAPS_PREFIX;
    public final String VERMESSUNG_HOST_BILDER;
    public final String VERMESSUNG_HOST_GRENZNIEDERSCHRIFTEN;
    public final String LANDPARCEL_FEATURE_RENDERER_COLOR;

    public final String DEMOSERVICEURL;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlkisConf object.
     *
     * @param  serviceProperties  DOCUMENT ME!
     */
    public AlkisConf(final Properties serviceProperties) {
        SERVER = serviceProperties.getProperty("SERVER");
        SERVICE = serviceProperties.getProperty("SERVICE");
        USER = serviceProperties.getProperty("USER");
        PASSWORD = serviceProperties.getProperty("PASSWORD");

        DEMOSERVICEURL = serviceProperties.getProperty("DEMOSERVICEURL");

        CATALOG_SERVICE = serviceProperties.getProperty("CATALOG_SERVICE");
        INFO_SERVICE = serviceProperties.getProperty("INFO_SERVICE");
        SEARCH_SERVICE = serviceProperties.getProperty("SEARCH_SERVICE");

        EINZEL_NACHWEIS_SERVICE = SERVER + serviceProperties.getProperty("BUCH_NACHWEIS_SERVICE");
        LISTEN_NACHWEIS_SERVICE = SERVER + serviceProperties.getProperty("LISTEN_NACHWEIS_SERVICE");
        LIEGENSCHAFTSKARTE_SERVICE = SERVER + serviceProperties.getProperty("LIEGENSCHAFTSKARTE_SERVICE");

        SRS_GEOM = serviceProperties.getProperty("SRS_GEOM");
        SRS_SERVICE = serviceProperties.getProperty("SRS_SERVICE");
        MAP_CALL_STRING = serviceProperties.getProperty("MAP_CALL_STRING") + SRS_SERVICE;
        GEO_BUFFER = Double.parseDouble(serviceProperties.getProperty("GEO_BUFFER"));
        GEO_BUFFER_MULTIPLIER = Double.parseDouble(serviceProperties.getProperty("GEO_BUFFER_MULTIPLIER"));

        NIVP_HOST = serviceProperties.getProperty("NIVP_HOST");
        NIVP_PREFIX = serviceProperties.getProperty("NIVP_PREFIX");

        APMAPS_HOST = serviceProperties.getProperty("APMAPS_HOST");
        APMAPS_PREFIX = serviceProperties.getProperty("APMAPS_PREFIX");

        VERMESSUNG_HOST_BILDER = serviceProperties.getProperty("VERMESSUNG_HOST_BILDER");
        VERMESSUNG_HOST_GRENZNIEDERSCHRIFTEN = serviceProperties.getProperty(
                "VERMESSUNG_HOST_GRENZNIEDERSCHRIFTEN");
        APMAPS_ETRS_HOST = serviceProperties.getProperty("APMAPS_ETRS_HOST");

        LANDPARCEL_FEATURE_RENDERER_COLOR = serviceProperties.getProperty("LANDPARCEL_FEATURE_RENDERER_COLOR");
    }
}
