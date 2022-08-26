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
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.types.MetaObjectNode;

import com.vividsolutions.jts.geom.Geometry;

import lombok.Getter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import java.awt.Color;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.cismet.cids.custom.utils.PotenzialflaecheReportCreator;
import de.cismet.cids.custom.utils.PotenzialflaechenMapsJson;
import de.cismet.cids.custom.utils.PotenzialflaechenProperties;
import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.wunda_blau.search.server.AlkisLandparcelGeometryMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.BodenrichtwertZoneMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.BplaeneMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.FnpHauptnutzungenMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.GeometrySearch;
import de.cismet.cids.custom.wunda_blau.search.server.KstGeometryMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.PotenzialflaecheSearch;
import de.cismet.cids.custom.wunda_blau.search.server.RestApiMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.RpdKategorieMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.StadtraumtypMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.WohnlagenKategorisierungMonSearch;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.DefaultServerAction;
import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

import de.cismet.cids.utils.serverresources.JsonServerResource;
import de.cismet.cids.utils.serverresources.PropertiesServerResource;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class PotenzialflaecheReportServerAction extends DefaultServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(PotenzialflaecheReportServerAction.class);
    public static final String TASK_NAME = "potenzialflaecheReport";

    private static String TEMPLATEREPLACER_INPUT_FILELIST = "{PDF_INPUT_FILELIST}";
    private static String TEMPLATEREPLACER_OUTPUT_FILE = "{PDF_OUTPUT_FILE}";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Property {

        //~ Enum constants -----------------------------------------------------

        BEZEICHNUNG(new SimpleFieldReportProperty("bezeichnung", String.class.getCanonicalName()), "Bezeichnung"),
        NUMMER(new SimpleFieldReportProperty("nummer", String.class.getCanonicalName()), "Nummer"),
        BESCHREIBUNG_FLAECHE(new SimpleFieldReportProperty("beschreibung_flaeche", String.class.getCanonicalName()),
            "Beschreibung der Fläche"),
        NOTWENDIGE_MASSNAHMEN(new SimpleFieldReportProperty("notwendige_massnahmen", String.class.getCanonicalName()),
            "Notwendige Maßnahmen"),
        INTERNE_HINWEISE(new SimpleFieldReportProperty("interne_hinweise", String.class.getCanonicalName()),
            "Interne Hinweise"),
        QUELLE(new SimpleFieldReportProperty("quelle", String.class.getCanonicalName()), "Quelle"),
        WOHNEINHEITEN_ANZAHL(new SimpleFieldReportProperty("anzahl_wohneinheiten", Integer.class.getCanonicalName()),
            "Anzahl mögl. Wohneinheiten"),
        FESTSETZUNGEN_BPLAN(new SimpleFieldReportProperty("festsetzungen_bplan", String.class.getCanonicalName()),
            "Festsetzungen"),
        BAUORDNUNGSRECHT_STAND(new SimpleFieldReportProperty(
                "stand_bauordnungsrecht",
                Date.class.getCanonicalName()),
            "Stand des Bauordnungsrechts"),
        STAND(new SimpleFieldReportProperty("stand", Date.class.getCanonicalName()), "Stand der Beschreibung"),
        JAHR_NUTZUNGSAUFGABE(new SimpleFieldReportProperty("jahr_brachflaeche", Date.class.getCanonicalName()),
            "Nutzungsaufgabe"),
        VORHANDENE_BEBAUUNG(new SimpleFieldReportProperty("bestand_bebauung", String.class.getCanonicalName()),
            "Bestand Bebauung"),

        KAMPAGNE(new KeytableReportProperty("kampagne", "pf_kampagne"), "Kategorie"),
        LAGEBEWERTUNG_VERKEHR(new KeytableReportProperty("fk_lagebewertung_verkehr", "pf_lagebewertung_verkehr"),
            "Lagebewertung, Verkehr"),
        SIEDLUNGSRAEUMLICHE_LAGE(new KeytableReportProperty(
                "fk_siedlungsraeumliche_lage",
                "pf_siedlungsraeumliche_lage"),
            "Siedlungsräumliche Lage"),
        TOPOGRAFIE(new KeytableReportProperty("topografie", "pf_topografie"), "Topografie"),
        HANG(new KeytableReportProperty("fk_ausrichtung", "pf_ausrichtung"), "Hang"),
        VERWERTBARKEIT(new KeytableReportProperty("fk_verwertbarkeit", "pf_verwertbarkeit"), "Verwertbarkeit"),
        VERFUEGBBARKEIT(new KeytableReportProperty("verfuegbarkeit", "pf_verfuegbarkeit"), "Verfügbarkeit"),
        ENTWICKLUNGSAUSSSICHTEN(new KeytableReportProperty("fk_entwicklungsaussichten", "pf_entwicklungsaussichten"),
            "Entwicklungsaussichten"),
        ENTWICKLUNGSSTAND(new KeytableReportProperty("fk_entwicklungsstand", "pf_entwicklungsstand"),
            "Entwicklungsstand"),
        REVITALISIERUNG(new KeytableReportProperty("fk_revitalisierung", "pf_revitalisierung"), "Revitalisierung"),
        AEUSSERE_ERSCHLIESSUNG(new KeytableReportProperty("fk_aeussere_erschliessung", "pf_aeussere_erschliessung"),
            "Äußere Erschließung"),
        POTENZIALART(new KeytableReportProperty("fk_potenzialart", "pf_potenzialart"), "Potenzialart"),
        KATEGORIE(new KeytableReportProperty("fk_kategorie", "pf_kategorie"), "Entwicklungsart"),
        WOHNEINHEITEN(new KeytableReportProperty("fk_wohneinheiten", "pf_wohneinheiten"), "Wohneinheiten"),
        OEPNV_ANBINDUNG(new KeytableReportProperty("fk_oepnv", "pf_oepnv"), "ÖPNV-Qualität"),
        KLIMAINFORMATIONEN(new KeytableReportProperty("fk_klimainformationen", "pf_klimainformationen"),
            "Klimainformationen"),
        VERSIEGELUNG(new KeytableReportProperty("fk_versiegelung", "pf_versiegelung"), "Versiegelung"),
        BAUORDNUNGSRECHT_GENEHMIGUNG(new KeytableReportProperty(
                "fk_bauordnungsrecht_genehmigung",
                "pf_bauordnungsrecht_genehmigung"),
            "Bauordnungsrecht (Genehmigung)"),
        BAUORDNUNGSRECHT_BAULAST(new KeytableReportProperty(
                "fk_bauordnungsrecht_baulast",
                "pf_bauordnungsrecht_baulast"),
            "Bauordnungsrecht (Baulast)"),
        HANDLUNGSDRUCK(new KeytableReportProperty("handlungsdruck", "pf_handlungsdruck"), "Handlungsdruck"),
        HANDLUNGSPRIORITAET(new KeytableReportProperty("fk_handlungsprioritaet", "pf_handlungsprioritaet"),
            "Handlungspriorität"),

        EIGENTUEMER(new MultiKeytableReportProperty(
                "arr_eigentuemer",
                "pf_eigentuemer_arr.fk_eigentuemer",
                "pf_eigentuemer"),
            "Eigentümer"),
        BISHERIGE_NUTZUNG(new MultiKeytableReportProperty(
                "bisherige_nutzung",
                "pf_potenzialflaechen_bisherige_nutzung.nutzung",
                "pf_nutzung"),
            "Bisherige Nutzung"),
        UMGEBUNGSNUTZUNG(new MultiKeytableReportProperty(
                "umgebungsnutzung",
                "pf_potenzialflaechen_umgebungsnutzung.nutzung",
                "pf_nutzung"),
            "Umgebungsnutzung"),
        NAEHE_ZU(new MultiKeytableReportProperty("arr_naehen_zu", "pf_naehen_zu.fk_naehe_zu", "pf_naehe_zu"), "Nähe zu"),
        BRACHFLAECHENKATEGORIE(new MultiKeytableReportProperty(
                "arr_brachflaechen",
                "pf_brachflaechen.fk_brachflaeche",
                "pf_brachflaeche"),
            "Brachfläche"),
        EMPFOHLENE_NUTZUNGEN(new MultiKeytableReportProperty(
                "arr_empfohlene_nutzungen",
                "pf_empfohlene_nutzungen.fk_empfohlene_nutzung",
                "pf_empfohlene_nutzung"),
            "Empfohlene Nutzung"),
        EMPFOHLENE_NUTZUNGEN_WOHNEN(new MultiKeytableReportProperty(
                "arr_empfohlene_nutzungen_wohnen",
                "pf_empfohlene_nutzungen_wohnen.fk_empfohlene_nutzung_wohnen",
                "pf_empfohlene_nutzung_wohnen"),
            "Empfohlene Art der Wohnnutzung"),
        RESTRIKTIONEN(new MultiKeytableReportProperty(
                "arr_restriktionen",
                "pf_restriktionen.fk_restriktion",
                "pf_restriktion"),
            "Restriktionen"),
        GROESSE(new VirtualReportProperty() {

                @Override
                public String calculateProperty(final PotenzialflaecheReportCreator creator) {
                    final CidsBean flaecheBean = creator.getFlaecheBean();
                    final Object geo = flaecheBean.getProperty("geometrie.geo_field");
                    double area = 0.0;

                    if (geo instanceof Geometry) {
                        area = ((Geometry)geo).getArea();
                    }

                    final double m2 = Math.round(area * 100) / 100.0;
                    final double ha = Math.round(area / 1000) / 10.0;
                    return String.format("%,.2f m² (circa %,.1f ha)", m2, ha);
                }
            }, "Größe"),
        BEBAUUNGSPLAN(new MonSearchReportProperty("bplan_verfahren") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new BplaeneMonSearch(
                            -3d, // per mail so gewünscht
                            new BplaeneMonSearch.SubUnion(
                                "nummer || ' (' || vstandr || ' ' || TO_CHAR(TO_DATE(datumr, 'DD.MM.YYYY'), 'DD.MM.YYYY') || ')'",
                                "LEFT JOIN geom ON geom.id = bplan_verfahren.geometrie",
                                "vstandr IS NOT NULL",
                                "('-'||LPAD(REGEXP_REPLACE(COALESCE(nummer, '0'), '[^0-9]+.*$', '', 'g'), 4, '0') || LPAD(COALESCE(TO_CHAR(TO_DATE(datumr, 'DD.MM.YYYY'), 'YYYYMMDD'), '0'), 8, '0'))::bigint"),
                            new BplaeneMonSearch.SubUnion(
                                "nummer || ' (' || vstandi || ' ' || TO_CHAR(TO_DATE(datumi, 'DD.MM.YYYY'), 'DD.MM.YYYY') || ')'",
                                "LEFT JOIN geom ON geom.id = bplan_verfahren.georefi",
                                "vstandi IS NOT NULL",
                                "('-'||LPAD(REGEXP_REPLACE(COALESCE(nummer, '0'), '[^0-9]+.*$', '', 'g'), 4, '0') || LPAD(COALESCE(TO_CHAR(TO_DATE(datumi, 'DD.MM.YYYY'), 'YYYYMMDD'), '0'), 8, '0'))::bigint"));
                }
            }, "BPlan"),
        STADTBEZIRK(new MonSearchReportProperty("kst_stadtbezirk") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new KstGeometryMonSearch(KstGeometryMonSearch.SearchFor.BEZIRK, -2d);
                }
            }, "Stadtbezirke"),
        QUARTIER(new MonSearchReportProperty("kst_quartier") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new KstGeometryMonSearch(KstGeometryMonSearch.SearchFor.QUARTIER, -2d);
                }
            }, "Quartiere"),
        FLURSTUECKE(new MonSearchReportProperty("alkis_landparcel") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new AlkisLandparcelGeometryMonSearch(-2d);
                }
            }, "Flurstücke"),
        WOHNLAGEN(new MonSearchReportProperty("wohnlage_kategorie") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new WohnlagenKategorisierungMonSearch(0.1d, -2d);
                }
            }, "Wohnlagen"),
        STADTRAUMTYPEN(new MonSearchReportProperty("srt_kategorie") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new StadtraumtypMonSearch(-2d);
                }
            }, "Stadtraumtypen"),
        FLAECHENNUTZUNGSPLAN(new MonSearchReportProperty("fnp_hn_kategorie") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new FnpHauptnutzungenMonSearch(0.1d, -2d);
                }
            }, "Flächennutzungsplan"),
        REGIONALPLAN(new MonSearchReportProperty("rpd_kategorie") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new RpdKategorieMonSearch(0.2d, -2d);
                }
            }, "Regionalplan"),

        BODENRICHTWERTE(new MonSearchReportProperty("brw_zone") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new BodenrichtwertZoneMonSearch(-2d);
                }
            }, "Bodenrichtwerte"),
        BACKCOLOR(new VirtualReportProperty() {

                @Override
                public String calculateProperty(final PotenzialflaecheReportCreator creator) {
                    final CidsBean flaecheBean = creator.getFlaecheBean();
                    final String colorcode = (String)flaecheBean.getProperty("kampagne.colorcode");
                    return colorcode;
                }
            }, "Größe"),
        FORECOLOR(new VirtualReportProperty() {

                @Override
                public String calculateProperty(final PotenzialflaecheReportCreator creator) {
                    final CidsBean flaecheBean = creator.getFlaecheBean();
                    final String colorcode = (String)flaecheBean.getProperty("kampagne.colorcode");
                    final Color backgroundColor = Color.decode(colorcode);
                    final double luminance = (0.2126 * backgroundColor.getRed()) + (0.7152 * backgroundColor.getGreen())
                                + (0.0722 * backgroundColor.getBlue());
                    return (luminance < 140) ? "#FFFFFF" : "#000000";
                }
            }, "Größe");

        //~ Instance fields ----------------------------------------------------

        @Getter private final ReportProperty value;
        private final String toString;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Property object.
         *
         * @param  value     DOCUMENT ME!
         * @param  toString  DOCUMENT ME!
         */
        private Property(final ReportProperty value, final String toString) {
            this.value = value;
            this.toString = toString;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString() {
            return toString;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        POTENZIALFLAECHE, KATEGORIE, BODY_TYPE, RESULT_TYPE, TEMPLATE, EXTERNAL
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum BodyType {

        //~ Enum constants -----------------------------------------------------

        POTENZIALFLAECHE, KATEGORIE
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ResultType {

        //~ Enum constants -----------------------------------------------------

        PDF, ZIP
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    private final PropertiesServerResource POTENZIALFLAECHEN_PROPERTIES = (PropertiesServerResource)
        WundaBlauServerResources.POTENZIALFLAECHEN_PROPERTIES.getValue();
    private final JsonServerResource POTENZIALFLAECHEN_MAPS_JSON = (JsonServerResource)
        WundaBlauServerResources.POTENZIALFLAECHEN_MAPS_JSON.getValue();

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   object      DOCUMENT ME!
     * @param   table_name  DOCUMENT ME!
     * @param   key         DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private MetaObjectNode getFor(final Object object, final String table_name, final String key) throws Exception {
        if (object instanceof MetaObjectNode) {
            return (MetaObjectNode)object;
        } else if (object instanceof Integer) {
            return new MetaObjectNode(
                    "WUNDA_BLAU",
                    (Integer)object,
                    CidsBean.getMetaClassFromTableName("WUNDA_BLAU", table_name, getConnectionContext()).getId());
        } else if (object instanceof String) {
            final List singelResult = getMetaService().performCustomSearch(String.format(
                            "SELECT id, (SELECT id FROM cs_class WHERE table_name ILIKE '%1$s') FROM %1$s WHERE %2$s = '%3$s' LIMIT 1;",
                            table_name,
                            key,
                            StringEscapeUtils.escapeSql((String)object)),
                        getConnectionContext())
                        .iterator()
                        .next();
            if ((singelResult != null) && (singelResult.size() == 2)) {
                return new MetaObjectNode("WUNDA_BLAU", (Integer)singelResult.get(0), (Integer)singelResult.get(1));
            }
        }
        return null;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            BodyType bodyType = BodyType.POTENZIALFLAECHE;
            ResultType resultType = ResultType.PDF;

            final Set<MetaObjectNode> flaecheMons = new HashSet<>();
            final Set<MetaObjectNode> kategorieMons = new HashSet<>();
            MetaObjectNode templateMon = null;

            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    final Object value = sap.getValue();
                    if (sap.getKey().equals(Parameter.BODY_TYPE.toString())) {
                        bodyType = (value instanceof BodyType)
                            ? (BodyType)value : ((value instanceof String) ? BodyType.valueOf((String)value) : null);
                    } else if (sap.getKey().equals(Parameter.RESULT_TYPE.toString())) {
                        resultType = (value instanceof ResultType)
                            ? (ResultType)value
                            : ((value instanceof String) ? ResultType.valueOf((String)value) : null);
                    } else if (sap.getKey().equals(Parameter.POTENZIALFLAECHE.toString())) {
                        flaecheMons.add(getFor(sap.getValue(), "pf_potenzialflaeche", "nummer"));
                    } else if (sap.getKey().equals(Parameter.KATEGORIE.toString())) {
                        kategorieMons.add(getFor(sap.getValue(), "pf_kampagne", "bezeichnung"));
                    } else if (sap.getKey().equals(Parameter.TEMPLATE.toString())) {
                        templateMon = getFor(sap.getValue(), "pf_steckbrieftemplate", "bezeichnung");
                    }
                }
            }

            final CidsBean templateBean = (templateMon != null)
                ? getMetaService().getMetaObject(
                            getUser(),
                            templateMon.getObjectId(),
                            templateMon.getClassId(),
                            getConnectionContext()).getBean() : null;

            if (BodyType.POTENZIALFLAECHE.equals(bodyType)) {
                if (body instanceof MetaObjectNode) {
                    flaecheMons.add((MetaObjectNode)body);
                } else if (body instanceof byte[]) {
                    flaecheMons.add(getFor(new String((byte[])body), "pf_potenzialflaeche", "nummer"));
                } else if (body instanceof MetaObjectNode[]) {
                    flaecheMons.addAll(Arrays.asList((MetaObjectNode[])body));
                } else if (body instanceof Collection) {
                    flaecheMons.addAll((Collection<MetaObjectNode>)body);
                }
            } else if (BodyType.KATEGORIE.equals(bodyType)) {
                if (body instanceof MetaObjectNode) {
                    kategorieMons.add((MetaObjectNode)body);
                } else if (body instanceof byte[]) {
                    kategorieMons.add(getFor(new String((byte[])body), "pf_kampagne", "bezeichnung"));
                } else if (body instanceof MetaObjectNode[]) {
                    kategorieMons.addAll(Arrays.asList((MetaObjectNode[])body));
                } else if (body instanceof Collection) {
                    kategorieMons.addAll((Collection<MetaObjectNode>)body);
                }
            }

            if (!kategorieMons.isEmpty()) {
                for (final MetaObjectNode kategorieMon : kategorieMons) {
                    final PotenzialflaecheSearch.Configuration configuration =
                        new PotenzialflaecheSearch.Configuration();
                    configuration.addFilter(Property.KAMPAGNE, kategorieMon);
                    final PotenzialflaecheSearch search = new PotenzialflaecheSearch(true);
                    final Map localServers = new HashMap<>();
                    localServers.put("WUNDA_BLAU", getMetaService());
                    search.setActiveLocalServers(localServers);
                    search.setUser(getUser());
                    search.setConfiguration(configuration);
                    flaecheMons.addAll(search.performServerSearch());
                }
            }

            final String reportsDirectoryName = getProperties().getReportsDirectory();
            final String random = RandomStringUtils.randomAlphanumeric(24);
            if (flaecheMons.isEmpty()) {
                throw new Exception("flaeche not given");
            } else if (ResultType.PDF.equals(resultType)) {
                final File reportsDirectory = new File(reportsDirectoryName);
                final String pdfName = String.format("%s.pdf", random);
                final File pdfFile = new File(reportsDirectoryName, pdfName);
                if (flaecheMons.size() == 1) {
                    final MetaObjectNode flaecheMon = flaecheMons.iterator().next();
                    final CidsBean flaecheBean = getMetaService().getMetaObject(
                                getUser(),
                                flaecheMon.getObjectId(),
                                flaecheMon.getClassId(),
                                getConnectionContext())
                                .getBean();
                    createSingleReport(flaecheBean, templateBean, pdfFile);
                } else {
                    final String cmdTemplate = getProperties().getPdfMergeCmdTemplate();
                    if ((cmdTemplate != null) && !cmdTemplate.isEmpty()) {
                        final File tmpDir = new File(reportsDirectoryName, random);
                        if (!tmpDir.exists()) {
                            tmpDir.mkdir();
                        }
                        for (final MetaObjectNode flaecheMon : flaecheMons) {
                            final CidsBean flaecheBean = getMetaService().getMetaObject(
                                        getUser(),
                                        flaecheMon.getObjectId(),
                                        flaecheMon.getClassId(),
                                        getConnectionContext())
                                        .getBean();
                            final File pdfTmpFile = new File(
                                    tmpDir,
                                    String.format("%s.pdf", (String)flaecheBean.getProperty("nummer")));
                            createSingleReport(flaecheBean, templateBean, pdfTmpFile);
                        }

                        final String cmd = cmdTemplate.replaceAll(Pattern.quote(TEMPLATEREPLACER_INPUT_FILELIST),
                                    Matcher.quoteReplacement(String.format("\"%s\"/*", tmpDir)))
                                    .replaceAll(Pattern.quote(TEMPLATEREPLACER_OUTPUT_FILE),
                                        Matcher.quoteReplacement(String.format("\"%s\"", pdfFile)));
                        LOG.info(String.format("executing CMD: %s", cmd));
                        executeCmd(cmd, reportsDirectory);
                        for (final File tmpFile : tmpDir.listFiles()) {
                            tmpFile.delete();
                        }
                        tmpDir.delete();
                    } else {
                        throw new Exception("can't create multi-report pdf, no merge command provided");
                    }
                }
                return pdfName;
            } else if (ResultType.ZIP.equals(resultType)) {
                final File tmpDir = new File(getProperties().getReportsDirectory());
                final String zipName = String.format("%s.zip", random);
                final File zipFile = new File(tmpDir, zipName);
                try(final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
                    final StringBuffer errorAppender = new StringBuffer();
                    for (final MetaObjectNode flaecheMon : flaecheMons) {
                        final String pdfName = String.format("%s.pdf", RandomStringUtils.randomAlphanumeric(24));
                        final File pdfFile = new File(tmpDir, pdfName);

                        try {
                            final CidsBean flaecheBean = getMetaService().getMetaObject(
                                        getUser(),
                                        flaecheMon.getObjectId(),
                                        flaecheMon.getClassId(),
                                        getConnectionContext())
                                        .getBean();
                            createSingleReport(flaecheBean, templateBean, pdfFile);
                            appendZipEntryFor(
                                pdfFile,
                                String.format("%s.pdf", (String)flaecheBean.getProperty("nummer")),
                                zipOutputStream);
                            pdfFile.delete();
                        } catch (final Exception ex) {
                            errorAppender.append(String.format(
                                    "-----%s-----\n==========\n%s\n==========\n",
                                    flaecheMon.toString(),
                                    ex.getMessage()));
                        }
                    }
                    final String errors = errorAppender.toString();
                    if (!errors.isEmpty()) {
                        appendZipEntryFor(errors, "errors.txt", zipOutputStream);
                    }
                    zipOutputStream.closeEntry();
                    zipOutputStream.finish();
                    zipOutputStream.flush();
                    return zipName;
                }
            } else {
                return null;
            }
        } catch (final Exception ex) {
            LOG.error("error while creating report", ex);
            return ex;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   cmd         DOCUMENT ME!
     * @param   workingDir  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private static String executeCmd(final String cmd, final File workingDir) throws Exception {
        final ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", cmd).directory(workingDir);
        final Process process = builder.start();
        final InputStream is = process.getInputStream();
        return IOUtils.toString(new InputStreamReader(is));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   file             is DOCUMENT ME!
     * @param   entryName        DOCUMENT ME!
     * @param   zipOutputStream  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public ZipOutputStream appendZipEntryFor(final File file,
            final String entryName,
            final ZipOutputStream zipOutputStream) throws IOException {
        try(final FileInputStream fileInputStream = new FileInputStream(file)) {
            return appendZipEntryFor(fileInputStream, entryName, zipOutputStream);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   string           DOCUMENT ME!
     * @param   entryName        DOCUMENT ME!
     * @param   zipOutputStream  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public ZipOutputStream appendZipEntryFor(final String string,
            final String entryName,
            final ZipOutputStream zipOutputStream) throws IOException {
        return appendZipEntryFor(IOUtils.toInputStream(string, "UTF-8"), entryName, zipOutputStream);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   inputStream      DOCUMENT ME!
     * @param   entryName        DOCUMENT ME!
     * @param   zipOutputStream  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public ZipOutputStream appendZipEntryFor(final InputStream inputStream,
            final String entryName,
            final ZipOutputStream zipOutputStream) throws IOException {
        final ZipEntry zipEntry = new ZipEntry(entryName);
        zipOutputStream.putNextEntry(zipEntry);
        IOUtils.copy(inputStream, zipOutputStream);
        zipOutputStream.closeEntry();
        return zipOutputStream;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flaecheBean   DOCUMENT ME!
     * @param   templateBean  DOCUMENT ME!
     * @param   file          DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void createSingleReport(final CidsBean flaecheBean, final CidsBean templateBean, final File file)
            throws Exception {
        try(final FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            final PotenzialflaecheReportCreator creator = new PotenzialflaecheReportCreator(
                    getProperties(),
                    getMapsJson(),
                    flaecheBean,
                    getUser(),
                    getMetaService(),
                    getConnectionContext());
            creator.writeReportToOutputStream(flaecheBean, templateBean, fileOutputStream);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public PotenzialflaechenProperties getProperties() throws Exception {
        return (PotenzialflaechenProperties)ServerResourcesLoader.getInstance().get(POTENZIALFLAECHEN_PROPERTIES);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public PotenzialflaechenMapsJson getMapsJson() throws Exception {
        return (PotenzialflaechenMapsJson)ServerResourcesLoader.getInstance().get(POTENZIALFLAECHEN_MAPS_JSON);
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    public abstract static class ReportProperty {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new ReportProperty object.
         */
        public ReportProperty() {
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @param   creator  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         *
         * @throws  Exception  DOCUMENT ME!
         */
        public abstract Object calculateProperty(final PotenzialflaecheReportCreator creator) throws Exception;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    public abstract static class PathReportProperty extends ReportProperty {

        //~ Instance fields ----------------------------------------------------

        private final String path;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new SingleFieldReportProperty object.
         *
         * @param  path  DOCUMENT ME!
         */
        public PathReportProperty(final String path) {
            this.path = path;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public Object calculateProperty(final PotenzialflaecheReportCreator creator) throws Exception {
            return creator.getFlaecheBean().getProperty(getPath());
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class SimpleFieldReportProperty extends PathReportProperty {

        //~ Instance fields ----------------------------------------------------

        @Getter private final String className;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new MultifieldReportProperty object.
         *
         * @param  path  DOCUMENT ME!
         */
        public SimpleFieldReportProperty(final String path) {
            this(path, null);
        }

        /**
         * Creates a new SinglefieldReportProperty object.
         *
         * @param  path       DOCUMENT ME!
         * @param  className  DOCUMENT ME!
         */
        public SimpleFieldReportProperty(final String path, final String className) {
            super(path);

            this.className = className;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class KeytableReportProperty extends PathReportProperty {

        //~ Instance fields ----------------------------------------------------

        @Getter private final String foreignTable;
        @Getter private final String filterPath;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new SinglefieldReportProperty object.
         *
         * @param  bindingPath   DOCUMENT ME!
         * @param  foreignTable  DOCUMENT ME!
         */
        public KeytableReportProperty(final String bindingPath, final String foreignTable) {
            this(bindingPath, bindingPath, foreignTable);
        }

        /**
         * Creates a new KeytableReportProperty object.
         *
         * @param  bindingPath   DOCUMENT ME!
         * @param  filterPath    DOCUMENT ME!
         * @param  foreignTable  DOCUMENT ME!
         */
        public KeytableReportProperty(final String bindingPath, final String filterPath, final String foreignTable) {
            super(bindingPath);
            this.foreignTable = foreignTable;
            this.filterPath = filterPath;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class MultiKeytableReportProperty extends KeytableReportProperty {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new MultifieldReportProperty object.
         *
         * @param  bindingPath   DOCUMENT ME!
         * @param  filterPath    DOCUMENT ME!
         * @param  foreignTable  DOCUMENT ME!
         */
        public MultiKeytableReportProperty(final String bindingPath,
                final String filterPath,
                final String foreignTable) {
            super(bindingPath, filterPath, foreignTable);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public Object calculateProperty(final PotenzialflaecheReportCreator creator) throws Exception {
            return creator.getFlaecheBean().getBeanCollectionProperty(getPath());
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public abstract static class VirtualReportProperty extends ReportProperty {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new VirtualReportProperty object.
         */
        public VirtualReportProperty() {
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public abstract static class MonSearchReportProperty extends VirtualReportProperty {

        //~ Instance fields ----------------------------------------------------

        @Getter private final String tableName;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new MonSearchReportProperty object.
         *
         * @param  tableName  DOCUMENT ME!
         */
        public MonSearchReportProperty(final String tableName) {
            this.tableName = tableName;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public abstract RestApiMonSearch createMonServerSearch();

        @Override
        public Collection calculateProperty(final PotenzialflaecheReportCreator creator) {
            final RestApiMonSearch serverSearch = createMonServerSearch();
            if (serverSearch != null) {
                if (serverSearch instanceof GeometrySearch) {
                    final CidsBean flaecheBean = creator.getFlaecheBean();
                    ((GeometrySearch)serverSearch).setGeometry((Geometry)flaecheBean.getProperty(
                            "geometrie.geo_field"));
                }
                final Collection<String> names = new ArrayList<>();
                try {
                    for (final MetaObjectNode mon : creator.executeSearch(serverSearch)) {
                        names.add(mon.getName());
                    }
                } catch (final Exception ex) {
                    LOG.error(ex, ex);
                    return null;
                }
                return names;
            } else {
                return null;
            }
        }
    }
}
