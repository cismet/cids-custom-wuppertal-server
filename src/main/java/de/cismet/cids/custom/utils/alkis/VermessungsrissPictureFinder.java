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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;

import de.cismet.commons.security.handler.ExtendedAccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class VermessungsrissPictureFinder {

    //~ Static fields/initializers ---------------------------------------------

    public static final String SEP = "/";
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(VermessungsrissPictureFinder.class);
    private static final String[] SUFFIXE = new String[] {
            ".tif",
            ".jpg",
            ".jpe",
            ".tiff",
            ".jpeg",
            ".TIF",
            ".JPG",
            ".JPE",
            ".TIFF",
            ".JPEG"
        };
    public static final String SUFFIX_REDUCED_SIZE = "_rs";
    private static final String LINKEXTENSION = ".txt";
    public static String PATH_VERMESSUNG = AlkisConstants.COMMONS.VERMESSUNG_HOST_BILDER; //
    public static String PATH_GRENZNIEDERSCHRIFT = AlkisConstants.COMMONS.VERMESSUNG_HOST_GRENZNIEDERSCHRIFTEN;
    private static final String GRENZNIEDERSCHRIFT_PREFIX = "GN";
    private static final String VERMESSUNGSRISS_PREFIX = "VR";
    private static final String PATH_PLATZHALTER = "platzhalter";

    //~ Instance fields --------------------------------------------------------

    private final ExtendedAccessHandler simpleUrlAccessHandler;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsrissPictureFinder object.
     *
     * @param  simpleUrlAccessHandler  DOCUMENT ME!
     */
    protected VermessungsrissPictureFinder(final ExtendedAccessHandler simpleUrlAccessHandler) {
        this.simpleUrlAccessHandler = simpleUrlAccessHandler;
    }

    /**
     * Creates a new VermessungsrissPictureFinder object.
     */
    private VermessungsrissPictureFinder() {
        this.simpleUrlAccessHandler = new SimpleHttpAccessHandler();
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   riss       blattnummer picture DOCUMENT ME!
     * @param   gemarkung  laufendeNummer DOCUMENT ME!
     * @param   flur       DOCUMENT ME!
     * @param   blatt      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<URL> findVermessungsrissPicture(final String riss,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        return findVermessungsrissPicture(true, riss, gemarkung, flur, blatt);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   checkReducedSize  DOCUMENT ME!
     * @param   riss              DOCUMENT ME!
     * @param   gemarkung         DOCUMENT ME!
     * @param   flur              DOCUMENT ME!
     * @param   blatt             DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<URL> findVermessungsrissPicture(final boolean checkReducedSize,
            final String riss,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        final String picturePath = getVermessungsrissPictureFilename(riss, gemarkung, flur, blatt);
        if (LOG.isDebugEnabled()) {
            LOG.debug("findVermessungrissPicture: " + picturePath);
        }

        return probeWebserverForRightSuffix(checkReducedSize, picturePath);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   riss       blattnummer picture DOCUMENT ME!
     * @param   gemarkung  laufendeNummer DOCUMENT ME!
     * @param   flur       DOCUMENT ME!
     * @param   blatt      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<URL> findGrenzniederschriftPicture(final String riss,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        return findGrenzniederschriftPicture(false, riss, gemarkung, flur, blatt);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   checkReducedSize  DOCUMENT ME!
     * @param   riss              DOCUMENT ME!
     * @param   gemarkung         DOCUMENT ME!
     * @param   flur              DOCUMENT ME!
     * @param   blatt             DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<URL> findGrenzniederschriftPicture(final boolean checkReducedSize,
            final String riss,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        final String picturePath = getGrenzniederschriftFilename(riss, gemarkung, flur, blatt);
        if (LOG.isDebugEnabled()) {
            LOG.debug("findGrenzniederschriftPicture: " + picturePath);
        }
        return probeWebserverForRightSuffix(checkReducedSize, picturePath);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   riss       blattnummer DOCUMENT ME!
     * @param   gemarkung  laufendeNummer DOCUMENT ME!
     * @param   flur       DOCUMENT ME!
     * @param   blatt      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getGrenzniederschriftFilename(final String riss,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        final String ret = getObjectFilename(true, true, riss, gemarkung, flur, blatt);

        return (ret != null) ? ret : null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   withPath              DOCUMENT ME!
     * @param   isGrenzniederschrift  blattnummer DOCUMENT ME!
     * @param   riss                  laufendeNummer DOCUMENT ME!
     * @param   gemarkung             DOCUMENT ME!
     * @param   flur                  DOCUMENT ME!
     * @param   blatt                 DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getObjectFilename(final boolean withPath,
            final boolean isGrenzniederschrift,
            final String riss,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        final StringBuffer buf = new StringBuffer();
        if (isGrenzniederschrift) {
            buf.append(GRENZNIEDERSCHRIFT_PREFIX);
        } else {
            buf.append(VERMESSUNGSRISS_PREFIX);
        }
        buf.append("_");
        buf.append(StringUtils.leftPad(riss, 3, '0'));
        buf.append("-");
        buf.append(String.format("%04d", gemarkung));
        buf.append("-");
        buf.append(StringUtils.leftPad(flur, 3, '0'));
        buf.append("-");
        buf.append(StringUtils.leftPad(blatt, 8, '0'));
        final StringBuffer b = new StringBuffer();
        if (withPath) {
            b.append(getFolder(isGrenzniederschrift, gemarkung));
            b.append(SEP);
        }
        b.append(buf.toString());
        return b.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   isGrenzNiederschrift  DOCUMENT ME!
     * @param   filename              DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getObjectPath(final boolean isGrenzNiederschrift, final String filename) {
        final Integer gemarkung;
        if (filename.startsWith(PATH_PLATZHALTER)) {
            return (isGrenzNiederschrift ? PATH_GRENZNIEDERSCHRIFT : PATH_VERMESSUNG) + filename;
        }
        final String[] splittedFilename = filename.split("-");
        gemarkung = Integer.parseInt(splittedFilename[1]);
        String filenameWithPrefix = isGrenzNiederschrift ? GRENZNIEDERSCHRIFT_PREFIX : VERMESSUNGSRISS_PREFIX;
        filenameWithPrefix += "_" + filename;
        return new StringBuffer(getFolder(isGrenzNiederschrift, gemarkung)).append(SEP)
                    .append(filenameWithPrefix)
                    .toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   isGrenzNiederschrift  DOCUMENT ME!
     * @param   documentFileName      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getLinkFromLinkDocument(final boolean isGrenzNiederschrift, final String documentFileName) {
        InputStream urlStream = null;
        try {
            final URL objectURL = new URL(documentFileName + LINKEXTENSION);
            if (simpleUrlAccessHandler.checkIfURLaccessible(objectURL)) {
                urlStream = simpleUrlAccessHandler.doRequest(objectURL);
                if (urlStream != null) {
                    final String link = IOUtils.toString(urlStream);
                    return link;
                }
            }
        } catch (Exception ex) {
            LOG.error("Exception while checking link docuemtn", ex);
        } finally {
            if (urlStream != null) {
                try {
                    urlStream.close();
                } catch (Exception e) {
                    LOG.warn("Error during closing InputStream.", e);
                }
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   riss       blattnummer DOCUMENT ME!
     * @param   gemarkung  laufendeNummer DOCUMENT ME!
     * @param   flur       DOCUMENT ME!
     * @param   blatt      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getVermessungsrissPictureFilename(final String riss,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        final String ret = getObjectFilename(true, false, riss, gemarkung, flur, blatt);

        return (ret != null) ? ret : null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   checkReducedSize   DOCUMENT ME!
     * @param   fileWithoutSuffix  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private List<URL> probeWebserverForRightSuffix(final boolean checkReducedSize,
            final String fileWithoutSuffix) {
        return probeWebserverForRightSuffix(checkReducedSize, fileWithoutSuffix, 0);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   checkReducedSize   DOCUMENT ME!
     * @param   fileWithoutSuffix  DOCUMENT ME!
     * @param   recursionDepth     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<URL> probeWebserverForRightSuffix(final boolean checkReducedSize,
            final String fileWithoutSuffix,
            final int recursionDepth) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching for picture: " + fileWithoutSuffix + "xxx");
        }
        final List<URL> results = new ArrayList<URL>();
        // check if there is a reduced size image direcly...
        final String searchName = checkReducedSize ? (fileWithoutSuffix + SUFFIX_REDUCED_SIZE) : fileWithoutSuffix;
        for (final String suffix : SUFFIXE) {
            try {
                final URL objectURL = new URL(searchName + suffix);

                if (simpleUrlAccessHandler.checkIfURLaccessible(objectURL)) {
                    results.add(objectURL);
                }
            } catch (Exception ex) {
                LOG.error("Problem occured, during checking for " + searchName + suffix, ex);
            }
        }
        // we need to do an extra round if we checked with _rs suffix...
        if (results.isEmpty() && checkReducedSize) {
            for (final String suffix : SUFFIXE) {
                try {
                    final URL objectURL = new URL(fileWithoutSuffix + suffix);

                    if (simpleUrlAccessHandler.checkIfURLaccessible(objectURL)) {
                        results.add(objectURL);
                    }
                } catch (Exception ex) {
                    LOG.error("Problem occured, during checking for " + searchName + suffix, ex);
                }
            }
        }
        // if the results is still empty check if there is a link...
        if (results.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No picture file found. Check for Links");
            }
            if (recursionDepth < 3) {
                InputStream urlStream = null;
                try {
                    final URL objectURL = new URL(fileWithoutSuffix + LINKEXTENSION);
                    if (simpleUrlAccessHandler.checkIfURLaccessible(objectURL)) {
                        urlStream = simpleUrlAccessHandler.doRequest(objectURL);
                        if (urlStream != null) {
                            final String link = IOUtils.toString(urlStream);
                            final boolean isGrenzNiederschrift = fileWithoutSuffix.contains(GRENZNIEDERSCHRIFT_PREFIX);
                            return probeWebserverForRightSuffix(
                                    checkReducedSize,
                                    getObjectPath(isGrenzNiederschrift, link.trim()),
                                    recursionDepth
                                            + 1);
                        }
                    }
                } catch (Exception ex) {
                    LOG.error(ex, ex);
                } finally {
                    if (urlStream != null) {
                        try {
                            urlStream.close();
                        } catch (Exception ex) {
                            LOG.warn("Error during closing InputStream.", ex);
                        }
                    }
                }
            } else {
                LOG.error(
                    "No hop,hop,hop possible within this logic. Seems to be an endless loop, sorry.",
                    new Exception("JustTheStackTrace"));
            }
        }
        return results;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   isGrenzniederschrift  DOCUMENT ME!
     * @param   gemarkung             DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getFolder(final boolean isGrenzniederschrift, final Integer gemarkung) {
        final StringBuffer buf;
        if (isGrenzniederschrift) {
            buf = new StringBuffer(PATH_GRENZNIEDERSCHRIFT);
        } else {
            buf = new StringBuffer(PATH_VERMESSUNG);
        }
        return buf.append(String.format("%04d", gemarkung)).toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static VermessungsrissPictureFinder getInstance() {
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

        private static final VermessungsrissPictureFinder INSTANCE = new VermessungsrissPictureFinder();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
