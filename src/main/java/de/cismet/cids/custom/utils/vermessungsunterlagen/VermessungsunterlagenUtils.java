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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import de.cismet.cids.custom.utils.alkis.AlkisProductDescription;
import de.cismet.cids.custom.utils.alkis.AlkisProducts;
import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;

import de.cismet.commons.security.AccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenUtils {

    //~ Static fields/initializers ---------------------------------------------

    public static final int SRID = 25832;
    private static final ObjectMapper EXCEPTION_MAPPER = new ObjectMapper();

    private static final transient Logger LOG = Logger.getLogger(VermessungsunterlagenUtils.class);

    private static final ObjectMapper JOB_MAPPER;

    static {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JOB_MAPPER = mapper;
    }

    private static final int MAX_BUFFER_SIZE = 1024;

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   ex  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static String getExceptionJson(final Exception ex) throws Exception {
        return VermessungsunterlagenUtils.EXCEPTION_MAPPER.writeValueAsString(ex);
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
     * @param   polygonArrayNode  DOCUMENT ME!
     * @param   wrapped           DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static Polygon createAnfragepolygon(final JsonNode polygonArrayNode, final boolean wrapped) {
        final GeometryFactory geometryFactory = new GeometryFactory();
        final Collection<Coordinate> coordinates = new ArrayList<>(polygonArrayNode.size());
        for (final JsonNode objNode : polygonArrayNode) {
            final Double x = (wrapped ? objNode.get("polygon").get(0).get("$value") : objNode.get("x")).asDouble();
            final Double y = (wrapped ? objNode.get("polygon").get(1).get("$value") : objNode.get("y")).asDouble();
            coordinates.add(new Coordinate(x, y));
        }
        final LinearRing ring = new LinearRing(new CoordinateArraySequence(
                    coordinates.toArray(new Coordinate[0])),
                geometryFactory);
        final Polygon anfragepolygon = geometryFactory.createPolygon(ring, new LinearRing[0]);
        anfragepolygon.setSRID(SRID);
        return anfragepolygon;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   objNode  DOCUMENT ME!
     * @param   wrapped  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean createPunktnummernreservierungBean(
            final JsonNode objNode,
            final boolean wrapped) {
        final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean punktnummernreservierungBean =
            new VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean();
        punktnummernreservierungBean.setAnzahlPunktnummern(getInteger("anzahlPunktnummern", objNode, wrapped));
        punktnummernreservierungBean.setKatasteramtsID(getString("katasteramtsID", objNode, wrapped));
        punktnummernreservierungBean.setUtmKilometerQuadrat(getString("utmKilometerQuadrat", objNode, wrapped));
        return punktnummernreservierungBean;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   tmpNode  DOCUMENT ME!
     * @param   wrapped  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static JsonNode getValueNode(final JsonNode tmpNode, final boolean wrapped) {
        if (tmpNode == null) {
            return null;
        }
        return wrapped ? (((tmpNode.get("$value") != null)) ? tmpNode.get("$value") : null) : tmpNode;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   tmpNode  DOCUMENT ME!
     * @param   wrapped  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String getString(final JsonNode tmpNode, final boolean wrapped) {
        final JsonNode valueNode = getValueNode(tmpNode, wrapped);
        return (valueNode != null) ? valueNode.asText() : null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   attribute   DOCUMENT ME!
     * @param   parentNode  DOCUMENT ME!
     * @param   wrapped     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String getString(final String attribute, final JsonNode parentNode, final boolean wrapped) {
        final JsonNode tmpNode = parentNode.get(attribute);
        return getString(tmpNode, wrapped);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   attribute   DOCUMENT ME!
     * @param   parentNode  DOCUMENT ME!
     * @param   wrapped     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static Boolean getBoolean(final String attribute, final JsonNode parentNode, final boolean wrapped) {
        final JsonNode tmpNode = parentNode.get(attribute);
        final JsonNode valueNode = getValueNode(tmpNode, wrapped);
        return (valueNode != null) ? valueNode.asBoolean() : null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   attribute   DOCUMENT ME!
     * @param   parentNode  DOCUMENT ME!
     * @param   wrapped     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static Integer getInteger(final String attribute, final JsonNode parentNode, final boolean wrapped) {
        final JsonNode tmpNode = parentNode.get(attribute);
        final JsonNode valueNode = getValueNode(tmpNode, wrapped);
        return (valueNode != null) ? valueNode.asInt() : null;
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
        final VermessungsunterlagenAnfrageBean anfrageBean = new VermessungsunterlagenAnfrageBean();
        anfrageBean.setPortalVersion(VermessungsunterlagenUtils.getString("portalVersion", rootNode, false));
        final boolean wrapped = !anfrageBean.isNewPortalVersion();
        if (wrapped) { // is not 2.1.0
            final JsonNode in0 = rootNode.get("in0");

            final Collection<Polygon> anfragepolygonList = new ArrayList<>();
            final JsonNode anfragepolygonArrayNode = in0.get("anfragepolygonArray").get("anfragepolygonArray");
            if (anfragepolygonArrayNode != null) {
                final JsonNode polygonArrayNode = anfragepolygonArrayNode.get("polygon").get("polygon");
                if (polygonArrayNode.isArray()) {
                    anfragepolygonList.add(VermessungsunterlagenUtils.createAnfragepolygon(polygonArrayNode, wrapped));
                }
            }

            final Collection<VermessungsunterlagenAnfrageBean.AntragsflurstueckBean> antragsflurstueckList =
                new ArrayList<>();
            final JsonNode antragsflurstuecksArrayNode = in0.get("antragsflurstuecksArray")
                        .get("antragsflurstuecksArray");
            if (antragsflurstuecksArrayNode != null) {
                if (antragsflurstuecksArrayNode.isArray()) {
                    for (final JsonNode objNode : antragsflurstuecksArrayNode) {
                        final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean antragsflurstueckBean =
                            createAntragsflurstueckBean(objNode, wrapped);
                        antragsflurstueckList.add(antragsflurstueckBean);
                    }
                } else {
                    final JsonNode objNode = antragsflurstuecksArrayNode;
                    final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean antragsflurstueckBean =
                        createAntragsflurstueckBean(objNode, wrapped);
                    antragsflurstueckList.add(antragsflurstueckBean);
                }
            }

            final Collection<String> artderVermessungList = new ArrayList<>();
            final JsonNode artderVermessungNode = in0.get("artderVermessung").get("artderVermessung");
            if ((artderVermessungNode != null)) {
                if (artderVermessungNode.isArray()) {
                    for (final JsonNode objNode : artderVermessungNode) {
                        artderVermessungList.add(getString(objNode, wrapped));
                    }
                } else {
                    final JsonNode objNode = artderVermessungNode;
                    artderVermessungList.add(getString(objNode, wrapped));
                }
            }

            final Collection<VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean> punktnummernreservierungList =
                new ArrayList<>();
            final JsonNode punktnummernreservierungsArrayNode = in0.get("punktnummernreservierungsArray")
                        .get("punktnummernreservierungsArray");
            if ((punktnummernreservierungsArrayNode != null)) {
                if (punktnummernreservierungsArrayNode.isArray()) {
                    for (final JsonNode objNode : punktnummernreservierungsArrayNode) {
                        final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean punktnummernreservierungBean =
                            createPunktnummernreservierungBean(objNode, wrapped);
                        punktnummernreservierungList.add(punktnummernreservierungBean);
                    }
                } else {
                    final JsonNode objNode = punktnummernreservierungsArrayNode;
                    final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean punktnummernreservierungBean =
                        createPunktnummernreservierungBean(objNode, wrapped);
                    punktnummernreservierungList.add(punktnummernreservierungBean);
                }
            }

            anfrageBean.setAnfragepolygonArray(anfragepolygonList.toArray(new Polygon[0]));
            anfrageBean.setAntragsflurstuecksArray(antragsflurstueckList.toArray(
                    new VermessungsunterlagenAnfrageBean.AntragsflurstueckBean[0]));
            anfrageBean.setArtderVermessung(artderVermessungList.toArray(new String[0]));
            anfrageBean.setPunktnummernreservierungsArray(punktnummernreservierungList.toArray(
                    new VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean[0]));
            anfrageBean.setAktenzeichenKatasteramt(getString("aktenzeichenKatasteramt", in0, wrapped));
            anfrageBean.setAnonymousOrder(null);
            anfrageBean.setGeschaeftsbuchnummer(getString("geschaeftsbuchnummer", in0, wrapped));
            anfrageBean.setKatasteramtAuftragsnummer(getString("katasteramtAuftragsnummer", in0, wrapped));
            anfrageBean.setKatasteramtsId(getString("katasteramtsId", in0, wrapped));
            anfrageBean.setNameVermessungsstelle(getString("nameVermessungsstelle", in0, wrapped));
            anfrageBean.setZulassungsnummerVermessungsstelle(getString(
                    "zulassungsnummerVermessungsstelle",
                    in0,
                    wrapped));
            anfrageBean.setSaumAPSuche(getString("saumAPSuche", in0, wrapped));
            final boolean nurPNR = Boolean.TRUE.equals(getBoolean("nurPunktnummernreservierung", in0, wrapped));
            anfrageBean.setMitAPBeschreibungen(!nurPNR);
            anfrageBean.setMitAPKarten(!nurPNR);
            anfrageBean.setMitAPUebersichten(!nurPNR);
            anfrageBean.setMitNIVPBeschreibungen(!nurPNR);
            anfrageBean.setMitNIVPUebersichten(!nurPNR);
            anfrageBean.setMitAlkisBestandsdatenmitEigentuemerinfo(!nurPNR);
            anfrageBean.setMitAlkisBestandsdatenohneEigentuemerinfo(false);
            anfrageBean.setMitAlkisBestandsdatennurPunkte(!nurPNR);
            anfrageBean.setMitRisse(!nurPNR);
            anfrageBean.setMitGrenzniederschriften(getBoolean("mitGrenzniederschriften", in0, wrapped));
            anfrageBean.setMitPunktnummernreservierung(!punktnummernreservierungList.isEmpty());
        } else {
            final JsonNode datenSatzNode = rootNode.get("AntragsdatensatzBean");

            final Collection<Polygon> anfragepolygonList = new ArrayList<>();
            final JsonNode anfragepolygonArrayNode = datenSatzNode.get("antragsPolygone");
            if ((anfragepolygonArrayNode != null) && anfragepolygonArrayNode.isArray()) {
                for (final JsonNode anfragepolygonNode : anfragepolygonArrayNode) {
                    anfragepolygonList.add(VermessungsunterlagenUtils.createAnfragepolygon(
                            anfragepolygonNode.get("points"),
                            wrapped));
                }
            }

            final Collection<VermessungsunterlagenAnfrageBean.AntragsflurstueckBean> antragsflurstueckList =
                new ArrayList<>();
            final JsonNode antragsflurstuecksArrayNode = datenSatzNode.get("antragsflurstuecke");
            if (antragsflurstuecksArrayNode != null) {
                if (antragsflurstuecksArrayNode.isArray()) {
                    for (final JsonNode objNode : antragsflurstuecksArrayNode) {
                        final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean antragsflurstueckBean =
                            createAntragsflurstueckBean(objNode, wrapped);
                        antragsflurstueckList.add(antragsflurstueckBean);
                    }
                } else {
                    final JsonNode objNode = antragsflurstuecksArrayNode;
                    final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean antragsflurstueckBean =
                        createAntragsflurstueckBean(objNode, wrapped);
                    antragsflurstueckList.add(antragsflurstueckBean);
                }
            }

            final Collection<String> artderVermessungList = new ArrayList<>();
            final JsonNode artderVermessungNode = datenSatzNode.get("artderVermessung");
            if ((artderVermessungNode != null)) {
                if (artderVermessungNode.isArray()) {
                    for (final JsonNode objNode : artderVermessungNode) {
                        artderVermessungList.add(getString(objNode, wrapped));
                    }
                } else {
                    final JsonNode objNode = artderVermessungNode;
                    artderVermessungList.add(getString(objNode, wrapped));
                }
            }

            final Collection<VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean> punktnummernreservierungList =
                new ArrayList<>();
            final JsonNode punktnummernreservierungsArrayNode = datenSatzNode.get("punktnummernreservierungen");
            if ((punktnummernreservierungsArrayNode != null)) {
                if (punktnummernreservierungsArrayNode.isArray()) {
                    for (final JsonNode objNode : punktnummernreservierungsArrayNode) {
                        final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean punktnummernreservierungBean =
                            createPunktnummernreservierungBean(objNode, wrapped);
                        punktnummernreservierungList.add(punktnummernreservierungBean);
                    }
                } else {
                    final JsonNode objNode = punktnummernreservierungsArrayNode;
                    final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean punktnummernreservierungBean =
                        createPunktnummernreservierungBean(objNode, wrapped);
                    punktnummernreservierungList.add(punktnummernreservierungBean);
                }
            }

            anfrageBean.setAnfragepolygonArray(anfragepolygonList.toArray(new Polygon[0]));
            anfrageBean.setAntragsflurstuecksArray(antragsflurstueckList.toArray(
                    new VermessungsunterlagenAnfrageBean.AntragsflurstueckBean[0]));
            anfrageBean.setArtderVermessung(artderVermessungList.toArray(new String[0]));
            anfrageBean.setPunktnummernreservierungsArray(punktnummernreservierungList.toArray(
                    new VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean[0]));
            anfrageBean.setAktenzeichenKatasteramt(getString("aktenzeichenKatasteramt", datenSatzNode, wrapped));
            anfrageBean.setAnonymousOrder(getBoolean("anonymousOrder", datenSatzNode, wrapped));
            anfrageBean.setGeschaeftsbuchnummer(getString("geschaeftsbuchnummer", datenSatzNode, wrapped));
            anfrageBean.setKatasteramtAuftragsnummer(getString("katasteramtAuftragsnummer", datenSatzNode, wrapped));
            anfrageBean.setKatasteramtsId(getString("katasteramtsId", datenSatzNode, wrapped));
            anfrageBean.setNameVermessungsstelle(getString("nameVermessungsstelle", datenSatzNode, wrapped));
            anfrageBean.setZulassungsnummerVermessungsstelle(getString(
                    "zulassungsnummerVermessungsstelle",
                    datenSatzNode,
                    wrapped));
            anfrageBean.setSaumAPSuche(getString("saumAPSuche", datenSatzNode, wrapped));
            anfrageBean.setMitAPBeschreibungen(getBoolean("mitAPBeschreibungen", datenSatzNode, wrapped));
            anfrageBean.setMitAPKarten(getBoolean("mitAPKarten", datenSatzNode, wrapped));
            anfrageBean.setMitAPUebersichten(getBoolean("mitAPUebersichten", datenSatzNode, wrapped));
            anfrageBean.setMitNIVPBeschreibungen(false);
            anfrageBean.setMitNIVPUebersichten(false);
            anfrageBean.setMitAlkisBestandsdatenmitEigentuemerinfo(getBoolean(
                    "mitAlkisBestandsdatenmitEigentuemerinfo",
                    datenSatzNode,
                    wrapped));
            anfrageBean.setMitAlkisBestandsdatennurPunkte(getBoolean(
                    "mitAlkisBestandsdatennurPunkte",
                    datenSatzNode,
                    wrapped));
            anfrageBean.setMitAlkisBestandsdatenohneEigentuemerinfo(getBoolean(
                    "mitAlkisBestandsdatenohneEigentuemerinfo",
                    datenSatzNode,
                    wrapped));
            anfrageBean.setMitRisse(getBoolean("mitRisse", datenSatzNode, wrapped));
            anfrageBean.setMitGrenzniederschriften(getBoolean("mitGrenzniederschriften", datenSatzNode, wrapped));
            anfrageBean.setMitPunktnummernreservierung(getBoolean(
                    "mitPunktnummernreservierung",
                    datenSatzNode,
                    wrapped));
            anfrageBean.setTest(true);
        }

        return anfrageBean;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   objNode  DOCUMENT ME!
     * @param   wrapped  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static VermessungsunterlagenAnfrageBean.AntragsflurstueckBean createAntragsflurstueckBean(
            final JsonNode objNode,
            final boolean wrapped) {
        final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean antragsflurstueckBean =
            new VermessungsunterlagenAnfrageBean.AntragsflurstueckBean();
        antragsflurstueckBean.setFlurID(getString("flurID", objNode, wrapped));
        antragsflurstueckBean.setFlurstuecksID(getString("flurstuecksID", objNode, wrapped));
        antragsflurstueckBean.setGemarkungsID(getString("gemarkungsID", objNode, wrapped));
        return antragsflurstueckBean;
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
        for (final AlkisProductDescription product : ServerAlkisProducts.getInstance().getAlkisMapProducts()) {
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
     * @param   gemarkung  flurstueck DOCUMENT ME!
     * @param   flur       DOCUMENT ME!
     * @param   zaehler    DOCUMENT ME!
     * @param   nenner     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String[] createFlurstueckParts(final String gemarkung,
            final String flur,
            final String zaehler,
            final String nenner) {
        try {
            final String formattedGemarkung = gemarkung.startsWith("05") ? gemarkung.substring(2) : gemarkung;
            final String formattedFlur = String.format("%03d", Integer.parseInt(flur));
            final String formattedZahler = Integer.valueOf(zaehler).toString();
            final String formattedNenner = (nenner != null) ? Integer.valueOf(nenner).toString() : "0";
            return new String[] { formattedGemarkung, formattedFlur, formattedZahler, formattedNenner };
        } catch (final Exception ex) {
            return null;
        }
    }
}
