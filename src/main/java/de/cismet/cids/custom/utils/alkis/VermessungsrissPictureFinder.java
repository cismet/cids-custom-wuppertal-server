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
import java.io.UnsupportedEncodingException;

import java.net.URL;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.List;

import de.cismet.cids.dynamics.CidsBean;

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

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            VermessungsrissPictureFinder.class);

    public static final String SEP = "/";
    public static final String SUFFIX_REDUCED_SIZE = "_rs";

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
    private static final String LINKEXTENSION = ".txt";
    private static final String PREFIX_GRENZNIEDERSCHRIFT = "GN";
    private static final String PREFIX_VERMESSUNGSRISS = "VR";
    private static final String PREFIX_ERGAENZUNGSKARTEN = "GN";
    private static final String PREFIX_FLURBUECHER = "FB";
    private static final String PREFIX_LIEGENSCHAFTSBUECHER = "LB";
    private static final String PREFIX_NAMENSVERZEICHNIS = "NV";
    private static final String SCHLUESSEL_ERGAENZUNGSKARTEN = "518";
    private static final String SCHLUESSEL_FLURBUECHER1 = "536";
    private static final String SCHLUESSEL_FLURBUECHER2 = "537";
    private static final String SCHLUESSEL_LIEGENSCHAFTSBUECHER1 = "546";
    private static final String SCHLUESSEL_LIEGENSCHAFTSBUECHER2 = "547";
    private static final String SCHLUESSEL_NAMENSVERZEICHNIS = "566";
    private static final String PATH_PLATZHALTER = "platzhalter";

    //~ Instance fields --------------------------------------------------------

    private final ExtendedAccessHandler simpleUrlAccessHandler;
    private final AlkisConf alkisConf;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsrissPictureFinder object.
     *
     * @param  simpleUrlAccessHandler  DOCUMENT ME!
     * @param  alkisConf               DOCUMENT ME!
     */
    protected VermessungsrissPictureFinder(final ExtendedAccessHandler simpleUrlAccessHandler,
            final AlkisConf alkisConf) {
        this.simpleUrlAccessHandler = simpleUrlAccessHandler;
        this.alkisConf = alkisConf;
    }

    /**
     * Creates a new VermessungsrissPictureFinder object.
     */
    private VermessungsrissPictureFinder() {
        this.simpleUrlAccessHandler = new SimpleHttpAccessHandler();
        this.alkisConf = ServerAlkisConf.getInstance();
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   riss       DOCUMENT ME!
     * @param   gemarkung  DOCUMENT ME!
     * @param   flur       DOCUMENT ME!
     * @param   blatt      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<String> findVermessungsrissPicture(final String riss,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        final String picturePath = getVermessungsrissPictureFilename(riss, gemarkung, flur, blatt);
        if (LOG.isDebugEnabled()) {
            LOG.debug("findVermessungrissPicture: " + picturePath);
        }

        return probeWebserverForRightSuffix(false, true, picturePath);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   schluessel    DOCUMENT ME!
     * @param   gemarkung     DOCUMENT ME!
     * @param   steuerbezirk  DOCUMENT ME!
     * @param   bezeichner    DOCUMENT ME!
     * @param   historisch    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<String> findVermessungsbuchwerkPicture(final String schluessel,
            final CidsBean gemarkung,
            final Integer steuerbezirk,
            final String bezeichner,
            final boolean historisch) {
        final String fileName = getVermessungsbuchwerkPictureFilename(
                schluessel,
                gemarkung,
                steuerbezirk,
                bezeichner,
                historisch);
        if (LOG.isDebugEnabled()) {
            LOG.debug("findVermessungrissPicture: " + fileName);
        }

        return probeWebserverForRightSuffix(true, true, fileName);
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
    public List<String> findGrenzniederschriftPicture(final String riss,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        final String picturePath = getGrenzniederschriftFilename(riss, gemarkung, flur, blatt);
        if (LOG.isDebugEnabled()) {
            LOG.debug("findGrenzniederschriftPicture: " + picturePath);
        }
        return probeWebserverForRightSuffix(false, true, picturePath);
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
     * @param   schluessel            laufendeNummer DOCUMENT ME!
     * @param   gemarkung             DOCUMENT ME!
     * @param   flur                  DOCUMENT ME!
     * @param   blatt                 DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getObjectFilename(final boolean withPath,
            final boolean isGrenzniederschrift,
            final String schluessel,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        final boolean isErganzungskarte = SCHLUESSEL_ERGAENZUNGSKARTEN.equals(schluessel);
        final StringBuffer buf = new StringBuffer();
        if (isGrenzniederschrift) {
            buf.append(PREFIX_GRENZNIEDERSCHRIFT);
        } else {
            buf.append(PREFIX_VERMESSUNGSRISS);
        }
        buf.append("_");
        buf.append(StringUtils.leftPad(schluessel, 3, '0'));
        buf.append("-");
        buf.append(String.format("%04d", gemarkung));
        buf.append("-");
        buf.append(StringUtils.leftPad(flur, 3, '0'));
        buf.append("-");
        buf.append(StringUtils.leftPad(blatt, 8, '0'));
        final StringBuffer b = new StringBuffer();
        if (withPath) {
            b.append(getFolder(isErganzungskarte, isGrenzniederschrift, gemarkung));
            b.append(SEP);
        }
        b.append(buf.toString());
        return b.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   schluessel    DOCUMENT ME!
     * @param   gemarkung     DOCUMENT ME!
     * @param   steuerbezirk  DOCUMENT ME!
     * @param   bezeichner    DOCUMENT ME!
     * @param   historisch    isGrenzniederschrift DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  UnsupportedEncodingException  DOCUMENT ME!
     */
    public String getBuchwerkFilename(final String schluessel,
            final CidsBean gemarkung,
            final Integer steuerbezirk,
            final String bezeichner,
            final boolean historisch) throws UnsupportedEncodingException {
        final StringBuffer buf = new StringBuffer();
        buf.append(getBuchwerkFolder(schluessel, gemarkung));
        if (SCHLUESSEL_ERGAENZUNGSKARTEN.equals(schluessel)) {
            buf.append(PREFIX_ERGAENZUNGSKARTEN).append("_");
        } else if (SCHLUESSEL_FLURBUECHER1.equals(schluessel)
                    || SCHLUESSEL_FLURBUECHER2.equals(schluessel)) {
            buf.append(PREFIX_FLURBUECHER).append("_");
        } else if (SCHLUESSEL_LIEGENSCHAFTSBUECHER1.equals(schluessel)
                    || SCHLUESSEL_LIEGENSCHAFTSBUECHER2.equals(schluessel)) {
            buf.append(PREFIX_LIEGENSCHAFTSBUECHER).append("_");
        } else if (SCHLUESSEL_NAMENSVERZEICHNIS.equals(schluessel)) {
            buf.append(PREFIX_NAMENSVERZEICHNIS).append("_");
        }
        buf.append(StringUtils.leftPad(schluessel, 3, '0'))
                .append("-")
                .append(String.format("%04d", (Integer)gemarkung.getProperty("id")))
                .append("-")
                .append(historisch ? "001" : "000")
                .append("-")
                .append(steuerbezirk)
                .append(StringUtils.leftPad(bezeichner, 7, '0'));

        return buf.toString();
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
        if (filename.startsWith(PATH_PLATZHALTER)) {
            return (isGrenzNiederschrift ? alkisConf.getVermessungHostGrenzniederschriften()
                                         : alkisConf.getVermessungHostBilder()) + filename;
        }
        final boolean isErganzungskarte = filename.contains((isGrenzNiederschrift ? PREFIX_VERMESSUNGSRISS
                                                                                  : PREFIX_GRENZNIEDERSCHRIFT) + "_"
                        + SCHLUESSEL_ERGAENZUNGSKARTEN + "-");
        final String[] splittedFilename = filename.split("-");
        final Integer gemarkung = Integer.parseInt(splittedFilename[1]);
        final String filenameWithPrefix = (isGrenzNiederschrift ? PREFIX_GRENZNIEDERSCHRIFT : PREFIX_VERMESSUNGSRISS)
                    + "_" + filename;
        return new StringBuffer(getFolder(isErganzungskarte, isGrenzNiederschrift, gemarkung)).append(SEP)
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
     * @param   schluessel    DOCUMENT ME!
     * @param   gemarkung     DOCUMENT ME!
     * @param   steuerbezirk  DOCUMENT ME!
     * @param   bezeichner    DOCUMENT ME!
     * @param   historisch    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getVermessungsbuchwerkPictureFilename(final String schluessel,
            final CidsBean gemarkung,
            final Integer steuerbezirk,
            final String bezeichner,
            final boolean historisch) {
        final String ret;
        try {
            ret = getBuchwerkFilename(schluessel, gemarkung, steuerbezirk, bezeichner, historisch);
        } catch (final UnsupportedEncodingException ex) {
            LOG.error(ex, ex);
            return null;
        }

        return (ret != null) ? ret : null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   buchwerk           DOCUMENT ME!
     * @param   checkReducedSize   DOCUMENT ME!
     * @param   fileWithoutSuffix  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private List<String> probeWebserverForRightSuffix(final boolean buchwerk,
            final boolean checkReducedSize,
            final String fileWithoutSuffix) {
        return probeWebserverForRightSuffix(buchwerk, checkReducedSize, fileWithoutSuffix, 0);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   buchwerk           DOCUMENT ME!
     * @param   checkReducedSize   DOCUMENT ME!
     * @param   fileWithoutSuffix  DOCUMENT ME!
     * @param   recursionDepth     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<String> probeWebserverForRightSuffix(final boolean buchwerk,
            final boolean checkReducedSize,
            final String fileWithoutSuffix,
            final int recursionDepth) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching for picture: " + fileWithoutSuffix + "xxx");
        }
        final List<String> results = new ArrayList<>();
        // check if there is a reduced size image direcly...        
        for (final String suffix : SUFFIXE) {
            final String fileWithSuffix = (checkReducedSize ? (fileWithoutSuffix + SUFFIX_REDUCED_SIZE) : fileWithoutSuffix) + suffix;
            try {
                final URL objectURL = alkisConf.getDownloadUrlForDocument(fileWithSuffix);
                if (simpleUrlAccessHandler.checkIfURLaccessible(objectURL)) {
                    results.add(fileWithSuffix);
                }
            } catch (Exception ex) {
                LOG.error("Problem occured, during checking for " + fileWithSuffix, ex);
            }
        }
        // we need to do an extra round if we checked with _rs suffix...
        if (results.isEmpty() && checkReducedSize) {
            for (final String suffix : SUFFIXE) {
                final String fileWithSuffix = fileWithoutSuffix + suffix;
                try {
                    final URL objectURL = alkisConf.getDownloadUrlForDocument(fileWithSuffix);
                    if (simpleUrlAccessHandler.checkIfURLaccessible(objectURL)) {
                        results.add(fileWithSuffix);
                    }
                } catch (Exception ex) {
                    LOG.error("Problem occured, during checking for " + fileWithSuffix, ex);
                }
            }
        }        
        // if the results is empty check if there is a link...
        if (results.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No picture file found. Check for Links");
            }
            if (recursionDepth < 3) {
                InputStream urlStream = null;
                try {
                    final URL objectURL = alkisConf.getDownloadUrlForDocument(fileWithoutSuffix + LINKEXTENSION);
                    if (simpleUrlAccessHandler.checkIfURLaccessible(objectURL)) {
                        urlStream = simpleUrlAccessHandler.doRequest(objectURL);
                        if (urlStream != null) {
                            final String link = IOUtils.toString(urlStream).trim();
                            if (buchwerk) {
                                return probeWebserverForRightSuffix(
                                        buchwerk,
                                        checkReducedSize,
                                        fileWithoutSuffix.substring(0, fileWithoutSuffix.lastIndexOf(SEP))
                                                + SEP
                                                + link,
                                        recursionDepth
                                                + 1);
                            } else {
                                final boolean isGrenzNiederschrift = fileWithoutSuffix.contains(
                                        PREFIX_GRENZNIEDERSCHRIFT);
                                return probeWebserverForRightSuffix(
                                        buchwerk,
                                        checkReducedSize,
                                        getObjectPath(isGrenzNiederschrift, link),
                                        recursionDepth
                                                + 1);
                            }
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
     * @param   isErgaenzungskarte    DOCUMENT ME!
     * @param   isGrenzniederschrift  DOCUMENT ME!
     * @param   gemarkung             DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getFolder(final boolean isErgaenzungskarte,
            final boolean isGrenzniederschrift,
            final Integer gemarkung) {
        final StringBuffer buf;
        if (isErgaenzungskarte) {
            buf = new StringBuffer(alkisConf.getVermessungHostErgaenzungskarten());
        } else if (isGrenzniederschrift) {
            buf = new StringBuffer(alkisConf.getVermessungHostGrenzniederschriften());
        } else {
            buf = new StringBuffer(alkisConf.getVermessungHostBilder());
        }
        if (!isErgaenzungskarte) {
            buf.append(String.format("%04d", gemarkung));
        }
        return buf.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   schluessel  DOCUMENT ME!
     * @param   gemarkung   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  UnsupportedEncodingException  DOCUMENT ME!
     */
    public String getBuchwerkFolder(final String schluessel, final CidsBean gemarkung)
            throws UnsupportedEncodingException {
        final StringBuffer buf = new StringBuffer();
        if (SCHLUESSEL_NAMENSVERZEICHNIS.equals(schluessel)) {
            buf.append(alkisConf.getVermessungHostNamensverzeichnis())
                    .append(SEP)
                    .append(PREFIX_NAMENSVERZEICHNIS)
                    .append("_")
                    .append(StringUtils.leftPad(schluessel, 3, '0'))
                    .append("-")
                    .append(String.format("%04d", (Integer)gemarkung.getProperty("id")));
        } else if (SCHLUESSEL_FLURBUECHER1.equals(schluessel)
                    || SCHLUESSEL_FLURBUECHER2.equals(schluessel)) {
            buf.append(alkisConf.getVermessungHostFlurbuecher());
        } else if (SCHLUESSEL_LIEGENSCHAFTSBUECHER1.equals(schluessel)
                    || SCHLUESSEL_LIEGENSCHAFTSBUECHER2.equals(schluessel)) {
            buf.append(alkisConf.getVermessungHostLiegenschaftsbuecher())
                    .append(SEP)
                    .append(URLEncoder.encode((String)gemarkung.getProperty("name"), "UTF-8"));
        }
        return buf.toString();
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
