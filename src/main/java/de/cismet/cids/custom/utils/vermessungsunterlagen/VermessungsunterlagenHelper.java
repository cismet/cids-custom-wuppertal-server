/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.vermessungsunterlagen;

/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/

import com.bedatadriven.jackson.datatype.jts.JtsModule;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.awt.geom.Point2D;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenHelper {

    //~ Static fields/initializers ---------------------------------------------

    private static final ObjectMapper MAPPER;

    static {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER = mapper;
    }

    private static final transient Logger LOG = Logger.getLogger(VermessungsunterlagenHelper.class);

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
     * @param   json  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public static ExecuteJobBean createExecuteJobBean(final String json) throws IOException {
        final JsonNode rootNode = MAPPER.readTree(json);
        final JsonNode in0 = rootNode.get("in0");

        final ExecuteJobBean executeJobBean = new ExecuteJobBean();
        executeJobBean.setAktenzeichenKatasteramt(getString("aktenzeichenKatasteramt", in0));

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
                anfragepolygonList.add(anfragepolygon);
            }
        }
        executeJobBean.setAnfragepolygonArray(anfragepolygonList.toArray(new Polygon[0]));

        final Collection<AntragsflurstueckBean> antragsflurstueckList = new ArrayList<AntragsflurstueckBean>();
        final JsonNode antragsflurstuecksArrayNode = in0.get("antragsflurstuecksArray").get("antragsflurstuecksArray");
        if ((antragsflurstuecksArrayNode != null) && antragsflurstuecksArrayNode.isArray()) {
            for (final JsonNode objNode : antragsflurstuecksArrayNode) {
                final AntragsflurstueckBean antragsflurstueckBean = new AntragsflurstueckBean();
                antragsflurstueckBean.setFlurID(getString("flurID", objNode));
                antragsflurstueckBean.setFlurstuecksID(getString("flurstuecksID", objNode));
                antragsflurstueckBean.setGemarkungsID(getString("gemarkungsID", objNode));
                antragsflurstueckList.add(antragsflurstueckBean);
            }
        }
        executeJobBean.setAntragsflurstuecksArray(antragsflurstueckList.toArray(new AntragsflurstueckBean[0]));

        executeJobBean.setGeschaeftsbuchnummer(getString("geschaeftsbuchnummer", in0));
        executeJobBean.setKatasteramtAuftragsnummer(getString("katasteramtAuftragsnummer", in0));
        executeJobBean.setKatasteramtsId(getString("katasteramtsId", in0));
        executeJobBean.setMitGrenzniederschriften(getBoolean("mitGrenzniederschriften", in0));
        executeJobBean.setNameVermessungsstelle(getString("nameVermessungsstelle", in0));
        executeJobBean.setNurPunktnummernreservierung(getBoolean("nurPunktnummernreservierung", in0));
        executeJobBean.setSaumAPSuche(getString("saumAPSuche", in0));

        final Collection<PunktnummernreservierungBean> punktnummernreservierungList =
            new ArrayList<PunktnummernreservierungBean>();
        final JsonNode punktnummernreservierungsArrayNode = in0.get("punktnummernreservierungsArray")
                    .get("punktnummernreservierungsArray");
        if ((punktnummernreservierungsArrayNode != null) && punktnummernreservierungsArrayNode.isArray()) {
            for (final JsonNode objNode : punktnummernreservierungsArrayNode) {
                final PunktnummernreservierungBean punktnummernreservierungBean = new PunktnummernreservierungBean();
                punktnummernreservierungBean.setAnzahlPunktnummern(getInteger("anzahlPunktnummern", objNode));
                punktnummernreservierungBean.setKatasteramtsID(getString("katasteramtsID", objNode));
                punktnummernreservierungBean.setUtmKilometerQuadrat(getString("utmKilometerQuadrat", objNode));
                punktnummernreservierungList.add(punktnummernreservierungBean);
            }
        }
        executeJobBean.setPunktnummernreservierungsArray(punktnummernreservierungList.toArray(
                new PunktnummernreservierungBean[0]));

        executeJobBean.setZulassungsnummerVermessungsstelle(getString("zulassungsnummerVermessungsstelle", in0));

        return executeJobBean;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        try {
            configLog4J();

            final File directory = new File("/home/jruiz/tmp/");
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

                final ExecuteJobBean executeJobBean = createExecuteJobBean(executeJobContent);

                final ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JtsModule());
                LOG.info("Created object: " + mapper.writeValueAsString(executeJobBean));
                LOG.info("----");
            }
        } catch (final Exception ex) {
            LOG.error(ex, ex);
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

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    public static class ExecuteJobBean {

        //~ Instance fields ----------------------------------------------------

        private String aktenzeichenKatasteramt;
        private Polygon[] anfragepolygonArray;
        private AntragsflurstueckBean[] antragsflurstuecksArray;
        private String[] artderVermessung;
        private String geschaeftsbuchnummer;
        private String katasteramtAuftragsnummer;
        private String katasteramtsId;
        private Boolean mitGrenzniederschriften;
        private String nameVermessungsstelle;
        private Boolean nurPunktnummernreservierung;
        private PunktnummernreservierungBean[] punktnummernreservierungsArray;
        private String saumAPSuche;
        private String zulassungsnummerVermessungsstelle;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    public static class AntragsflurstueckBean {

        //~ Instance fields ----------------------------------------------------

        private String flurID;
        private String flurstuecksID;
        private String gemarkungsID;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    public static class PunktnummernreservierungBean {

        //~ Instance fields ----------------------------------------------------

        private Integer anzahlPunktnummern;
        private String katasteramtsID;
        private String utmKilometerQuadrat;
    }
}
