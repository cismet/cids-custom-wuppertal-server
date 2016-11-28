/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.vermessungsunterlagen;

/**
 * *************************************************
 *
 * cismet GmbH, Saarbruecken, Germany
 *
 * ... and it just works.
 *
 ***************************************************
 */
import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.LightweightMetaObject;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

import org.openide.util.Lookup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import java.net.URL;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.alkis.AlkisProductDescription;
import de.cismet.cids.custom.utils.alkis.AlkisProducts;
import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;
import de.cismet.cids.custom.utils.nas.NasProduct;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.CidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cids.utils.MetaClassCacheService;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.commons.concurrency.CismetExecutors;

import de.cismet.commons.security.AccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import static org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenHelper {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(VermessungsunterlagenHelper.class);

    private static final int MAX_BUFFER_SIZE = 1024;
    private static final ObjectMapper EXCEPTION_MAPPER = new ObjectMapper();
    private static final ObjectMapper JOB_MAPPER;

    public static final String DIR_PREFIX = "VermUnterlagen";

    public static final String ALLOWED_TASKS_CONFIG_ATTR = "vup.tasks_allowed";
    public static final int SRID = 25832;

    static {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JOB_MAPPER = mapper;
    }

    public static final NasProduct NAS_PRODUCT_KOMPLETT;
    public static final NasProduct NAS_PRODUCT_PUNKTE;

    static {
        final ObjectMapper mapper = new ObjectMapper();
        NasProduct productPunkte = null;
        NasProduct productKomplett = null;
        final ArrayList<NasProduct> nasProducts;
        try {
            nasProducts = mapper.readValue(ServerResourcesLoader.getInstance().loadStringReader(
                        WundaBlauServerResources.NAS_PRODUCT_DESCRIPTION_JSON.getValue()),
                    mapper.getTypeFactory().constructCollectionType(List.class, NasProduct.class));
            for (final NasProduct nasProduct : nasProducts) {
                if ("punkte".equals(nasProduct.getKey())) {
                    productPunkte = nasProduct;
                } else if ("komplett".equals(nasProduct.getKey())) {
                    productKomplett = nasProduct;
                }
            }
        } catch (final Exception ex) {
            final String message = "could not load NasProducts";
            LOG.error(message, ex);
            throw new RuntimeException(message, ex);
        }
        NAS_PRODUCT_PUNKTE = productPunkte;
        NAS_PRODUCT_KOMPLETT = productKomplett;
    }

    //~ Instance fields --------------------------------------------------------

    private MetaClass mc_VERMESSUNGSUNTERLAGENAUFTRAG;
    private MetaClass mc_VERMESSUNGSUNTERLAGENAUFTRAG_FLURSTUECK;
    private MetaClass mc_VERMESSUNGSUNTERLAGENAUFTRAG_PUNKTNUMMER;
    private MetaClass mc_VERMESSUNGSUNTERLAGENAUFTRAG_VERMESSUNGSART;
    private MetaClass mc_GEOM;

    private final Map<String, VermessungsunterlagenJob> jobMap = new HashMap<String, VermessungsunterlagenJob>();

    private MetaService metaService;
    private User user;
    private final VermessungsunterlagenProperties vermessungsunterlagenProperties;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenHelper object.
     */
    private VermessungsunterlagenHelper() {
        Properties properties = null;
        try {
            properties = ServerResourcesLoader.getInstance()
                        .loadProperties(WundaBlauServerResources.VERMESSUNGSUNTERLAGENPORTAL_PROPERTIES.getValue());
        } catch (final Exception ex) {
            LOG.error("VermessungsunterlagenHelper could not load the properties", ex);
        }
        this.vermessungsunterlagenProperties = new VermessungsunterlagenProperties(properties);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public VermessungsunterlagenProperties getProperties() {
        return vermessungsunterlagenProperties;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   metaService  DOCUMENT ME!
     * @param   user         DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void init(final MetaService metaService, final User user) throws Exception {
        this.metaService = metaService;
        this.user = user;

        this.mc_GEOM = getMetaService().getClassByTableName(getUser(), "geom");
        this.mc_VERMESSUNGSUNTERLAGENAUFTRAG_PUNKTNUMMER = getMetaService()
                    .getClassByTableName(getUser(), "vermessungsunterlagenauftrag_punktnummer");
        this.mc_VERMESSUNGSUNTERLAGENAUFTRAG = getMetaService().getClassByTableName(
                getUser(),
                "vermessungsunterlagenauftrag");
        this.mc_VERMESSUNGSUNTERLAGENAUFTRAG_VERMESSUNGSART = getMetaService()
                    .getClassByTableName(getUser(), "vermessungsunterlagenauftrag_vermessungsart");
        this.mc_VERMESSUNGSUNTERLAGENAUFTRAG_FLURSTUECK = getMetaService()
                    .getClassByTableName(getUser(), "vermessungsunterlagenauftrag_flurstueck");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   attribute   DOCUMENT ME!
     * @param   parentNode  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String getString(final String attribute, final JsonNode parentNode) {
        final JsonNode tmpNode = parentNode.get(attribute);
        return ((tmpNode != null) && (tmpNode.get("$value") != null)) ? tmpNode.get("$value").asText() : null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   attribute   DOCUMENT ME!
     * @param   parentNode  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static Boolean getBoolean(final String attribute, final JsonNode parentNode) {
        final JsonNode tmpNode = parentNode.get(attribute);
        return ((tmpNode != null) && (tmpNode.get("$value") != null)) ? tmpNode.get("$value").asBoolean() : null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   attribute   DOCUMENT ME!
     * @param   parentNode  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static Integer getInteger(final String attribute, final JsonNode parentNode) {
        final JsonNode tmpNode = parentNode.get(attribute);
        return ((tmpNode != null) && (tmpNode.get("$value") != null)) ? tmpNode.get("$value").asInt() : null;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private FTPClient getConnectedFTPClient() throws Exception {
        final FTPClient ftpClient = new FTPClient();
        ftpClient.connect(vermessungsunterlagenProperties.getFtpHost());

        final int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            throw new Exception("Exception in connecting to FTP Server");
        }
        ftpClient.login(vermessungsunterlagenProperties.getFtpLogin(), vermessungsunterlagenProperties.getFtpPass());
        return ftpClient;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   in           DOCUMENT ME!
     * @param   ftpFilePath  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void uploadToFTP(final InputStream in, final String ftpFilePath) throws Exception {
        final FTPClient connectedFtpClient = getConnectedFTPClient();
        connectedFtpClient.enterLocalPassiveMode();
        connectedFtpClient.setFileType(BINARY_FILE_TYPE);
        connectedFtpClient.storeFile(ftpFilePath, in);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   ftpFilePath  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public InputStream downloadFromFTP(final String ftpFilePath) throws Exception {
        final FTPClient connectedFtpClient = getConnectedFTPClient();
        connectedFtpClient.enterLocalPassiveMode();
        connectedFtpClient.setFileType(BINARY_FILE_TYPE);
        return connectedFtpClient.retrieveFileStream(ftpFilePath);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   executeJobContent  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String createJob(final String executeJobContent) {
        try {
            final String jobKey = generateUniqueJobKey();
            final VermessungsunterlagenAnfrageBean anfrageBean = createAnfrageBean(executeJobContent);
            final VermessungsunterlagenJob job = new VermessungsunterlagenJob(jobKey, anfrageBean);
            try {
                persistJobCidsBean(job, executeJobContent);
                CismetExecutors.newSingleThreadExecutor().execute(job);
            } catch (final Exception ex) {
                LOG.info("error while persisting Job", ex);
                job.setStatus(VermessungsunterlagenJob.Status.ERROR);
                job.setException(new VermessungsunterlagenException(
                        "Der Datensatz konnte nicht abgespeichert werden.",
                        ex));
            }
            jobMap.put(jobKey, job);
            return jobKey;
        } catch (final Exception ex) {
            LOG.error("Unexpected error while creating job !", ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   lwmo  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public CidsBean loadCidsBean(final LightweightMetaObject lwmo) throws Exception {
        return getMetaService().getMetaObject(getUser(), lwmo.getObjectID(), lwmo.getClassID()).getBean();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private String generateUniqueJobKey() throws Exception {
        String jobKey;
        do {
            jobKey = RandomStringUtils.randomAlphanumeric(8);
        } while (isJobKeyAlreadyExisting(jobKey));
        return jobKey;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   jobKey  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private boolean isJobKeyAlreadyExisting(final String jobKey) throws Exception {
        if (jobMap.containsKey(jobKey)) { // exists in memory ?
            return true;
        } else {                          // exists in database ?
            final List result = getMetaService().performCustomSearch("SELECT schluessel FROM "
                            + mc_VERMESSUNGSUNTERLAGENAUFTRAG + " WHERE schluessel LIKE '" + jobKey + "'");
            return !result.isEmpty();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  jobKey  DOCUMENT ME!
     */
    public void cleanUp(final String jobKey) {
        jobMap.remove(jobKey);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   jobkey  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public VermessungsunterlagenJob getJob(final String jobkey) {
        if (jobMap.containsKey(jobkey)) {
            return jobMap.get(jobkey);
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   job                anfrageBean DOCUMENT ME!
     * @param   executeJobContent  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void persistJobCidsBean(final VermessungsunterlagenJob job, final String executeJobContent)
            throws Exception {
        final VermessungsunterlagenAnfrageBean anfrageBean = job.getAnfrageBean();

        final Polygon[] aparr = anfrageBean.getAnfragepolygonArray();
        final Geometry geometry = ((aparr != null) && (aparr.length > 0)) ? aparr[0] : null;
        final CidsBean geomBean;
        if (geometry != null) {
            geometry.setSRID(SRID);
            geomBean = CidsBean.createNewCidsBeanFromTableName("WUNDA_BLAU", mc_GEOM.getTableName());
            geomBean.setProperty("geo_field", geometry);
        } else {
            geomBean = null;
        }

        final CidsBean jobCidsBean = CidsBean.createNewCidsBeanFromTableName(
                "WUNDA_BLAU",
                mc_VERMESSUNGSUNTERLAGENAUFTRAG.getTableName());
        jobCidsBean.setProperty("executejob_json", executeJobContent);
        jobCidsBean.setProperty("schluessel", job.getKey());
        jobCidsBean.setProperty("geometrie", geomBean);
        jobCidsBean.setProperty(
            "geometrie_flurstuecke",
            CidsBean.createNewCidsBeanFromTableName("WUNDA_BLAU", mc_GEOM.getTableName()));
        jobCidsBean.setProperty("aktenzeichen", anfrageBean.getAktenzeichenKatasteramt());
        for (final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean flurstueckBean
                    : anfrageBean.getAntragsflurstuecksArray()) {
            final CidsBean flurstueck = CidsBean.createNewCidsBeanFromTableName(
                    "WUNDA_BLAU",
                    mc_VERMESSUNGSUNTERLAGENAUFTRAG_FLURSTUECK.getTableName());
            flurstueck.setProperty("gemarkung", flurstueckBean.getGemarkungsID());
            flurstueck.setProperty("flur", flurstueckBean.getFlurID());
            flurstueck.setProperty("flurstueck", flurstueckBean.getFlurstuecksID());
            jobCidsBean.getBeanCollectionProperty("flurstuecke").add(flurstueck);
        }
        for (final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean pnrBean
                    : anfrageBean.getPunktnummernreservierungsArray()) {
            final CidsBean pnr = CidsBean.createNewCidsBeanFromTableName(
                    "WUNDA_BLAU",
                    mc_VERMESSUNGSUNTERLAGENAUFTRAG_PUNKTNUMMER.getTableName());
            pnr.setProperty("anzahl", pnrBean.getAnzahlPunktnummern());
            pnr.setProperty("katasteramt", pnrBean.getKatasteramtsID());
            pnr.setProperty("kilometerquadrat", pnrBean.getUtmKilometerQuadrat());
            jobCidsBean.getBeanCollectionProperty("punktnummern").add(pnr);
        }
        jobCidsBean.setProperty("mit_grenzniederschriften", anfrageBean.getMitGrenzniederschriften());
        jobCidsBean.setProperty("geschaeftsbuchnummer", anfrageBean.getGeschaeftsbuchnummer());
        jobCidsBean.setProperty("auftragsnummer", anfrageBean.getKatasteramtAuftragsnummer());
        jobCidsBean.setProperty("katasteramtsid", anfrageBean.getKatasteramtsId());
        jobCidsBean.setProperty("vermessungsstelle", anfrageBean.getZulassungsnummerVermessungsstelle());
        jobCidsBean.setProperty("nur_punktnummernreservierung", anfrageBean.getNurPunktnummernreservierung());
        try {
            jobCidsBean.setProperty("saumap", Integer.parseInt(anfrageBean.getSaumAPSuche()));
        } catch (final Exception ex) {
            // validation will fail. Need to be catched so that the object can be persisted.
            // The validation exception will be stored in the exception_json field later on.
            // That's why the exception can be ignored here.
        }
        for (final String art : anfrageBean.getArtderVermessung()) {
            final CidsBean pnr = CidsBean.createNewCidsBeanFromTableName(
                    "WUNDA_BLAU",
                    mc_VERMESSUNGSUNTERLAGENAUFTRAG_VERMESSUNGSART.getTableName());
            pnr.setProperty("name", art);
            jobCidsBean.getBeanCollectionProperty("vermessungsarten").add(pnr);
        }
        jobCidsBean.setProperty("timestamp", new Timestamp(new Date().getTime()));
        jobCidsBean.setProperty("tasks", Arrays.toString(getAllowedTasks().toArray()));

        job.setCidsBean(getMetaService().insertMetaObject(getUser(), jobCidsBean.getMetaObject()).getBean());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public Collection<String> getAllowedTasks() throws Exception {
        final String rawAllowedTasks = DomainServerImpl.getServerInstance()
                    .getConfigAttr(getUser(), ALLOWED_TASKS_CONFIG_ATTR);
        final Collection<String> allowedTasks = new ArrayList<String>();
        if (rawAllowedTasks != null) {
            for (final String allowedTask : Arrays.asList(rawAllowedTasks.split("\n"))) {
                if (allowedTask != null) {
                    allowedTasks.add(allowedTask.trim());
                }
            }
        }
        return allowedTasks;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   job           DOCUMENT ME!
     * @param   zipDateiname  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public final void updateJobCidsBeanZip(final VermessungsunterlagenJob job, final String zipDateiname)
            throws Exception {
        final CidsBean jobCidsBean = job.getCidsBean();

        jobCidsBean.setProperty("zip_pfad", zipDateiname);
        jobCidsBean.setProperty("zip_timestamp", new Timestamp(new Date().getTime()));

        getMetaService().updateMetaObject(getUser(), jobCidsBean.getMetaObject());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   job     DOCUMENT ME!
     * @param   status  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public final void updateJobCidsBeanStatus(final VermessungsunterlagenJob job, final Boolean status)
            throws Exception {
        final CidsBean jobCidsBean = job.getCidsBean();

        jobCidsBean.setProperty("status", status);
        jobCidsBean.setProperty("status_timestamp", new Timestamp(new Date().getTime()));

        getMetaService().updateMetaObject(getUser(), jobCidsBean.getMetaObject());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   job   DOCUMENT ME!
     * @param   geom  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public final void updateJobCidsBeanFlurstueckGeom(final VermessungsunterlagenJob job, final Geometry geom)
            throws Exception {
        final CidsBean jobCidsBean = job.getCidsBean();

        jobCidsBean.setProperty("geometrie_flurstuecke.geo_field", geom);

        getMetaService().updateMetaObject(getUser(), jobCidsBean.getMetaObject());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   job  DOCUMENT ME!
     * @param   ex   DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public final void updateJobCidsBeanException(final VermessungsunterlagenJob job, final Exception ex)
            throws Exception {
        final CidsBean jobCidsBean = job.getCidsBean();

        jobCidsBean.setProperty("exception_json", EXCEPTION_MAPPER.writeValueAsString(ex));
        jobCidsBean.setProperty("exception_timestamp", new Timestamp(new Date().getTime()));

        getMetaService().updateMetaObject(getUser(), jobCidsBean.getMetaObject());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   vermessungsart  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public CidsBean selectOrCreateVermessungArt(final String vermessungsart) throws Exception {
        final CidsBean bean = CidsBean.createNewCidsBeanFromTableName(
                "WUNDA_BLAU",
                mc_VERMESSUNGSUNTERLAGENAUFTRAG_VERMESSUNGSART.getTableName());
        getMetaService().getMetaObject(getUser(), "SELECT FROM ");
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   json  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public static VermessungsunterlagenAnfrageBean createAnfrageBean(final String json) throws IOException {
        final JsonNode rootNode = JOB_MAPPER.readTree(json);
        final JsonNode in0 = rootNode.get("in0");

        final VermessungsunterlagenAnfrageBean anfrageBean = new VermessungsunterlagenAnfrageBean();
        anfrageBean.setAktenzeichenKatasteramt(getString("aktenzeichenKatasteramt", in0));

        final Collection<Polygon> anfragepolygonList = new ArrayList<Polygon>();
        final JsonNode anfragepolygonArrayNode = in0.get("anfragepolygonArray").get("anfragepolygonArray");

        if (anfragepolygonArrayNode != null) {
            final JsonNode polygonArrayNode = anfragepolygonArrayNode.get("polygon").get("polygon");
            if (polygonArrayNode.isArray()) {
                final GeometryFactory geometryFactory = new GeometryFactory();
                final Collection<Coordinate> coordinates = new ArrayList<Coordinate>(polygonArrayNode.size());
                for (final JsonNode objNode : polygonArrayNode) {
                    final Double x = objNode.get("polygon").get(0).get("$value").asDouble();
                    final Double y = objNode.get("polygon").get(1).get("$value").asDouble();
                    coordinates.add(new Coordinate(x, y));
                }
                final LinearRing ring = new LinearRing(new CoordinateArraySequence(
                            coordinates.toArray(new Coordinate[0])),
                        geometryFactory);
                final Polygon anfragepolygon = geometryFactory.createPolygon(ring, new LinearRing[0]);
                anfragepolygon.setSRID(SRID);
                anfragepolygonList.add(anfragepolygon);
            }
        }
        anfrageBean.setAnfragepolygonArray(anfragepolygonList.toArray(new Polygon[0]));

        final Collection<VermessungsunterlagenAnfrageBean.AntragsflurstueckBean> antragsflurstueckList =
            new ArrayList<VermessungsunterlagenAnfrageBean.AntragsflurstueckBean>();
        final JsonNode antragsflurstuecksArrayNode = in0.get("antragsflurstuecksArray").get("antragsflurstuecksArray");
        if (antragsflurstuecksArrayNode != null) {
            if (antragsflurstuecksArrayNode.isArray()) {
                for (final JsonNode objNode : antragsflurstuecksArrayNode) {
                    final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean antragsflurstueckBean =
                        new VermessungsunterlagenAnfrageBean.AntragsflurstueckBean();
                    antragsflurstueckBean.setFlurID(getString("flurID", objNode));
                    antragsflurstueckBean.setFlurstuecksID(getString("flurstuecksID", objNode));
                    antragsflurstueckBean.setGemarkungsID(getString("gemarkungsID", objNode));
                    antragsflurstueckList.add(antragsflurstueckBean);
                }
            } else {
                final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean antragsflurstueckBean =
                    new VermessungsunterlagenAnfrageBean.AntragsflurstueckBean();
                antragsflurstueckBean.setFlurID(getString("flurID", antragsflurstuecksArrayNode));
                antragsflurstueckBean.setFlurstuecksID(getString("flurstuecksID", antragsflurstuecksArrayNode));
                antragsflurstueckBean.setGemarkungsID(getString("gemarkungsID", antragsflurstuecksArrayNode));
                antragsflurstueckList.add(antragsflurstueckBean);
            }
        }
        anfrageBean.setAntragsflurstuecksArray(antragsflurstueckList.toArray(
                new VermessungsunterlagenAnfrageBean.AntragsflurstueckBean[0]));

        final Collection<String> artderVermessungList = new ArrayList<String>();
        final JsonNode artderVermessungNode = in0.get("artderVermessung");
        if ((artderVermessungNode != null)) {
            if (artderVermessungNode.isArray()) {
                for (final JsonNode objNode : artderVermessungNode) {
                    artderVermessungList.add(getString("artderVermessung", objNode));
                }
            } else {
                artderVermessungList.add(getString("artderVermessung", artderVermessungNode));
            }
        }
        anfrageBean.setArtderVermessung(artderVermessungList.toArray(new String[0]));

        anfrageBean.setGeschaeftsbuchnummer(getString("geschaeftsbuchnummer", in0));
        anfrageBean.setKatasteramtAuftragsnummer(getString("katasteramtAuftragsnummer", in0));
        anfrageBean.setKatasteramtsId(getString("katasteramtsId", in0));
        anfrageBean.setMitGrenzniederschriften(getBoolean("mitGrenzniederschriften", in0));
        anfrageBean.setNameVermessungsstelle(getString("nameVermessungsstelle", in0));
        anfrageBean.setNurPunktnummernreservierung(getBoolean("nurPunktnummernreservierung", in0));
        anfrageBean.setSaumAPSuche(getString("saumAPSuche", in0));

        final Collection<VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean> punktnummernreservierungList =
            new ArrayList<VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean>();
        final JsonNode punktnummernreservierungsArrayNode = in0.get("punktnummernreservierungsArray")
                    .get("punktnummernreservierungsArray");
        if ((punktnummernreservierungsArrayNode != null) && punktnummernreservierungsArrayNode.isArray()) {
            for (final JsonNode objNode : punktnummernreservierungsArrayNode) {
                final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean punktnummernreservierungBean =
                    new VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean();
                punktnummernreservierungBean.setAnzahlPunktnummern(getInteger("anzahlPunktnummern", objNode));
                punktnummernreservierungBean.setKatasteramtsID(getString("katasteramtsID", objNode));
                punktnummernreservierungBean.setUtmKilometerQuadrat(getString("utmKilometerQuadrat", objNode));
                punktnummernreservierungList.add(punktnummernreservierungBean);
            }
        }
        anfrageBean.setPunktnummernreservierungsArray(punktnummernreservierungList.toArray(
                new VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean[0]));

        anfrageBean.setZulassungsnummerVermessungsstelle(getString("zulassungsnummerVermessungsstelle", in0));

        return anfrageBean;
    }

    /**
     * DOCUMENT ME!
     */
    public void test() {
        try {
            if ((vermessungsunterlagenProperties.getAbsPathTest() != null)
                        && !vermessungsunterlagenProperties.getAbsPathTest().isEmpty()) {
                final File directory = new File(vermessungsunterlagenProperties.getAbsPathTest());
                if (directory.exists()) {
                    final File[] executeJobFiles = directory.listFiles(new FilenameFilter() {

                                @Override
                                public boolean accept(final File dir, final String name) {
                                    return name.startsWith("executeJob.") && name.endsWith(".json");
                                }
                            });

                    for (final File executeJobFile : executeJobFiles) {
                        new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    try {
                                        if (LOG.isDebugEnabled()) {
                                            LOG.debug("----");
                                            LOG.debug("Path: " + executeJobFile.getAbsolutePath());
                                        }

                                        final String executeJobContent = IOUtils.toString(
                                                new FileInputStream(executeJobFile));
                                        if (LOG.isDebugEnabled()) {
                                            LOG.debug("Content: " + executeJobContent);
                                        }

                                        final String jobkey = createJob(executeJobContent);
                                        LOG.info("Job created: " + jobkey);
                                    } catch (final Exception ex) {
                                        LOG.error(ex, ex);
                                    }
                                }
                            }).start();
                    }
                }
            }
        } catch (final Exception ex) {
            LOG.error(ex, ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        try {
            configLog4J();

            final File directory = new File(new VermessungsunterlagenHelper().vermessungsunterlagenProperties
                            .getAbsPathTest());
            final File[] executeJobFiles = directory.listFiles(new FilenameFilter() {

                        @Override
                        public boolean accept(final File dir, final String name) {
                            return name.startsWith("executeJob.") && name.endsWith(".json");
                                // return name.equals("executeJob.2016-09-20T12:22:21.783Z.1988.json");
                        }
                    });

            for (final File executeJobFile : executeJobFiles) {
                LOG.info("----");
                LOG.info("Path: " + executeJobFile.getAbsolutePath());

                final String executeJobContent = IOUtils.toString(new FileInputStream(executeJobFile));
                LOG.info("Content: " + executeJobContent);

                final VermessungsunterlagenAnfrageBean anfrageBean = createAnfrageBean(executeJobContent);

////import com.bedatadriven.jackson.datatype.jts.JtsModule;
///*
//        <dependency>
//            <groupId>com.bedatadriven</groupId>
//            <artifactId>jackson-datatype-jts</artifactId>
//            <version>2.3</version>
//        </dependency>
//*/
//
//                final ObjectMapper mapper = new ObjectMapper();
//                mapper.registerModule(new JtsModule());
//                LOG.info("Created object: " + mapper.writeValueAsString(anfrageBean));
//                LOG.info("----");
            }
        } catch (final Exception ex) {
            LOG.error(ex, ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public final MetaService getMetaService() {
        return metaService;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public final User getUser() {
        return user;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   jobkey  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getPath(final String jobkey) {
        return vermessungsunterlagenProperties.getAbsPathTmp() + "/" + DIR_PREFIX + "_" + jobkey;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  ex       DOCUMENT ME!
     * @param  filname  DOCUMENT ME!
     */
    public static void writeExceptionJson(final Exception ex, final String filname) {
        try {
            final File file = new File(filname);
            EXCEPTION_MAPPER.writeValue(file, ex);
        } catch (final IOException ex1) {
            LOG.error(filname + " could not be written !", ex1);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  in  DOCUMENT ME!
     */
    public static void closeStream(final InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (final Exception ex) {
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  out  DOCUMENT ME!
     */
    public static void closeStream(final OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (final Exception ex) {
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   serverSearch  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public Collection performSearch(final CidsServerSearch serverSearch) throws SearchException {
        final Map localServers = new HashMap<String, Remote>();
        localServers.put("WUNDA_BLAU", getMetaService());
        serverSearch.setActiveLocalServers(localServers);
        serverSearch.setUser(getUser());

        return (Collection<MetaObjectNode>)serverSearch.performServerSearch();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   mons  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<CidsBean> loadBeans(final Collection<MetaObjectNode> mons) {
        if (mons != null) {
            final Collection<CidsBean> beans = new ArrayList<CidsBean>(mons.size());
            for (final MetaObjectNode mon : mons) {
                if (mon != null) {
                    try {
                        final MetaObject mo = getMetaService().getMetaObject(
                                getUser(),
                                mon.getObjectId(),
                                mon.getClassId());
                        mo.setAllClasses(
                            ((MetaClassCacheService)Lookup.getDefault().lookup(MetaClassCacheService.class))
                                        .getAllClasses(mo.getDomain()));
                        beans.add(mo.getBean());
                    } catch (final RemoteException ex) {
                        LOG.warn("error while loading AP: OID:" + mon.getObjectId() + ", GID: " + mon.getClassId(), ex);
                    }
                }
            }
            return beans;
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   jasperReport  reportResourceName DOCUMENT ME!
     * @param   parameters    DOCUMENT ME!
     * @param   dataSource    DOCUMENT ME!
     * @param   outputStream  filename DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static void jasperReportDownload(final JasperReport jasperReport,
            final Map parameters,
            final JRDataSource dataSource,
            final OutputStream outputStream) throws Exception {
        final JasperPrint print = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        JasperExportManager.exportReportToPdfStream(print, outputStream);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   url               DOCUMENT ME!
     * @param   requestParameter  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static InputStream doPostRequest(final URL url, final Reader requestParameter) throws Exception {
        return
            new SimpleHttpAccessHandler().doRequest(
                url,
                requestParameter,
                AccessHandler.ACCESS_METHODS.POST_REQUEST,
                AlkisProducts.POST_HEADER);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   url  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static InputStream doGetRequest(final URL url) throws Exception {
        return new SimpleHttpAccessHandler().doRequest(url);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   src   DOCUMENT ME!
     * @param   dest  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public static void downloadStream(final InputStream src, final OutputStream dest) throws IOException {
        boolean downloading = true;
        while (downloading) {
            // Size buffer according to how much of the file is left to download.
            final byte[] buffer;
            buffer = new byte[MAX_BUFFER_SIZE];

            // Read from server into buffer.
            final int read = src.read(buffer);
            if (read == -1) {
                downloading = false;
            } else {
                // Write buffer to file.
                dest.write(buffer, 0, read);
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    private static void configLog4J() {
        final Properties p = new Properties();
        p.put("log4j.appender.Remote", "org.apache.log4j.net.SocketAppender");
        p.put("log4j.appender.Remote.remoteHost", "localhost");
        p.put("log4j.appender.Remote.port", Integer.toString(4445));
        p.put("log4j.appender.Remote.locationInfo", "true");
        p.put("log4j.rootLogger", "DEBUG,Remote");
        org.apache.log4j.PropertyConfigurator.configure(p);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static VermessungsunterlagenHelper getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   box     DOCUMENT ME!
     * @param   width   DOCUMENT ME!
     * @param   height  DOCUMENT ME!
     * @param   scale   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static boolean doesBoundingBoxFitIntoLayout(final Envelope box,
            final int width,
            final int height,
            final double scale) {
        final double realWorldLayoutWidth = ((double)width) / 1000.0d * scale;
        final double realWorldLayoutHeigth = ((double)height) / 1000.0d * scale;
        return (realWorldLayoutWidth >= box.getWidth()) && (realWorldLayoutHeigth >= box.getHeight());
    }
    /**
     * DOCUMENT ME!
     *
     * @param   clazz        DOCUMENT ME!
     * @param   type         DOCUMENT ME!
     * @param   boundingBox  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static AlkisProductDescription determineAlkisProduct(final String clazz,
            final String type,
            final Envelope boundingBox) {
        AlkisProductDescription minimalWidthFittingProduct = null;
        AlkisProductDescription minimalHeightFittingProduct = null;
        AlkisProductDescription defaultProduct = null;
        for (final AlkisProductDescription product : ServerAlkisProducts.getInstance().ALKIS_MAP_PRODUCTS) {
            if (clazz.equals(product.getClazz()) && type.equals(product.getType())) {
                if (product.isDefaultProduct()) {
                    defaultProduct = product;
                }
                final boolean fitting = doesBoundingBoxFitIntoLayout(
                        boundingBox,
                        product.getWidth(),
                        product.getHeight(),
                        Integer.parseInt(String.valueOf(product.getMassstab())));
                if (fitting) {
                    if (minimalWidthFittingProduct == null) {
                        // at least the first is the minimal
                        minimalWidthFittingProduct = product;
                    } else if (product.getWidth() <= minimalWidthFittingProduct.getWidth()) {
                        // is smaller or equals
                        if (product.getWidth() < minimalWidthFittingProduct.getWidth()) {
                            // is smaller
                            minimalWidthFittingProduct = product;
                        } else if (Integer.parseInt(String.valueOf(product.getMassstab()))
                                    < Integer.parseInt(String.valueOf(minimalHeightFittingProduct.getMassstab()))) {
                            // not smaller (equals) in size but in scale
                            minimalWidthFittingProduct = product;
                        }
                    }
                    // same as for width but now with height
                    if (minimalHeightFittingProduct == null) {
                        minimalHeightFittingProduct = product;
                    } else if (product.getHeight() <= minimalHeightFittingProduct.getHeight()) {
                        if (product.getHeight() < minimalHeightFittingProduct.getHeight()) {
                            minimalHeightFittingProduct = product;
                        } else if (Integer.parseInt(String.valueOf(product.getMassstab()))
                                    < Integer.parseInt(String.valueOf(minimalHeightFittingProduct.getMassstab()))) {
                            minimalHeightFittingProduct = product;
                        }
                    }
                }
            }
        }

        if ((minimalWidthFittingProduct != null) && (minimalHeightFittingProduct != null)) {
            final boolean isMinimalWidthHoch = minimalWidthFittingProduct.getWidth()
                        < minimalWidthFittingProduct.getHeight();
            final boolean isMinimalHeightHoch = minimalHeightFittingProduct.getWidth()
                        < minimalHeightFittingProduct.getHeight();

            // hochkannt priorisieren
            if (isMinimalWidthHoch && isMinimalHeightHoch) {
                return minimalWidthFittingProduct;
            } else if (isMinimalWidthHoch) {
                return minimalWidthFittingProduct;
            } else {
                return minimalHeightFittingProduct;
            }
        } else if (minimalWidthFittingProduct != null) {
            return minimalWidthFittingProduct;
        } else if (minimalHeightFittingProduct != null) {
            return minimalHeightFittingProduct;
        } else {
            return defaultProduct;
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final VermessungsunterlagenHelper INSTANCE = new VermessungsunterlagenHelper();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
