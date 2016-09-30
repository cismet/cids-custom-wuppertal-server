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
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.vividsolutions.jts.geom.Coordinate;
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
import net.sf.jasperreports.engine.util.JRLoader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.cismet.cids.custom.utils.nas.NasProduct;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskAPList;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskAPMap;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskNasKomplett;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskNasPunkte;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskRisseBilder;
import de.cismet.cids.custom.utils.vermessungsunterlagen.tasks.VermUntTaskRisseGrenzniederschrift;
import de.cismet.cids.custom.wunda_blau.search.server.CidsMeasurementPointSearchStatement;
import de.cismet.cids.custom.wunda_blau.search.server.CidsVermessungRissSearchStatement;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.CidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.commons.concurrency.CismetExecutors;

import de.cismet.commons.security.AccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.tools.PropertyReader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenHelper {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(VermessungsunterlagenHelper.class);
    private static final String PROPERTIES =
        "/de/cismet/cids/custom/wunda_blau/res/vermessungsunterlagen/vermessungsunterlagen_conf.properties";
    private static final int MAX_BUFFER_SIZE = 1024;
    private static final int SRID = 25832;
    private static final ObjectMapper EXCEPTION_MAPPER = new ObjectMapper();

    private static final String PATH_TMP;

    static {
        String pathTmp = "/tmp";
        try {
            final PropertyReader serviceProperties = new PropertyReader(PROPERTIES);
            pathTmp = serviceProperties.getProperty("PATH_TMP");
        } catch (final Exception ex) {
            LOG.warn("could not read " + PROPERTIES + ". setting PATH_TMP to default value: " + pathTmp, ex);
        } finally {
            PATH_TMP = pathTmp;
        }
    }

    private static final ObjectMapper MAPPER;

    static {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER = mapper;
    }

    public static final NasProduct NAS_PRODUCT_KOMPLETT;
    public static final NasProduct NAS_PRODUCT_PUNKTE;

    static {
        final ObjectMapper mapper = new ObjectMapper();
        NasProduct productPunkte = null;
        NasProduct productKomplett = null;
        final ArrayList<NasProduct> nasProducts;
        try {
            nasProducts = mapper.readValue(VermessungsunterlagenHelper.class.getResourceAsStream(
                        "/de/cismet/cids/custom/nas/nasProductDescription.json"),
                    mapper.getTypeFactory().constructCollectionType(List.class, NasProduct.class));
            for (final NasProduct nasProduct : nasProducts) {
                if ("punkte".equals(nasProduct.getKey())) {
                    productPunkte = nasProduct;
                } else if ("komplett".equals(nasProduct.getKey())) {
                    productKomplett = nasProduct;
                }
            }
        } catch (IOException ex) {
            LOG.error(ex, ex);
        }
        NAS_PRODUCT_PUNKTE = productPunkte;
        NAS_PRODUCT_KOMPLETT = productKomplett;
    }

    private static final String PATH_TEST = "/home/jruiz/tmp";

    //~ Instance fields --------------------------------------------------------

    private final Map<String, VermessungsunterlagenJob> jobMap = new HashMap<String, VermessungsunterlagenJob>();

    private final MetaService metaService;
    private final User user;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenHelper object.
     *
     * @param  metaService  DOCUMENT ME!
     * @param  user         DOCUMENT ME!
     */
    public VermessungsunterlagenHelper(final MetaService metaService, final User user) {
        this.metaService = metaService;
        this.user = user;
    }

    //~ Methods ----------------------------------------------------------------

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
     * @param   anfrageContent  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String createJob(final String anfrageContent) {
        try {
            final VermessungsunterlagenAnfrageBean anfrageBean = createAnfrageBean(anfrageContent);

//            final ObjectMapper mapper = new ObjectMapper();
//            mapper.registerModule(new JtsModule());
//            LOG.info("Created object: " + mapper.writeValueAsString(anfrageBean));
//            LOG.info("----");

            final String jobkey = createJob(anfrageBean);
            return jobkey;
        } catch (final Exception ex) {
            LOG.warn("error while creating job", ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   anfrageBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String createJob(final VermessungsunterlagenAnfrageBean anfrageBean) {
        final String jobkey = generateUniqueJobKey();

        final VermessungsunterlagenJob job = new VermessungsunterlagenJob(jobkey, anfrageBean);
        persistJob(job);

        try {
            if (new VermessungsunterlagenValidator(this).validateAndGetErrorMessage(anfrageBean)) {
                if (!anfrageBean.getNurPunktnummernreservierung()) {

                    final Geometry geometry = anfrageBean.getAnfragepolygonArray()[0];
                    final int saum = Integer.parseInt(anfrageBean.getSaumAPSuche());
                    final Geometry geometrySaum = geometry.buffer(saum);
                    geometrySaum.setSRID(geometry.getSRID());

                    final Collection<CidsBean> apBeans = getAPs(geometrySaum);
                    final Collection<VermessungsunterlagenAnfrageBean.AntragsflurstueckBean> flurstuecke = Arrays
                                .asList(
                                    anfrageBean.getAntragsflurstuecksArray());

                    final Collection<CidsBean> risseBeans = getRisse(flurstuecke);

                    job.addTask(new VermUntTaskNasKomplett(jobkey, getUser(), geometry));
                    job.addTask(new VermUntTaskNasPunkte(jobkey, getUser(), geometry));
                    job.addTask(new VermUntTaskAPMap(jobkey, apBeans));
                    job.addTask(new VermUntTaskAPList(jobkey, apBeans));
                    job.addTask(new VermUntTaskRisseBilder(jobkey, risseBeans));
                    job.addTask(new VermUntTaskRisseGrenzniederschrift(jobkey, risseBeans));

                    CismetExecutors.newSingleThreadExecutor().execute(job);
                }

                jobMap.put(jobkey, job);
            }
        } catch (final Exception ex) {
            LOG.info(ex.getMessage());
            job.setStatus(VermessungsunterlagenJob.Status.ERROR);
            job.setException(ex);
        }

        return jobkey;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String generateUniqueJobKey() {
        String jobKey;
        do {
            jobKey = RandomStringUtils.randomAlphanumeric(8);
        } while (jobMap.containsKey(jobKey));
        return jobKey;
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
     * @param  job  DOCUMENT ME!
     */
    private void persistJob(final VermessungsunterlagenJob job) {
        // TODO
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
        final JsonNode rootNode = MAPPER.readTree(json);
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
        if ((antragsflurstuecksArrayNode != null) && antragsflurstuecksArrayNode.isArray()) {
            for (final JsonNode objNode : antragsflurstuecksArrayNode) {
                final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean antragsflurstueckBean =
                    new VermessungsunterlagenAnfrageBean.AntragsflurstueckBean();
                antragsflurstueckBean.setFlurID(getString("flurID", objNode));
                antragsflurstueckBean.setFlurstuecksID(getString("flurstuecksID", objNode));
                antragsflurstueckBean.setGemarkungsID(getString("gemarkungsID", objNode));
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
            final File directory = new File(PATH_TEST);
            final File[] executeJobFiles = directory.listFiles(new FilenameFilter() {

                        @Override
                        public boolean accept(final File dir, final String name) {
                            return name.startsWith("executeJob.") && name.endsWith(".json");
                                // return name.equals("executeJob.2016-09-20T12:22:21.783Z.1988.json");
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

                                final String executeJobContent = IOUtils.toString(new FileInputStream(executeJobFile));
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Content: " + executeJobContent);
                                }

                                final String jobkey = createJob(executeJobContent);
                                LOG.info("Job created: " + jobkey);
                            } catch (IOException ex) {
                                LOG.error(ex, ex);
                            }
                        }
                    }).start();
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

            final File directory = new File(PATH_TEST);
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

                final VermessungsunterlagenAnfrageBean executeJobBean = createAnfrageBean(executeJobContent);

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
//                LOG.info("Created object: " + mapper.writeValueAsString(executeJobBean));
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
    public MetaService getMetaService() {
        return metaService;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public User getUser() {
        return user;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geometry  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Collection<CidsBean> getAPs(final Geometry geometry) {
        try {
            final Collection<CidsMeasurementPointSearchStatement.Pointtype> pointtypes = Arrays.asList(
                    CidsMeasurementPointSearchStatement.Pointtype.AUFNAHMEPUNKTE,
                    CidsMeasurementPointSearchStatement.Pointtype.SONSTIGE_VERMESSUNGSPUNKTE
                    // CidsMeasurementPointSearchStatement.Pointtype.GRENZPUNKTE,
                    // CidsMeasurementPointSearchStatement.Pointtype.BESONDERE_GEBAEUDEPUNKTE,
                    // CidsMeasurementPointSearchStatement.Pointtype.BESONDERE_BAUWERKSPUNKTE,
                    // CidsMeasurementPointSearchStatement.Pointtype.BESONDERE_TOPOGRAPHISCHE_PUNKTE,
                    // CidsMeasurementPointSearchStatement.Pointtype.NIVELLEMENT_PUNKTE
                    );

            final CidsServerSearch serverSearch = new CidsMeasurementPointSearchStatement(
                    "",
                    pointtypes,
                    null,
                    geometry);
            final Collection<MetaObjectNode> mons = performSearch(serverSearch);
            return loadBeans(mons);
        } catch (final SearchException ex) {
            LOG.error("error while searching for APs", ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   jobkey  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String getPath(final String jobkey) {
        return PATH_TMP + "/" + jobkey;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  ex       DOCUMENT ME!
     * @param  filname  DOCUMENT ME!
     */
    public static void writeExceptionJson(final Exception ex, final String filname) {
        try {
            EXCEPTION_MAPPER.writeValue(new File(filname), ex);
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
     * @param   flurstueckBeans  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Collection<CidsBean> getRisse(
            final Collection<VermessungsunterlagenAnfrageBean.AntragsflurstueckBean> flurstueckBeans) {
        try {
            final Collection<String> schluesselCollection = Arrays.asList(
                    "503",
                    "504",
                    "505",
                    "506",
                    "507",
                    "508");

            final Collection<Map<String, String>> flurstuecke = new LinkedList<Map<String, String>>();

            for (final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean flurstueckBean : flurstueckBeans) {
                final String[] split = flurstueckBean.getFlurstuecksID().split("/");
                final String zaehler = split[0];
                final String nenner = (split.length == 1) ? null : split[1];
                final Map<String, String> flurstueckMap = new HashMap<String, String>();
                flurstueckMap.put(
                    CidsVermessungRissSearchStatement.FLURSTUECK_GEMARKUNG,
                    flurstueckBean.getGemarkungsID());
                flurstueckMap.put(
                    CidsVermessungRissSearchStatement.FLURSTUECK_FLUR,
                    flurstueckBean.getFlurID());
                flurstueckMap.put(
                    CidsVermessungRissSearchStatement.FLURSTUECK_ZAEHLER,
                    zaehler);
                flurstueckMap.put(
                    CidsVermessungRissSearchStatement.FLURSTUECK_NENNER,
                    nenner);
                flurstuecke.add(flurstueckMap);
            }

            final CidsServerSearch serverSearch = new CidsVermessungRissSearchStatement(
                    null,
                    null,
                    null,
                    null,
                    schluesselCollection,
                    null,
                    flurstuecke);
            final Collection<MetaObjectNode> mons = performSearch(serverSearch);
            return loadBeans(mons);
        } catch (SearchException ex) {
            LOG.error("error while loading risse", ex);
            return null;
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
    private Collection<CidsBean> loadBeans(final Collection<MetaObjectNode> mons) {
        if (mons != null) {
            final Collection<CidsBean> beans = new ArrayList<CidsBean>(mons.size());
            for (final MetaObjectNode mon : mons) {
                if (mon != null) {
                    try {
                        beans.add(getMetaService().getMetaObject(getUser(), mon.getObjectId(), mon.getClassId())
                                    .getBean());
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
     * @param   reportResourceName  DOCUMENT ME!
     * @param   parameters          DOCUMENT ME!
     * @param   dataSource          DOCUMENT ME!
     * @param   outputStream        filename DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static void jasperReportDownload(final String reportResourceName,
            final Map parameters,
            final JRDataSource dataSource,
            final OutputStream outputStream) throws Exception {
        final JasperReport jasperReport = (JasperReport)JRLoader.loadObject(VermessungsunterlagenHelper.class
                        .getResourceAsStream(
                            reportResourceName));
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
        return new SimpleHttpAccessHandler().doRequest(
                url,
                requestParameter,
                AccessHandler.ACCESS_METHODS.POST_REQUEST);
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
}
