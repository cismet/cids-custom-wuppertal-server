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
package de.cismet.cids.custom.utils.vermessungsunterlagen;

import Sirius.server.middleware.types.MetaObjectNode;

import com.vividsolutions.jts.geom.Geometry;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenException;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenJobException;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskAPList;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskAPMap;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskAPUebersicht;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskNasKomplett;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskNasPunkte;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskNivPBeschreibungen;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskNivPUebersicht;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskPNR;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskRisseBilder;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskRisseGrenzniederschrift;
import de.cismet.cids.custom.wunda_blau.search.server.CidsMeasurementPointSearchStatement;
import de.cismet.cids.custom.wunda_blau.search.server.CidsVermessungRissSearchStatement;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.CidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.commons.concurrency.CismetExecutors;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextProvider;

import static de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper.writeExceptionJson;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenJob implements Runnable, ConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(VermessungsunterlagenJob.class);

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Status {

        //~ Enum constants -----------------------------------------------------

        WAITING, WORKING, ERROR, OK
    }

    //~ Instance fields --------------------------------------------------------

    private final ConnectionContext connectionContext;

    @Getter private final String key;
    @Getter private final VermessungsunterlagenAnfrageBean anfrageBean;
    @Getter private Status status = Status.WAITING;
    @Getter private VermessungsunterlagenException exception;
    @Getter private final Map<VermessungsunterlagenTask, Future> taskMap =
        new HashMap<VermessungsunterlagenTask, Future>();
    @Getter private String ftpZipPath;
    @Getter private String webDAVPath;
    private final transient ThreadPoolExecutor taskExecutor = (ThreadPoolExecutor)CismetExecutors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
    private final transient CompletionService<VermessungsunterlagenTask> completionService =
        new ExecutorCompletionService<VermessungsunterlagenTask>(taskExecutor);
    private final transient VermessungsunterlagenHelper helper = VermessungsunterlagenHelper.getInstance();
    private final transient VermessungsunterlagenValidator validator = new VermessungsunterlagenValidator(
            helper,
            getConnectionContext());
    @Getter @Setter private transient CidsBean cidsBean;
    @Getter private final transient Collection<String> allowedTask;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenJobBean object.
     *
     * @param   jobkey             DOCUMENT ME!
     * @param   anfrageBean        DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public VermessungsunterlagenJob(final String jobkey,
            final VermessungsunterlagenAnfrageBean anfrageBean,
            final ConnectionContext connectionContext) throws Exception {
        this.key = jobkey;
        this.anfrageBean = anfrageBean;

        this.allowedTask = helper.getAllowedTasks();
        this.connectionContext = connectionContext;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  status  DOCUMENT ME!
     */
    public void setStatus(final Status status) {
        LOG.info("Job changed (" + getKey() + "): " + status.toString());
        this.status = status;

        try {
            switch (status) {
                case OK: {
                    helper.updateJobCidsBeanStatus(this, Boolean.TRUE);
                }
                break;
                case ERROR: {
                    helper.updateJobCidsBeanStatus(this, Boolean.FALSE);
                }
                break;
                default: {
                    helper.updateJobCidsBeanStatus(this, null);
                }
                break;
            }
        } catch (final Exception ex) {
            LOG.warn("error Updating Status of cidsbean", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   task  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void submitTask(final VermessungsunterlagenTask task) throws Exception {
        if (isTaskAllowed(task.getType())) {
            this.taskMap.put(task, completionService.submit(task));
        } else {
            LOG.info("Not allowed to run task of Type " + task.getType() + ".");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   type  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isTaskAllowed(final String type) {
        return allowedTask.contains(type);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  exception  DOCUMENT ME!
     */
    public void setException(final VermessungsunterlagenException exception) {
        this.exception = exception;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geometry  flurstueckBeans DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Collection<CidsBean> searchRisse(final Geometry geometry) {
        try {
            final Collection<String> schluesselCollection = Arrays.asList(
                    "503",
                    "504",
                    "505",
                    "506",
                    "507",
                    "508");

            final CidsServerSearch serverSearch = new CidsVermessungRissSearchStatement(
                    null,
                    null,
                    null,
                    null,
                    schluesselCollection,
                    geometry,
                    null);
            final Collection<MetaObjectNode> mons = helper.performSearch(serverSearch);
            return helper.loadBeans(mons);
        } catch (final SearchException ex) {
            LOG.error("error while loading risse", ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geometry  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Collection<CidsBean> searchAPs(final Geometry geometry) {
        try {
            final CidsServerSearch serverSearch = new CidsMeasurementPointSearchStatement(
                    "",
                    Arrays.asList(
                        CidsMeasurementPointSearchStatement.Pointtype.AUFNAHMEPUNKTE,
                        CidsMeasurementPointSearchStatement.Pointtype.SONSTIGE_VERMESSUNGSPUNKTE),
                    null,
                    geometry);
            final Collection<MetaObjectNode> mons = helper.performSearch(serverSearch);
            return helper.loadBeans(mons);
        } catch (final SearchException ex) {
            LOG.error("error while searching for APs", ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geometry  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Collection<CidsBean> searchNivPs(final Geometry geometry) {
        try {
            final CidsServerSearch serverSearch = new CidsMeasurementPointSearchStatement(
                    "",
                    Arrays.asList(CidsMeasurementPointSearchStatement.Pointtype.NIVELLEMENT_PUNKTE),
                    null,
                    geometry);
            final Collection<MetaObjectNode> mons = helper.performSearch(serverSearch);
            return helper.loadBeans(mons);
        } catch (final SearchException ex) {
            LOG.error("error while searching for APs", ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   cidsBeans             DOCUMENT ME!
     * @param   intersectionGeometry  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Geometry createGeometryFrom(final Collection<CidsBean> cidsBeans, final Geometry intersectionGeometry)
            throws Exception {
        Geometry geometry = null;
        for (final CidsBean cidsBean : cidsBeans) {
            final Geometry fsGeometry = (Geometry)cidsBean.getProperty("geometrie.geo_field");
            geometry = (geometry == null) ? fsGeometry : geometry.union(fsGeometry);
        }

        if ((geometry != null) && (intersectionGeometry != null)) {
            geometry = geometry.intersection(intersectionGeometry);
        }

        if (geometry != null) {
            geometry.setSRID(VermessungsunterlagenHelper.SRID);
        }
        return geometry;
    }

    /**
     * DOCUMENT ME!
     */
    @Override
    public void run() {
        setStatus(Status.WORKING);
        try {
            if (validator.validateAndGetErrorMessage(anfrageBean)) {
                try {
                    new File(getPath()).mkdirs();

                    final int saum = Integer.parseInt(anfrageBean.getSaumAPSuche());
                    final Geometry vermessungsGeometrie =
                        ((anfrageBean.getAnfragepolygonArray() != null)
                                    && (anfrageBean.getAnfragepolygonArray()[0] != null))
                        ? anfrageBean.getAnfragepolygonArray()[0] : null;

                    final Geometry vermessungsGeometrieSaum;
                    if (vermessungsGeometrie != null) {
                        vermessungsGeometrieSaum = vermessungsGeometrie.buffer(saum);
                        vermessungsGeometrieSaum.setSRID(vermessungsGeometrie.getSRID());
                    } else {
                        vermessungsGeometrieSaum = null;
                    }

                    final Geometry geometryFlurstuecke = createGeometryFrom(validator.getFlurstuecke(),
                            validator.isGeometryFromFlurstuecke() ? null : vermessungsGeometrie);

                    helper.updateJobCidsBeanFlurstueckGeom(this, geometryFlurstuecke);

                    if (!validator.isVermessungsstelleKnown() || validator.isPnrNotZero()) {
                        submitTask(new VermUntTaskPNR(
                                getKey(),
                                anfrageBean.getZulassungsnummerVermessungsstelle(),
                                anfrageBean.getGeschaeftsbuchnummer(),
                                anfrageBean.getPunktnummernreservierungsArray()));
                    }
                    if (!anfrageBean.getNurPunktnummernreservierung()) {
                        if (isTaskAllowed(VermUntTaskAPMap.TYPE) || isTaskAllowed(VermUntTaskAPList.TYPE)
                                    || isTaskAllowed(VermUntTaskAPUebersicht.TYPE)) {
                            if (vermessungsGeometrieSaum != null) {
                                final Collection<CidsBean> aPs = searchAPs(vermessungsGeometrieSaum);
                                if (!aPs.isEmpty()) {
                                    submitTask(new VermUntTaskAPMap(getKey(), aPs));
                                    submitTask(new VermUntTaskAPList(getKey(), aPs));
                                    submitTask(new VermUntTaskAPUebersicht(
                                            getKey(),
                                            aPs,
                                            validator.getFlurstuecke(),
                                            anfrageBean.getGeschaeftsbuchnummer()));
                                }
                            }
                        }

                        if (isTaskAllowed(VermUntTaskNivPBeschreibungen.TYPE)
                                    || isTaskAllowed(VermUntTaskNivPUebersicht.TYPE)) {
                            if (vermessungsGeometrieSaum != null) {
                                final Collection<CidsBean> nivPs = searchNivPs(vermessungsGeometrieSaum);
                                if (!nivPs.isEmpty()) {
                                    submitTask(new VermUntTaskNivPBeschreibungen(getKey(), nivPs));
                                    submitTask(new VermUntTaskNivPUebersicht(
                                            getKey(),
                                            nivPs,
                                            validator.getFlurstuecke(),
                                            anfrageBean.getGeschaeftsbuchnummer()));
                                }
                            }
                        }

                        if (isTaskAllowed(VermUntTaskNasKomplett.TYPE) || isTaskAllowed(VermUntTaskNasPunkte.TYPE)) {
                            final String requestId = getKey();
                            if (vermessungsGeometrie != null) {
                                submitTask(new VermUntTaskNasKomplett(
                                        getKey(),
                                        helper.getUser(),
                                        requestId,
                                        vermessungsGeometrie));
                            }
                            if (vermessungsGeometrieSaum != null) {
                                submitTask(new VermUntTaskNasPunkte(
                                        getKey(),
                                        helper.getUser(),
                                        requestId,
                                        vermessungsGeometrieSaum));
                            }
                        }

                        if (isTaskAllowed(VermUntTaskRisseBilder.TYPE)
                                    || isTaskAllowed(VermUntTaskRisseGrenzniederschrift.TYPE)) {
                            final Collection<CidsBean> risse = searchRisse(geometryFlurstuecke);
                            if (!risse.isEmpty()) {
                                submitTask(new VermUntTaskRisseBilder(
                                        getKey(),
                                        risse,
                                        anfrageBean.getGeschaeftsbuchnummer(),
                                        ""));
                                if (Boolean.TRUE.equals(anfrageBean.getMitGrenzniederschriften())) {
                                    submitTask(new VermUntTaskRisseGrenzniederschrift(
                                            getKey(),
                                            risse,
                                            anfrageBean.getGeschaeftsbuchnummer(),
                                            ""));
                                }
                            }
                        }
                    }

                    int received = 0;
                    while (received < taskMap.size()) {
                        final Future<VermessungsunterlagenTask> resultFuture = completionService.take();
                        final VermessungsunterlagenTask task = resultFuture.get();
                        if (!validator.isIgnoreError()
                                    && VermessungsunterlagenTask.Status.ERROR.equals(task.getStatus())) {
                            throw new VermessungsunterlagenJobException(
                                "Ein unerwarteter Fehler ist beim Ausführen des Tasks "
                                        + task.getType()
                                        + " aufgetreten.\n "
                                        + "Bitte wenden Sie sich an das Geodatenzentrum der Stadt Wuppertal "
                                        + VermessungsunterlagenValidator.CONTACT
                                        + ".",
                                task.getException());
                        }
                        received++;
                    }

                    final String zipFilePath = getPath() + ".zip";
                    final File zipFile = new File(zipFilePath);
                    zipDirectoryTo(zipFile);
                    uploadZip(zipFile);

                    try {
                        helper.updateJobCidsBeanZip(this, zipFilePath);
                    } catch (final Exception ex2) {
                        LOG.error("Error while updating cids bean for " + getKey(), ex2);
                    }

                    setStatus(Status.OK);
                } catch (final Exception ex) {
                    writeExceptionJson(ex, getPath() + "/fehlerprotokoll.json");
                    throw ex;
                } finally {
                    taskExecutor.shutdown();
                }
            }
        } catch (final Exception ex) {
            final VermessungsunterlagenException vermessungsunterlagenException;
            if (ex instanceof VermessungsunterlagenException) {
                vermessungsunterlagenException = (VermessungsunterlagenException)ex;
            } else {
                LOG.error("unexpected error while excecution VermessungsunterlagenJob !", ex);
                vermessungsunterlagenException = new VermessungsunterlagenException(
                        "Ein unerwarter Fehler ist aufgetreten!",
                        ex);
            }
            setException(vermessungsunterlagenException);
            setStatus(Status.ERROR);
            try {
                helper.updateJobCidsBeanException(this, vermessungsunterlagenException);
            } catch (final Exception ex2) {
                LOG.error("Error while updating cids bean for " + getKey(), ex2);
            }
        } finally {
            helper.cleanup(getKey());
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  host   DOCUMENT ME!
     * @param  port   DOCUMENT ME!
     * @param  level  DOCUMENT ME!
     */
    public static void configure4LumbermillOn(final String host, final int port, final String level) {
        final Properties p = new Properties();
        p.put("log4j.appender.Remote", "org.apache.log4j.net.SocketAppender");
        p.put("log4j.appender.Remote.remoteHost", host);
        p.put("log4j.appender.Remote.port", Integer.toString(port));
        p.put("log4j.appender.Remote.locationInfo", "true");
        p.put("log4j.rootLogger", level + ",Remote");
        org.apache.log4j.PropertyConfigurator.configure(p);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   file  DOCUMENT ME!
     *
     * @throws  VermessungsunterlagenException  DOCUMENT ME!
     */
    public void uploadZip(final File file) throws VermessungsunterlagenException {
        if (helper.getProperties().isFtpEnabled()) {
            try {
                this.ftpZipPath = uploadZipToFTP(file);
            } catch (final Exception ex) {
                this.ftpZipPath = null;
                throw new VermessungsunterlagenException("Fehler beim Hochladen der Zip-Datei auf den FTP-Server.", ex);
            }
        }

        if (helper.getProperties().isWebDavEnabled()) {
            try {
                this.webDAVPath = uploadZipToWebDAV(file);
            } catch (final Exception ex) {
                this.webDAVPath = null;
                throw new VermessungsunterlagenException(
                    "Fehler beim Hochladen der Zip-Datei auf den WebDAV-Server.",
                    ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   file  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private static String uploadZipToFTP(final File file) throws Exception {
        final String fileName = file.getName();
        final InputStream inputStream = new FileInputStream(file);

        final String ftpPath = VermessungsunterlagenHelper.getInstance().getProperties().getFtpPath();
        final String ftpZipPath = (ftpPath.isEmpty() ? "" : ("/" + ftpPath)) + fileName;
        VermessungsunterlagenHelper.getInstance().uploadToFTP(inputStream, ftpZipPath);
        return ftpZipPath;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   file  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private static String uploadZipToWebDAV(final File file) throws Exception {
        final String fileName = file.getName();
        final InputStream inputStream = new FileInputStream(file);

        final String webDAVPath = VermessungsunterlagenHelper.getInstance().getProperties().getWebDavPath();
        final String webDAVZipPath = ((webDAVPath.isEmpty() ? "" : ("/" + webDAVPath)) + fileName);
        VermessungsunterlagenHelper.getInstance().uploadToWebDAV(inputStream, webDAVZipPath);
        return webDAVZipPath;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   file  DOCUMENT ME!
     *
     * @throws  VermessungsunterlagenException  Exception DOCUMENT ME!
     */
    private void zipDirectoryTo(final File file) throws VermessungsunterlagenException {
        ZipOutputStream zipOut = null;
        try {
            zipOut = new ZipOutputStream(new FileOutputStream(file));
            addDirectoryToZip("", getPath(), zipOut);
            zipOut.flush();
        } catch (final Exception ex) {
            throw new VermessungsunterlagenException("Fehler beim erzeugen der ZIP-Datei", ex);
        } finally {
            VermessungsunterlagenHelper.closeStream(zipOut);
        }
    }

    /**
     * recursively add files to the zip files.
     *
     * @param   path      DOCUMENT ME!
     * @param   fileName  DOCUMENT ME!
     * @param   zipOut    DOCUMENT ME!
     * @param   flag      DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void addFileToZip(final String path,
            final String fileName,
            final ZipOutputStream zipOut,
            final boolean flag) throws Exception {
        final File file = new File(fileName);

        if (flag == true) {
            zipOut.putNextEntry(new ZipEntry(path + "/" + file.getName() + "/"));
        } else if (file.isDirectory()) {
            addDirectoryToZip(path, fileName, zipOut);
        } else {
            final byte[] buf = new byte[1024];
            int len;
            final FileInputStream in = new FileInputStream(fileName);
            zipOut.putNextEntry(new ZipEntry(path + "/" + file.getName()));
            while ((len = in.read(buf)) > 0) {
                zipOut.write(buf, 0, len);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   path     DOCUMENT ME!
     * @param   dirName  DOCUMENT ME!
     * @param   zipOut   DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void addDirectoryToZip(final String path, final String dirName, final ZipOutputStream zipOut)
            throws Exception {
        final File dir = new File(dirName);

        if (dir.isDirectory()) {
            if (dir.list().length == 0) {
                addFileToZip(path, dirName, zipOut, true);
            } else {
                for (final String filename : dir.list()) {
                    if (path.isEmpty()) {
                        addFileToZip(dir.getName(), dirName + "/" + filename, zipOut, false);
                    } else {
                        addFileToZip(path + "/" + dir.getName(), dirName + "/" + filename, zipOut, false);
                    }
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getPath() {
        return helper.getPath(getKey().replace("/", "--"));
    }
}
