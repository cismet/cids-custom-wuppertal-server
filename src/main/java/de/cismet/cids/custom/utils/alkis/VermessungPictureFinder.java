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

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.newuser.User;

import lombok.Getter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;

import java.net.URL;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingWorker;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.commons.security.handler.ExtendedAccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextProvider;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class VermessungPictureFinder implements ConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(VermessungPictureFinder.class);

    public static final String SEP = "/";

    private static final String[] ENDINGS = new String[] {
            ".pdf",
            ".tif",
            ".jpg",
            ".jpm",
            ".tiff",
            ".jpeg",
            ".TIF",
            ".JPG"
        };
    public static final String LINKEXTENSION = ".txt";
    private static final String PREFIX_GRENZNIEDERSCHRIFT = "GN";
    private static final String PREFIX_VERMESSUNGSRISS = "VR";
    private static final String PREFIX_ERGAENZUNGSKARTEN = "GN";
    private static final String PREFIX_FLURBUECHER = "FB";
    private static final String PREFIX_LIEGENSCHAFTSBUECHER = "LB";
    private static final String PREFIX_LIEGENSCHAFTSKARTEN = "LK";
    private static final String PREFIX_NAMENSVERZEICHNIS = "NV";
    private static final String SCHLUESSEL_INSELKARTEN = "516";
    private static final String SCHLUESSEL_ERGAENZUNGSKARTEN = "518";
    private static final String SCHLUESSEL_FLURBUECHER1 = "536";
    private static final String SCHLUESSEL_FLURBUECHER2 = "537";
    private static final String SCHLUESSEL_LIEGENSCHAFTSBUECHER1 = "546";
    private static final String SCHLUESSEL_LIEGENSCHAFTSBUECHER2 = "547";
    private static final String SCHLUESSEL_NAMENSVERZEICHNIS = "566";
    private static final String PATH_PLATZHALTER = "platzhalter";

    private static Map<String, CidsBean> FILE_ENDINGS = new HashMap<>();

    //~ Instance fields --------------------------------------------------------

    @Getter private final ExtendedAccessHandler accessHandler;
    @Getter private final ServerAlkisConf alkisConf;
    @Getter private final User user;
    @Getter private final MetaService metaService;
    private final ConnectionContext connectionContext;
    private final MetaClass fileEndingMc;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsrissPictureFinder object.
     *
     * @param  user               DOCUMENT ME!
     * @param  metaService        DOCUMENT ME!
     * @param  connectionContext  DOCUMENT ME!
     */
    public VermessungPictureFinder(final User user,
            final MetaService metaService,
            final ConnectionContext connectionContext) {
        this.accessHandler = new SimpleHttpAccessHandler();
        this.alkisConf = ServerAlkisConf.getInstance();

        this.user = user;
        this.metaService = metaService;
        this.connectionContext = connectionContext;

        MetaClass fileEndingMc = null;
        try {
            fileEndingMc = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "vermessung_fileending_cache",
                    connectionContext);
        } catch (final Exception ex) {
            LOG.error(ex, ex);
        }
        this.fileEndingMc = fileEndingMc;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

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
    public String findVermessungsrissPicture(final String riss,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        final String picturePath = getVermessungsrissFilename(riss, gemarkung, flur, blatt);
        return identifyFullFilename(picturePath, false);
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
    public String findBuchwerkPicture(final String schluessel,
            final CidsBean gemarkung,
            final Integer steuerbezirk,
            final String bezeichner,
            final boolean historisch) {
        final String fileName = getBuchwerkFilename(
                schluessel,
                gemarkung,
                steuerbezirk,
                bezeichner,
                historisch);
        return identifyFullFilename(fileName, true);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   gemarkung  DOCUMENT ME!
     * @param   kmquadrat  DOCUMENT ME!
     * @param   liste      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String findGewannenPicture(final CidsBean gemarkung, final Integer kmquadrat,
            final boolean liste) {
        final String fileName = getGewannenFilename(
                gemarkung,
                kmquadrat,
                liste);
        return identifyFullFilename(fileName, true);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   ordner  DOCUMENT ME!
     * @param   nummer  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String findGebaeudebeschreibungPicture(final String ordner, final Integer nummer) {
        final String fileName = getGebaeudebeschreibungFilename(ordner, nummer);
        return identifyFullFilename(fileName, true);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   schluessel  DOCUMENT ME!
     * @param   gemarkung   DOCUMENT ME!
     * @param   flur        DOCUMENT ME!
     * @param   blatt       DOCUMENT ME!
     * @param   version     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String findInselkartePicture(final String schluessel,
            final CidsBean gemarkung,
            final String flur,
            final String blatt,
            final String version) {
        final String fileName = getInselkarteFilename(
                schluessel,
                gemarkung,
                flur,
                blatt,
                version);
        return identifyFullFilename(fileName, true);
    }

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
    public String findGrenzniederschriftPicture(final String riss,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        final String picturePath = getGrenzniederschriftFilename(riss, gemarkung, flur, blatt);
        return identifyFullFilename(picturePath, false);
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
        return getVermessungsrissFilename(true, true, riss, gemarkung, flur, blatt);
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
    private String getVermessungsrissFilename(final boolean withPath,
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
            b.append(getRissFolder(isErganzungskarte, isGrenzniederschrift, gemarkung));
            b.append(SEP);
        }
        b.append(buf.toString());
        return b.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   link  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getVermessungsrissLinkFilename(final String link) {
        return getObjectPath(false, link);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   link  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getGrenzniederschriftLinkFilename(final String link) {
        return getObjectPath(true, link);
    }
    /**
     * DOCUMENT ME!
     *
     * @param   isGrenzNiederschrift  DOCUMENT ME!
     * @param   filename              DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getObjectPath(final boolean isGrenzNiederschrift, final String filename) {
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
        return new StringBuffer(getRissFolder(isErganzungskarte, isGrenzNiederschrift, gemarkung)).append(SEP)
                    .append(filenameWithPrefix)
                    .toString();
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
    public String getVermessungsrissFilename(final String riss,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        return getVermessungsrissFilename(true, false, riss, gemarkung, flur, blatt);
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
    public String getBuchwerkFilename(final String schluessel,
            final CidsBean gemarkung,
            final Integer steuerbezirk,
            final String bezeichner,
            final boolean historisch) {
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
     * @param   gemarkung  DOCUMENT ME!
     * @param   kmquadrat  DOCUMENT ME!
     * @param   liste      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getGewannenFilename(
            final CidsBean gemarkung,
            final Integer kmquadrat,
            final boolean liste) {
        final StringBuffer buf = new StringBuffer();
        buf.append(getGewannenFolder());
        if (liste) {
            buf.append("Gewanne_")
                    .append(((String)gemarkung.getProperty("name")).replaceAll("Ä", "Ae").replaceAll("Ü", "Ue")
                        .replaceAll("Ö", "Oe").replaceAll("ä", "ae").replaceAll("ü", "ue").replaceAll("ö", "oe")
                        .replaceAll("ß", "ss"));
        } else {
            buf.append(Integer.toString(kmquadrat)).append("-Gewanne");
        }
        return buf.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   ordner  DOCUMENT ME!
     * @param   nummer  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getGebaeudebeschreibungFilename(
            final String ordner,
            final Integer nummer) {
        return new StringBuffer().append(getGebaeudebeschreibungenFolder())
                    .append(ordner)
                    .append(SEP)
                    .append(String.format("%08d", nummer))
                    .toString();
    }
    /**
     * DOCUMENT ME!
     *
     * @param   schluessel  DOCUMENT ME!
     * @param   gemarkung   DOCUMENT ME!
     * @param   flur        DOCUMENT ME!
     * @param   blatt       DOCUMENT ME!
     * @param   version     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getInselkarteFilename(final String schluessel,
            final CidsBean gemarkung,
            final String flur,
            final String blatt,
            final String version) {
        final StringBuffer buf = new StringBuffer(getInselkartenFolder(gemarkung)).append(PREFIX_LIEGENSCHAFTSKARTEN)
                    .append("_")
                    .append(StringUtils.leftPad(schluessel, 3, '0'))
                    .append("-")
                    .append(String.format("%04d", (Integer)gemarkung.getProperty("id")))
                    .append("-")
                    .append("000")
                    .append("-")
                    .append(StringUtils.leftPad(flur, 3, '0'))
                    .append(StringUtils.leftPad(blatt, 2, '0'))
                    .append(StringUtils.leftPad(version, 3, '0'));

        return buf.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fileWithoutSuffix  DOCUMENT ME!
     * @param   buchwerk           DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String identifyFullFilename(
            final String fileWithoutSuffix,
            final boolean buchwerk) {
        return identifyFullFilename(fileWithoutSuffix, buchwerk, 0);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fileWithoutEnding  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String identifyFilenameWithEnding(final String fileWithoutEnding) {
        final Set<String> endings = new LinkedHashSet<>();
        CidsBean firstGuess;
        if (FILE_ENDINGS.containsKey(fileWithoutEnding)) {
            firstGuess = FILE_ENDINGS.get(fileWithoutEnding);
            if (firstGuess == null) {
                return null;
            }
        } else {
            firstGuess = null;
        }
        if (firstGuess == null) {
            try {
                if (fileEndingMc != null) {
                    final String query = String.format(
                            "SELECT %d, %s "
                                    + "FROM %s "
                                    + "WHERE name = '%s';",
                            fileEndingMc.getID(),
                            fileEndingMc.getPrimaryKey(),
                            fileEndingMc.getTableName(),
                            fileWithoutEnding);

                    final MetaObject[] results = getMetaService().getMetaObject(
                            getUser(),
                            query,
                            getConnectionContext());
                    if ((results != null) && (results.length > 0)) {
                        firstGuess = results[0].getBean();
                    }
                }
            } catch (final Exception ex) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(ex, ex);
                }
            }
        }
        if (firstGuess != null) {
            final String ending = (String)firstGuess.getProperty("ending");
            endings.add(ending);
        }
        endings.addAll(Arrays.asList(ENDINGS));
        for (final String ending : endings) {
            final String fileWithEnding = fileWithoutEnding + ending;
            try {
                final URL objectURL = alkisConf.getDownloadUrlForDocument(fileWithEnding);
                if (accessHandler.checkIfURLaccessible(objectURL)) {
                    if (!FILE_ENDINGS.containsKey(fileWithoutEnding)) {
                        if (fileEndingMc != null) {
                            final CidsBean cidsBean = fileEndingMc.getEmptyInstance(getConnectionContext()).getBean();
                            FILE_ENDINGS.put(fileWithoutEnding, cidsBean);
                            new SwingWorker<Void, Object>() {

                                    @Override
                                    protected Void doInBackground() throws Exception {
                                        try {
                                            cidsBean.setProperty("name", fileWithoutEnding);
                                            cidsBean.setProperty("ending", ending);
                                            getMetaService().insertMetaObject(
                                                getUser(),
                                                cidsBean.getMetaObject(),
                                                getConnectionContext());
                                        } catch (final Exception ex) {
                                            LOG.error(ex, ex);
                                        }
                                        return null;
                                    }
                                }.execute();
                        }
                    } else {
                        final CidsBean cidsBean = FILE_ENDINGS.get(fileWithoutEnding);
                        new SwingWorker<Void, Object>() {

                                @Override
                                protected Void doInBackground() throws Exception {
                                    try {
                                        cidsBean.setProperty("ending", ending);
                                        getMetaService().updateMetaObject(
                                            getUser(),
                                            cidsBean.getMetaObject(),
                                            getConnectionContext());
                                    } catch (final Exception ex) {
                                        LOG.error(ex, ex);
                                    }
                                    return null;
                                }
                            }.execute();
                    }
                    return fileWithEnding;
                } else {
                    if (FILE_ENDINGS.containsKey(fileWithoutEnding)) {
                        final CidsBean cidsBean = FILE_ENDINGS.get(fileWithoutEnding);
                        if (ending.equals((String)cidsBean.getProperty("ending"))) {
                            new SwingWorker<Void, Object>() {

                                    @Override
                                    protected Void doInBackground() throws Exception {
                                        try {
                                            getMetaService().deleteMetaObject(
                                                getUser(),
                                                cidsBean.getMetaObject(),
                                                getConnectionContext());
                                        } catch (final Exception ex) {
                                            LOG.error(ex, ex);
                                        }
                                        return null;
                                    }
                                }.execute();
                        }
                    }
                }
            } catch (final Exception ex) {
                LOG.error("Problem occured, during checking for " + fileWithEnding, ex);
            }
        }
        FILE_ENDINGS.put(fileWithoutEnding, null);
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fileWithoutSuffix  DOCUMENT ME!
     * @param   buchwerk           DOCUMENT ME!
     * @param   recursionDepth     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String identifyFullFilename(
            final String fileWithoutSuffix,
            final boolean buchwerk,
            final int recursionDepth) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching for picture: " + fileWithoutSuffix);
        }

        final String fullSize = identifyFilenameWithEnding(fileWithoutSuffix);
        if (fullSize != null) {
            return fullSize;
        }

        // if the results is empty check if there is a link...
        if (LOG.isDebugEnabled()) {
            LOG.debug("No picture file found. Check for Links");
        }
        if (recursionDepth < 3) {
            InputStream urlStream = null;
            try {
                final URL objectURL = alkisConf.getDownloadUrlForDocument(fileWithoutSuffix + LINKEXTENSION);
                if (accessHandler.checkIfURLaccessible(objectURL)) {
                    urlStream = accessHandler.doRequest(objectURL);
                    if (urlStream != null) {
                        final String link = IOUtils.toString(urlStream).trim();
                        if (buchwerk) {
                            return identifyFullFilename(
                                    fileWithoutSuffix.substring(0, fileWithoutSuffix.lastIndexOf(SEP))
                                            + SEP
                                            + link,
                                    buchwerk,
                                    recursionDepth
                                            + 1);
                        } else {
                            final boolean isGrenzNiederschrift = fileWithoutSuffix.contains(
                                    PREFIX_GRENZNIEDERSCHRIFT);
                            return identifyFullFilename(
                                    getObjectPath(isGrenzNiederschrift, link),
                                    buchwerk,
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
        return null;
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
    private String getRissFolder(final boolean isErgaenzungskarte,
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
     */
    private String getBuchwerkFolder(final String schluessel, final CidsBean gemarkung) {
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
                    .append((String)gemarkung.getProperty("name"))
                    .append(SEP);
        }
        return buf.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getGewannenFolder() {
        return new StringBuffer().append(alkisConf.getVermessungHostGewannen()).append(SEP).toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getGebaeudebeschreibungenFolder() {
        return new StringBuffer().append(alkisConf.getVermessungHostGebaeudebeschreibungen()).append(SEP).toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   gemarkung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getInselkartenFolder(final CidsBean gemarkung) {
        return new StringBuffer().append(alkisConf.getVermessungHostInselkarten())
                    .append(SEP)
                    .append(String.format("%04d", (Integer)gemarkung.getProperty("id")))
                    .append(SEP)
                    .toString();
    }
}
