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
    private static final String GRENZNIEDERSCHRIFT_PREFIX = "GN";
    private static final String VERMESSUNGSRISS_PREFIX = "VR";
    private static final String BUCHWERK_ERGAENZUNGSKARTEN_PREFIX = "GN";
    private static final String BUCHWERK_FLURBUECHER_PREFIX = "FB";
    private static final String BUCHWERK_LIEGENSCHAFTSBUECHER_PREFIX = "LB";
    private static final String BUCHWERK_NAMENSVERZEICHNIS_PREFIX = "NV";
    private static final String BUCHWERK_ERGAENZUNGSKARTEN_SCHLUESSEL = "518";
    private static final String BUCHWERK_FLURBUECHER1_SCHLUESSEL = "536";
    private static final String BUCHWERK_FLURBUECHER2_SCHLUESSEL = "537";
    private static final String BUCHWERK_LIEGENSCHAFTSBUECHER_SCHLUESSEL = "546";
    private static final String BUCHWERK_NAMENSVERZEICHNIS_SCHLUESSEL = "566";
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
     * @param   schluessel    DOCUMENT ME!
     * @param   gemarkung     DOCUMENT ME!
     * @param   steuerbezirk  DOCUMENT ME!
     * @param   bezeichner    DOCUMENT ME!
     * @param   historisch    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<URL> findVermessungsbuchwerkPicture(final String schluessel,
            final CidsBean gemarkung,
            final Integer steuerbezirk,
            final String bezeichner,
            final boolean historisch) {
        return findVermessungsbuchwerkPicture(true, schluessel, gemarkung, steuerbezirk, bezeichner, historisch);
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
     * @param   checkReducedSize  DOCUMENT ME!
     * @param   schluessel        DOCUMENT ME!
     * @param   gemarkung         DOCUMENT ME!
     * @param   steuerbezirk      DOCUMENT ME!
     * @param   bezeichner        DOCUMENT ME!
     * @param   historisch        DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<URL> findVermessungsbuchwerkPicture(final boolean checkReducedSize,
            final String schluessel,
            final CidsBean gemarkung,
            final Integer steuerbezirk,
            final String bezeichner,
            final boolean historisch) {
        final String picturePath = getVermessungsbuchwerkPictureFilename(
                schluessel,
                gemarkung,
                steuerbezirk,
                bezeichner,
                historisch);
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
     * @param   withPath      DOCUMENT ME!
     * @param   schluessel    DOCUMENT ME!
     * @param   gemarkung     DOCUMENT ME!
     * @param   steuerbezirk  DOCUMENT ME!
     * @param   bezeichner    DOCUMENT ME!
     * @param   historisch    isGrenzniederschrift DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getBuchwerkFilename(final boolean withPath,
            final String schluessel,
            final CidsBean gemarkung,
            final Integer steuerbezirk,
            final String bezeichner,
            final boolean historisch) {
        final StringBuffer buf = new StringBuffer();
        if (withPath) {
            buf.append(getBuchwerkFolder(schluessel, gemarkung));
            buf.append(SEP);
        }

        if (BUCHWERK_ERGAENZUNGSKARTEN_SCHLUESSEL.equals(schluessel)) {
            buf.append(BUCHWERK_ERGAENZUNGSKARTEN_PREFIX).append("_");
        } else if (BUCHWERK_FLURBUECHER1_SCHLUESSEL.equals(schluessel)) {
            buf.append(BUCHWERK_FLURBUECHER_PREFIX).append("_");
        } else if (BUCHWERK_FLURBUECHER2_SCHLUESSEL.equals(schluessel)) {
            buf.append(BUCHWERK_FLURBUECHER_PREFIX).append("_");
        } else if (BUCHWERK_LIEGENSCHAFTSBUECHER_SCHLUESSEL.equals(schluessel)) {
            buf.append(BUCHWERK_LIEGENSCHAFTSBUECHER_PREFIX).append("_");
        } else if (BUCHWERK_NAMENSVERZEICHNIS_SCHLUESSEL.equals(schluessel)) {
            buf.append(BUCHWERK_NAMENSVERZEICHNIS_PREFIX).append("_");
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
        final Integer gemarkung;
        if (filename.startsWith(PATH_PLATZHALTER)) {
            return (isGrenzNiederschrift ? alkisConf.getVermessungHostGrenzniederschriften()
                                         : alkisConf.getVermessungHostBilder()) + filename;
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
        final String ret = getBuchwerkFilename(true, schluessel, gemarkung, steuerbezirk, bezeichner, historisch);

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
            buf = new StringBuffer(alkisConf.getVermessungHostGrenzniederschriften());
        } else {
            buf = new StringBuffer(alkisConf.getVermessungHostBilder());
        }
        return buf.append(String.format("%04d", gemarkung)).toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   schluessel  DOCUMENT ME!
     * @param   gemarkung   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getBuchwerkFolder(final String schluessel, final CidsBean gemarkung) {
        final StringBuffer buf = new StringBuffer();
        if (BUCHWERK_NAMENSVERZEICHNIS_SCHLUESSEL.equals(schluessel)) {
            buf.append(alkisConf.getVermessungHostNamensverzeichnis())
                    .append(SEP)
                    .append(BUCHWERK_NAMENSVERZEICHNIS_PREFIX)
                    .append("_")
                    .append(StringUtils.leftPad(schluessel, 3, '0'))
                    .append("-")
                    .append(String.format("%04d", (Integer)gemarkung.getProperty("id")));
        } else if (BUCHWERK_FLURBUECHER1_SCHLUESSEL.equals(schluessel)) {
            buf.append(alkisConf.getVermessungHostFlurbuecher());
        } else if (BUCHWERK_FLURBUECHER2_SCHLUESSEL.equals(schluessel)) {
            buf.append(alkisConf.getVermessungHostFlurbuecher());
        } else if (BUCHWERK_LIEGENSCHAFTSBUECHER_SCHLUESSEL.equals(schluessel)) {
            buf.append(alkisConf.getVermessungHostLiegenschaftsbuecher())
                    .append(SEP)
                    .append((String)gemarkung.getProperty("name"));
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
