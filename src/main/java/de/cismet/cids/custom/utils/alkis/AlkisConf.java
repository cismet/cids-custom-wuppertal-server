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

import java.net.URL;

import java.util.Properties;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public abstract class AlkisConf {

    //~ Static fields/initializers ---------------------------------------------

    private static final String DOWNLOAD_TEMPLATE =
        "<rasterfari:url>?REQUEST=GetMap&SERVICE=WMS&customDocumentInfo=download&LAYERS=<rasterfari:document>";

    //~ Instance fields --------------------------------------------------------

    private final String credentialsFile;
    private final String service;
    private final String server;
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
    private final String rasterfariUrl;
    private final String nivpHost;
    private final String nivpPrefix;
    private final String apmapsHost;
    private final String apmapsEtrsHost;
    private final String apmapsPrefix;
    private final String vermessungHostBilder;
    private final String vermessungHostGrenzniederschriften;
    private final String vermessungHostFlurbuecher;
    private final String vermessungHostLiegenschaftsbuecher;
    private final String vermessungHostNamensverzeichnis;
    private final String vermessungHostErgaenzungskarten;
    private final String landparcelFeatureRendererColor;
    private final String demoServiceUrl;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlkisConf object.
     *
     * @param  serviceProperties  DOCUMENT ME!
     */
    protected AlkisConf(final Properties serviceProperties) {
        credentialsFile = serviceProperties.getProperty("CREDENTIALS_FILE");
        service = serviceProperties.getProperty("SERVICE");
        server = serviceProperties.getProperty("SERVER");
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
        rasterfariUrl = serviceProperties.getProperty("RASTERFARI_URL");
        nivpHost = serviceProperties.getProperty("NIVP_HOST");
        nivpPrefix = serviceProperties.getProperty("NIVP_PREFIX");
        apmapsPrefix = serviceProperties.getProperty("APMAPS_PREFIX");
        vermessungHostBilder = serviceProperties.getProperty("VERMESSUNG_HOST_BILDER");
        vermessungHostGrenzniederschriften = serviceProperties.getProperty("VERMESSUNG_HOST_GRENZNIEDERSCHRIFTEN");
        vermessungHostErgaenzungskarten = serviceProperties.getProperty("VERMESSUNG_HOST_ERGAENZUNGSKARTEN");
        vermessungHostFlurbuecher = serviceProperties.getProperty("VERMESSUNG_HOST_FLURBUECHER");
        vermessungHostLiegenschaftsbuecher = serviceProperties.getProperty("VERMESSUNG_HOST_LIEGENSCHAFTSBUECHER");
        vermessungHostNamensverzeichnis = serviceProperties.getProperty("VERMESSUNG_HOST_NAMENSVERZEICHNIS");
        apmapsHost = serviceProperties.getProperty("APMAPS_HOST");
        apmapsEtrsHost = serviceProperties.getProperty("APMAPS_ETRS_HOST");
        landparcelFeatureRendererColor = serviceProperties.getProperty("LANDPARCEL_FEATURE_RENDERER_COLOR");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   document  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public final URL getDownloadUrlForDocument(final String document) throws Exception {
        return new URL(DOWNLOAD_TEMPLATE.replace("<rasterfari:url>", getRasterfariUrl()).replace(
                    "<rasterfari:document>",
                    document));
    }
}
