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

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import com.vividsolutions.jts.geom.Geometry;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder;

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
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskNasOhneEigentuemer;
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
    @Getter private final transient VermessungsunterlagenHandler helper;
    @Getter private final transient VermessungsunterlagenValidator validator;
    @Getter @Setter private transient CidsBean cidsBean;
    @Getter private final transient Collection<String> allowedTask;

    @Getter private final User user;
    @Getter private final MetaService metaService;
    @Getter private final VermessungsunterlagenProperties properties;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenJobBean object.
     *
     * @param   jobkey             DOCUMENT ME!
     * @param   anfrageBean        DOCUMENT ME!
     * @param   properties         DOCUMENT ME!
     * @param   user               DOCUMENT ME!
     * @param   metaService        DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public VermessungsunterlagenJob(final String jobkey,
            final VermessungsunterlagenAnfrageBean anfrageBean,
            final VermessungsunterlagenProperties properties,
            final User user,
            final MetaService metaService,
            final ConnectionContext connectionContext) throws Exception {
        this.key = jobkey;
        this.anfrageBean = anfrageBean;
        this.properties = properties;
        this.user = user;
        this.metaService = metaService;
        this.connectionContext = connectionContext;
        this.helper = new VermessungsunterlagenHandler(user, metaService, connectionContext);
        this.validator = new VermessungsunterlagenValidator(helper, connectionContext);
        this.allowedTask = helper.getAllowedTasks();
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
            task.setProperties(getProperties());
            task.setUser(getUser());
            task.setMetaService(getMetaService());
            task.initWithConnectionContext(getConnectionContext());
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
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Collection<CidsBean> searchRisse(final Geometry geometry) throws Exception {
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
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geometry  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Collection<CidsBean> searchAPs(final Geometry geometry) throws Exception {
        final CidsServerSearch serverSearch = new CidsMeasurementPointSearchStatement(
                "",
                Arrays.asList(
                    CidsMeasurementPointSearchStatement.Pointtype.AUFNAHMEPUNKTE,
                    CidsMeasurementPointSearchStatement.Pointtype.SONSTIGE_VERMESSUNGSPUNKTE),
                null,
                geometry);
        final Collection<MetaObjectNode> mons = helper.performSearch(serverSearch);
        return helper.loadBeans(mons);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geometry  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Collection<CidsBean> searchNivPs(final Geometry geometry) throws Exception {
        final CidsServerSearch serverSearch = new CidsMeasurementPointSearchStatement(
                "",
                Arrays.asList(CidsMeasurementPointSearchStatement.Pointtype.NIVELLEMENT_PUNKTE),
                null,
                geometry);
        final Collection<MetaObjectNode> mons = helper.performSearch(serverSearch);
        return helper.loadBeans(mons);
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
            if (cidsBean.getProperty("geometrie") instanceof Geometry) {
                final Geometry fsGeometry = (Geometry)cidsBean.getProperty("geometrie");
                geometry = (geometry == null) ? fsGeometry : geometry.union(fsGeometry);
            } else {
                final Geometry fsGeometry = (Geometry)cidsBean.getProperty("geometrie.geo_field");
                geometry = (geometry == null) ? fsGeometry : geometry.union(fsGeometry);
            }
        }

        if ((geometry != null) && (intersectionGeometry != null)) {
            geometry = geometry.intersection(intersectionGeometry);
        }

        if (geometry != null) {
            geometry.setSRID(VermessungsunterlagenUtils.SRID);
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
            final VermessungsunterlagenAnfrageBean anfrageBean = getAnfrageBean();
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
                        if (anfrageBean.isMitPunktnummernreservierung()) {
                            submitTask(new VermUntTaskPNR(
                                    getKey(),
                                    anfrageBean.getZulassungsnummerVermessungsstelle(),
                                    anfrageBean.getGeschaeftsbuchnummer(),
                                    anfrageBean.getPunktnummernreservierungsArray()));
                        }
                    }

                    final Collection<CidsBean> aPs;
                    {
                        if ((vermessungsGeometrieSaum != null)
                                    && (anfrageBean.isMitAPKarten()
                                        || anfrageBean.isMitAPBeschreibungen()
                                        || anfrageBean.isMitAPUebersichten())
                                    && (isTaskAllowed(VermUntTaskAPMap.TYPE)
                                        || isTaskAllowed(VermUntTaskAPList.TYPE)
                                        || isTaskAllowed(VermUntTaskAPUebersicht.TYPE))) {
                            aPs = searchAPs(vermessungsGeometrieSaum);
                        } else {
                            aPs = null;
                        }
                    }

                    final Collection<CidsBean> nivPs;
                    {
                        if ((vermessungsGeometrieSaum != null)
                                    && (anfrageBean.isMitNIVPBeschreibungen()
                                        || anfrageBean.isMitNIVPUebersichten())
                                    && (isTaskAllowed(VermUntTaskNivPBeschreibungen.TYPE)
                                        || isTaskAllowed(VermUntTaskNivPUebersicht.TYPE))) {
                            nivPs = searchNivPs(vermessungsGeometrieSaum);
                        } else {
                            nivPs = null;
                        }
                    }

                    final Collection<CidsBean> risse;
                    {
                        if ((anfrageBean.isMitRisse() && isTaskAllowed(VermUntTaskRisseBilder.TYPE))
                                    || ((anfrageBean.isMitGrenzniederschriften()
                                            && isTaskAllowed(VermUntTaskRisseGrenzniederschrift.TYPE))
                                        && (geometryFlurstuecke != null))) {
                            risse = searchRisse(geometryFlurstuecke);
                        } else {
                            risse = null;
                        }
                    }

                    if ((aPs != null) && !aPs.isEmpty()) {
                        if (anfrageBean.isMitAPKarten()) {
                            submitTask(new VermUntTaskAPMap(getKey(), aPs));
                        }
                        if (anfrageBean.isMitAPBeschreibungen()) {
                            submitTask(new VermUntTaskAPList(getKey(), aPs));
                        }
                        if (anfrageBean.isMitAPUebersichten()) {
                            submitTask(new VermUntTaskAPUebersicht(
                                    getKey(),
                                    aPs,
                                    validator.getFlurstuecke(),
                                    anfrageBean.getGeschaeftsbuchnummer()));
                        }
                    }

                    if ((nivPs != null) && !nivPs.isEmpty()) {
                        if (anfrageBean.isMitNIVPBeschreibungen()) {
                            submitTask(new VermUntTaskNivPBeschreibungen(getKey(), nivPs));
                        }
                        if (anfrageBean.isMitNIVPUebersichten()) {
                            submitTask(new VermUntTaskNivPUebersicht(
                                    getKey(),
                                    nivPs,
                                    validator.getFlurstuecke(),
                                    anfrageBean.getGeschaeftsbuchnummer()));
                        }
                    }

                    final String requestId = getKey();
                    if (anfrageBean.isMitAlkisBestandsdatenmitEigentuemerinfo() && (vermessungsGeometrie != null)) {
                        submitTask(new VermUntTaskNasKomplett(
                                getKey(),
                                requestId,
                                vermessungsGeometrie,
                                Boolean.TRUE.equals(anfrageBean.getAnonymousOrder())));
                    }
                    if (anfrageBean.isMitAlkisBestandsdatennurPunkte() && (vermessungsGeometrieSaum != null)) {
                        submitTask(new VermUntTaskNasPunkte(
                                getKey(),
                                requestId,
                                vermessungsGeometrieSaum,
                                Boolean.TRUE.equals(anfrageBean.getAnonymousOrder())));
                    }
                    if (anfrageBean.isMitAlkisBestandsdatenohneEigentuemerinfo()
                                && isTaskAllowed(VermUntTaskNasOhneEigentuemer.TYPE)) {
                        submitTask(new VermUntTaskNasOhneEigentuemer(
                                getKey(),
                                requestId,
                                vermessungsGeometrieSaum,
                                Boolean.TRUE.equals(anfrageBean.getAnonymousOrder())));
                    }

                    if ((risse != null) && !risse.isEmpty()) {
                        if (anfrageBean.isMitRisse()) {
                            submitTask(new VermUntTaskRisseBilder(
                                    getKey(),
                                    risse,
                                    anfrageBean.getGeschaeftsbuchnummer(),
                                    ""));
                        }
                        if (anfrageBean.isMitGrenzniederschriften()) {
                            submitTask(new VermUntTaskRisseGrenzniederschrift(
                                    getKey(),
                                    risse,
                                    anfrageBean.getGeschaeftsbuchnummer(),
                                    ""));
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
                    VermessungsunterlagenUtils.writeExceptionJson(ex, getPath() + "/fehlerprotokoll.json");
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
        final ConfigurationBuilder<BuiltConfiguration> builder = new DefaultConfigurationBuilder<>();

        builder.setStatusLevel(Level.WARN);
        builder.setConfigurationName("DynamicConfig");

        // Define appenders
        final AppenderComponentBuilder socketAppender = builder.newAppender("Remote", "Socket")
                    .addAttribute("host", host)
                    .addAttribute("port", port);
        socketAppender.add(builder.newLayout("JsonLayout"));
        builder.add(socketAppender);

        // Define root logger
        final RootLoggerComponentBuilder rootLogger = builder.newRootLogger(level);

        builder.add(rootLogger);

        // Build and apply the configuration
        final LoggerContext ctx = (LoggerContext)LogManager.getContext(false);
        final Configuration conf = builder.build();
        ctx.start(conf);
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
    private String uploadZipToFTP(final File file) throws Exception {
        final String fileName = file.getName();
        final InputStream inputStream = new FileInputStream(file);

        final String ftpPath = getHelper().getProperties().getFtpPath();
        final String ftpZipPath = (ftpPath.isEmpty() ? "" : ("/" + ftpPath)) + fileName;
        new VermessungsunterlagenFtpHelper().uploadToFTP(inputStream, ftpZipPath);
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
    private String uploadZipToWebDAV(final File file) throws Exception {
        final String fileName = file.getName();
        final InputStream inputStream = new FileInputStream(file);

        final String webDAVPath = getHelper().getProperties().getWebDavPath();
        final String webDAVZipPath = ((webDAVPath.isEmpty() ? "" : ("/" + webDAVPath)) + fileName);
        new VermessungsunterlagenWebdavHelper().uploadToWebDAV(inputStream, webDAVZipPath);
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
        try(final ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file))) {
            addDirectoryToZip("", getPath(), zipOut);
            zipOut.flush();
        } catch (final Exception ex) {
            throw new VermessungsunterlagenException("Fehler beim erzeugen der ZIP-Datei", ex);
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
        return getProperties().getPath(getKey());
    }
}
