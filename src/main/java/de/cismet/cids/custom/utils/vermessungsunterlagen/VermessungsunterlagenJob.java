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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

import static de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper.FTP_PATH;
import static de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper.writeExceptionJson;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenJob implements Runnable {

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

    @Getter private final String key;

    @Getter private final VermessungsunterlagenAnfrageBean anfrageBean;

    @Getter private Status status = Status.WAITING;

    @Getter private VermessungsunterlagenException exception;

    @Getter private final Map<VermessungsunterlagenTask, Future> taskMap =
        new HashMap<VermessungsunterlagenTask, Future>();

    @Getter private String ftpZipPath;

    private final transient ThreadPoolExecutor taskExecutor = (ThreadPoolExecutor)CismetExecutors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    private final transient CompletionService<VermessungsunterlagenTask> completionService =
        new ExecutorCompletionService<VermessungsunterlagenTask>(taskExecutor);

    private final transient VermessungsunterlagenHelper helper = VermessungsunterlagenHelper.getInstance();

    private final transient VermessungsunterlagenValidator validator = new VermessungsunterlagenValidator(helper);

    @Getter @Setter private transient CidsBean cidsBean;

    @Getter private transient Collection<String> allowedTask;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenJobBean object.
     *
     * @param   jobkey       DOCUMENT ME!
     * @param   anfrageBean  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public VermessungsunterlagenJob(final String jobkey, final VermessungsunterlagenAnfrageBean anfrageBean)
            throws Exception {
        this.key = jobkey;
        this.anfrageBean = anfrageBean;

        this.allowedTask = helper.getAllowedTasks();
    }

    //~ Methods ----------------------------------------------------------------

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
     * @param   cidsBeans  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Geometry createGeometryFrom(final Collection<CidsBean> cidsBeans) throws Exception {
        final Collection<Polygon> polygons = new ArrayList<Polygon>(cidsBeans.size());
        for (final CidsBean cidsBean : cidsBeans) {
            final Polygon polygon = (Polygon)cidsBean.getProperty("geometrie.geo_field");
            polygons.add(polygon);
        }
        final GeometryFactory geometryFactory = new GeometryFactory();
        final MultiPolygon geometry = geometryFactory.createMultiPolygon(polygons.toArray(new Polygon[0]));
        geometry.setSRID(VermessungsunterlagenHelper.SRID);
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
                    final Geometry vermessungsGeometrie = getAnfrageBean().getAnfragepolygonArray()[0];
                    final Geometry vermessungsGeometrieSaum = vermessungsGeometrie.buffer(saum);
                    vermessungsGeometrieSaum.setSRID(vermessungsGeometrie.getSRID());
                    final Geometry geometryFlurstuecke = createGeometryFrom(validator.getFlurstuecke());

                    helper.updateJobCidsBeanFlurstueckGeom(this, geometryFlurstuecke);

                    if (anfrageBean.getNurPunktnummernreservierung()) {
                        submitTask(new VermUntTaskPNR(
                                getKey(),
                                "PortalTest_"
                                        + anfrageBean.getGeschaeftsbuchnummer(),
                                anfrageBean.getPunktnummernreservierungsArray()));
                    } else {
                        if (isTaskAllowed(VermUntTaskAPMap.TYPE) || isTaskAllowed(VermUntTaskAPList.TYPE)) {
                            final Collection<CidsBean> saumAPs = searchAPs(vermessungsGeometrieSaum);
                            if (!saumAPs.isEmpty()) {
                                submitTask(new VermUntTaskAPMap(getKey(), saumAPs));
                                submitTask(new VermUntTaskAPList(getKey(), saumAPs));
                            }
                        }
                        if (isTaskAllowed(VermUntTaskAPUebersicht.TYPE)) {
                            final Collection<CidsBean> fsAPs = searchAPs(geometryFlurstuecke);
                            if (!fsAPs.isEmpty()) {
                                submitTask(new VermUntTaskAPUebersicht(
                                        getKey(),
                                        fsAPs,
                                        validator.getFlurstuecke(),
                                        anfrageBean.getGeschaeftsbuchnummer()));
                            }
                        }

                        if (isTaskAllowed(VermUntTaskAPMap.TYPE) || isTaskAllowed(VermUntTaskAPList.TYPE)) {
                            final Collection<CidsBean> saumNivPs = searchNivPs(vermessungsGeometrieSaum);
                            if (!saumNivPs.isEmpty()) {
                                submitTask(new VermUntTaskNivPBeschreibungen(getKey(), saumNivPs));
                            }
                        }
                        if (isTaskAllowed(VermUntTaskAPUebersicht.TYPE)) {
                            final Collection<CidsBean> fsNivPs = searchNivPs(geometryFlurstuecke);
                            if (!fsNivPs.isEmpty()) {
                                submitTask(new VermUntTaskNivPUebersicht(
                                        getKey(),
                                        fsNivPs,
                                        validator.getFlurstuecke(),
                                        anfrageBean.getGeschaeftsbuchnummer()));
                            }
                        }

                        if (isTaskAllowed(VermUntTaskNasKomplett.TYPE) || isTaskAllowed(VermUntTaskNasPunkte.TYPE)) {
                            final String requestId = getKey();
                            submitTask(new VermUntTaskNasKomplett(
                                    getKey(),
                                    helper.getUser(),
                                    requestId,
                                    vermessungsGeometrie));
                            submitTask(new VermUntTaskNasPunkte(
                                    getKey(),
                                    helper.getUser(),
                                    requestId,
                                    vermessungsGeometrieSaum));
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
                        if (!validator.ignoreError()
                                    && VermessungsunterlagenTask.Status.ERROR.equals(task.getStatus())) {
                            throw new VermessungsunterlagenException(
                                "Ein unerwarteter Fehler ist beim Ausführen des Tasks "
                                        + task.getType()
                                        + " aufgetreten.",
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
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   file  DOCUMENT ME!
     *
     * @throws  VermessungsunterlagenException  DOCUMENT ME!
     */
    public void uploadZip(final File file) throws VermessungsunterlagenException {
        this.ftpZipPath = null;
        try {
            final String ftpZipPath = (FTP_PATH.isEmpty() ? "" : ("/" + FTP_PATH)) + file.getName();
            VermessungsunterlagenHelper.getInstance().uploadToFTP(new FileInputStream(file), ftpZipPath);
            this.ftpZipPath = ftpZipPath;
        } catch (final Exception ex) {
            throw new VermessungsunterlagenException("Fehler beim Hochladen der Zip-Datei.", ex);
        }
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
        return VermessungsunterlagenHelper.getPath(getKey());
    }
}
