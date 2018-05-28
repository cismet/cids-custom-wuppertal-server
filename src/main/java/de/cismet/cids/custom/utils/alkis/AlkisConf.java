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

import lombok.Getter;

import java.util.Properties;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class AlkisConf {

    //~ Instance fields --------------------------------------------------------

    private final String server;
    private final String service;
    private final String user;
    private final String password;

    private final String catalogService;
    private final String infoService;
    private final String searchService;
    private final String srsGeom;
    private final String srsService;
    private final String mapCallString;
    private final double geoBuffer;
    private final double geoBufferMultiplier;
    private final String einzelNachweisService;
    private final String listenNachweisService;
    private final String LiegenschaftskarteService;
    private final String nivpHost;
    private final String nivpPrefix;
    private final String apmapsHost;
    private final String apmapsEtrsHost;
    private final String apmapsPrefix;
    private final String vermessungHostBilder;
    private final String vermessungHostGrenzniederschriften;
    private final String landparcelFeatureRendererColor;

    private final String demoServiceUrl;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlkisConf object.
     *
     * @param  serviceProperties  DOCUMENT ME!
     */
    public AlkisConf(final Properties serviceProperties) {
        server = serviceProperties.getProperty("SERVER");
        service = serviceProperties.getProperty("SERVICE");
        user = serviceProperties.getProperty("USER");
        password = serviceProperties.getProperty("PASSWORD");

        demoServiceUrl = serviceProperties.getProperty("DEMOSERVICEURL");

        catalogService = serviceProperties.getProperty("CATALOG_SERVICE");
        infoService = serviceProperties.getProperty("INFO_SERVICE");
        searchService = serviceProperties.getProperty("SEARCH_SERVICE");

        einzelNachweisService = server + serviceProperties.getProperty("BUCH_NACHWEIS_SERVICE");
        listenNachweisService = server + serviceProperties.getProperty("LISTEN_NACHWEIS_SERVICE");
        LiegenschaftskarteService = server + serviceProperties.getProperty("LIEGENSCHAFTSKARTE_SERVICE");

        srsGeom = serviceProperties.getProperty("SRS_GEOM");
        srsService = serviceProperties.getProperty("SRS_SERVICE");
        mapCallString = serviceProperties.getProperty("MAP_CALL_STRING") + srsService;
        geoBuffer = Double.parseDouble(serviceProperties.getProperty("GEO_BUFFER"));
        geoBufferMultiplier = Double.parseDouble(serviceProperties.getProperty("GEO_BUFFER_MULTIPLIER"));

        nivpHost = serviceProperties.getProperty("NIVP_HOST");
        nivpPrefix = serviceProperties.getProperty("NIVP_PREFIX");

        apmapsHost = serviceProperties.getProperty("APMAPS_HOST");
        apmapsPrefix = serviceProperties.getProperty("APMAPS_PREFIX");

        vermessungHostBilder = serviceProperties.getProperty("VERMESSUNG_HOST_BILDER");
        vermessungHostGrenzniederschriften = serviceProperties.getProperty(
                "VERMESSUNG_HOST_GRENZNIEDERSCHRIFTEN");
        apmapsEtrsHost = serviceProperties.getProperty("APMAPS_ETRS_HOST");

        landparcelFeatureRendererColor = serviceProperties.getProperty("LANDPARCEL_FEATURE_RENDERER_COLOR");
    }
}
