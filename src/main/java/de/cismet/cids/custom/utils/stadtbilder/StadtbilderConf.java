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
package de.cismet.cids.custom.utils.stadtbilder;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import de.cismet.cids.custom.wunda_blau.search.server.MetaObjectNodesStadtbildSerieSearchStatement;
import de.cismet.cids.utils.serverresources.ServerResource;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public abstract class StadtbilderConf {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(StadtbilderConf.class);

    public static final String YEAR = "{year}";
    public static final String IMAGE_NUMBER = "{imageNumber}";
    public static final String FILE_ENDING = "{fileEnding}";
    public static final String DIRECTION = "{direction}";
    public static final String FIRST_CHARACTER = "{firstCharacter}";

    private static final Integer DEFAULT_CACHE_SIZE = 100;

    //~ Instance fields --------------------------------------------------------

    private final String previewUrlBase;
    private final String highresUrlBase;
    private final String defaultHighresLocationTemplate;
    private final String defaultPreviewLocationTemplate;
    private final String reihenschraegHighresLocationTemplate;
    private final String reihenschraegPreviewLocationTemplate;
    private final String arcLocationTemplate;
    private final String[] fileFormats;
    private final Integer cacheSize;
    private final String tifferAnnotation;
    private ServerResource serverResource = null;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new StadtbilderConf object.
     *
     * @param  serviceProperties  DOCUMENT ME!
     */
    protected StadtbilderConf(final Properties serviceProperties) {
        previewUrlBase = serviceProperties.getProperty("PREVIEW_URL_BASE");
        highresUrlBase = serviceProperties.getProperty("HIGHRES_URL_BASE");
        defaultHighresLocationTemplate = serviceProperties.getProperty("DEFAULT_HIGHRES_LOCATION_TEMPLATE");
        defaultPreviewLocationTemplate = serviceProperties.getProperty("DEFAULT_PREVIEW_LOCATION_TEMPLATE");
        reihenschraegHighresLocationTemplate = serviceProperties.getProperty("REIHENSCHRAEG_HIGHRES_LOCATION_TEMPLATE");
        reihenschraegPreviewLocationTemplate = serviceProperties.getProperty("REIHENSCHRAEG_PREVIEW_LOCATION_TEMPLATE");
        arcLocationTemplate = serviceProperties.getProperty("ARC_LOCATION_TEMPLATE");
        fileFormats = (serviceProperties.getProperty("FILE_FORMATS") != null)
            ? serviceProperties.getProperty("FILE_FORMATS").split(",") : new String[0];
        cacheSize = (serviceProperties.getProperty("CACHE_SIZE") != null)
            ? Integer.valueOf(serviceProperties.getProperty("CACHE_SIZE")) : DEFAULT_CACHE_SIZE;
        tifferAnnotation = serviceProperties.getProperty("TIFFER_ANNOTATION");
    }

    protected StadtbilderConf(final ServerResource serverResource) throws Exception {
        this(ServerResourcesLoader.getInstance().loadProperties(serverResource));
        this.serverResource = serverResource;
    }

    //~ Methods ----------------------------------------------------------------

    public String getPreviewUrlBase() {
        if (serverResource != null) {
            try {
                Properties serviceProperties = ServerResourcesLoader.getInstance().loadProperties(serverResource);
                
                return serviceProperties.getProperty("PREVIEW_URL_BASE");
            } catch (final Exception ex) {
                throw new RuntimeException("Exception while initializing ServerStadtbilderConf", ex);
            }
        } else {
            return previewUrlBase;
        }
    }
    
    public String getHighresUrlBase() {
        if (serverResource != null) {
            try {
                Properties serviceProperties = ServerResourcesLoader.getInstance().loadProperties(serverResource);
                
                return serviceProperties.getProperty("HIGHRES_URL_BASE");
            } catch (final Exception ex) {
                throw new RuntimeException("Exception while initializing ServerStadtbilderConf", ex);
            }
        } else {
            return highresUrlBase;
        }
    }
    
    /**
     * DOCUMENT ME!
     *
     * @param   imageNumber    DOCUMENT ME!
     * @param   bildtypId      DOCUMENT ME!
     * @param   jahr           DOCUMENT ME!
     * @param   blickrichtung  DOCUMENT ME!
     * @param   format         DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public URL[] getPreviewPictureUrls(final String imageNumber,
            final Integer bildtypId,
            final Integer jahr,
            final String blickrichtung,
            final String format) {
        final String locationOfImage;
        if (MetaObjectNodesStadtbildSerieSearchStatement.Bildtyp.REIHENSCHRAEG.getId() == bildtypId) {
            locationOfImage = getReihenschraegPreviewLocationTemplate().replace(YEAR, String.valueOf(jahr))
                        .replace(DIRECTION, blickrichtung)
                        .replace(IMAGE_NUMBER, imageNumber);
        } else {
            locationOfImage = getDefaultPreviewLocationTemplate().replace(
                        FIRST_CHARACTER,
                        String.valueOf(imageNumber.charAt(0))).replace(IMAGE_NUMBER, imageNumber);
        }
        final List<URL> urls = new ArrayList<>();
        if (format == null) {
            for (final String fileEnding : getFileFormats()) {
                try {
                    urls.add(new URL(getPreviewUrlBase() + locationOfImage.replace(FILE_ENDING, fileEnding)));
                } catch (MalformedURLException ex) {
                    LOG.warn(ex, ex);
                }
            }
        } else {
            try {
                urls.add(new URL(getPreviewUrlBase() + locationOfImage.replace(FILE_ENDING, format)));
            } catch (MalformedURLException ex) {
                LOG.warn(ex, ex);
            }
        }
        return urls.toArray(new URL[0]);
    }
    /**
     * DOCUMENT ME!
     *
     * @param   imageNumber    DOCUMENT ME!
     * @param   bildtypId      DOCUMENT ME!
     * @param   jahr           DOCUMENT ME!
     * @param   blickrichtung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public URL[] getPreviewPictureUrls(final String imageNumber,
            final Integer bildtypId,
            final Integer jahr,
            final String blickrichtung) {
        return getPreviewPictureUrls(imageNumber, bildtypId, jahr, blickrichtung, null);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   imageNumber    DOCUMENT ME!
     * @param   bildtypId      DOCUMENT ME!
     * @param   jahr           DOCUMENT ME!
     * @param   blickrichtung  DOCUMENT ME!
     * @param   format         DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public URL[] getHighresPictureUrls(final String imageNumber,
            final Integer bildtypId,
            final Integer jahr,
            final String blickrichtung,
            final String format) {
        final String locationOfImage;
        if (MetaObjectNodesStadtbildSerieSearchStatement.Bildtyp.REIHENSCHRAEG.getId() == bildtypId) {
            locationOfImage = getReihenschraegHighresLocationTemplate().replace(YEAR, String.valueOf(jahr))
                        .replace(DIRECTION, blickrichtung)
                        .replace(IMAGE_NUMBER, imageNumber);
        } else {
            locationOfImage = getDefaultHighresLocationTemplate().replace(
                        FIRST_CHARACTER,
                        String.valueOf(imageNumber.charAt(0))).replace(IMAGE_NUMBER, imageNumber);
        }

        final List<URL> urls = new ArrayList<>();
        if (format == null) {
            for (final String fileEnding : getFileFormats()) {
                try {
                    urls.add(new URL(getHighresUrlBase() + locationOfImage.replace(FILE_ENDING, fileEnding)));
                } catch (MalformedURLException ex) {
                    LOG.warn(ex, ex);
                }
            }
        } else {
            try {
                urls.add(new URL(getHighresUrlBase() + locationOfImage.replace(FILE_ENDING, format)));
            } catch (MalformedURLException ex) {
                LOG.warn(ex, ex);
            }
        }
        return urls.toArray(new URL[0]);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   imageNumber    DOCUMENT ME!
     * @param   bildtypId      DOCUMENT ME!
     * @param   jahr           DOCUMENT ME!
     * @param   blickrichtung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public URL[] getHighresPictureUrls(final String imageNumber,
            final Integer bildtypId,
            final Integer jahr,
            final String blickrichtung) {
        return getHighresPictureUrls(imageNumber, bildtypId, jahr, blickrichtung, null);
    }
}
