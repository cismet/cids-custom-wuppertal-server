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

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.types.MetaObjectNode;

import com.vividsolutions.jts.geom.Geometry;

import lombok.Getter;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import de.cismet.cids.custom.utils.StampedByteArrayServerAction;
import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.properties.PotenzialflaechenProperties;
import de.cismet.cids.custom.wunda_blau.search.server.AlkisLandparcelGeometryMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.BodenrichtwertZoneMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.BplaeneMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.FnpHauptnutzungenMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.GeometrySearch;
import de.cismet.cids.custom.wunda_blau.search.server.KstGeometryMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.RestApiMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.RpdKategorieMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.StadtraumtypMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.WohnlagenKategorisierungMonSearch;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

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
public class PotenzialflaecheReportServerAction extends StampedByteArrayServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(PotenzialflaecheReportServerAction.class);
    public static final String TASK_NAME = "potenzialflaecheReport";

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

        KAMPAGNE(new KeytableReportProperty("kampagne", "pf_kampagne"), "Kampagne"),
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
        KATEGORIE(new KeytableReportProperty("fk_kategorie", "pf_kategorie"), "Kategorie"),
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

        KARTE_ORTHO(new VirtualReportProperty() {

                @Override
                public Object calculateProperty(final PotenzialflaecheReportCreator creator) throws Exception {
                    return creator.loadMapFor(PotenzialflaecheReportCreator.Type.PF_ORTHO);
                }
            }, "Karte (Ortho)"),
        KARTE_DGK(new VirtualReportProperty() {

                @Override
                public Object calculateProperty(final PotenzialflaecheReportCreator creator) throws Exception {
                    return creator.loadMapFor(PotenzialflaecheReportCreator.Type.PF_DGK);
                }
            }, "Karte (DGK)"),
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
                    return String.format("%.2f m² (circa %.1f ha)", m2, ha);
                }
            }, "Größe"),
        BEBAUUNGSPLAN(new MonSearchReportProperty("bplan_verfahren") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new BplaeneMonSearch(
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
                    return new KstGeometryMonSearch(KstGeometryMonSearch.SearchFor.BEZIRK);
                }
            }, "Stadtbezirke"),
        QUARTIER(new MonSearchReportProperty("kst_quartier") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new KstGeometryMonSearch(KstGeometryMonSearch.SearchFor.QUARTIER);
                }
            }, "Quartiere"),
        FLURSTUECKE(new MonSearchReportProperty("alkis_landparcel") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new AlkisLandparcelGeometryMonSearch();
                }
            }, "Flurstücke"),
        WOHNLAGEN(new MonSearchReportProperty("wohnlage_kategorie") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new WohnlagenKategorisierungMonSearch(0.1d);
                }
            }, "Wohnlagen"),
        STADTRAUMTYPEN(new MonSearchReportProperty("srt_kategorie") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new StadtraumtypMonSearch();
                }
            }, "Stadtraumtypen"),
        FLAECHENNUTZUNGSPLAN(new MonSearchReportProperty("fnp_hn_kategorie") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new FnpHauptnutzungenMonSearch(0.1d);
                }
            }, "Flächennutzungsplan"),
        REGIONALPLAN(new MonSearchReportProperty("rpd_kategorie") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new RpdKategorieMonSearch(0.2d);
                }
            }, "Regionalplan"),

        BODENRICHTWERTE(new MonSearchReportProperty("brw_zone") {

                @Override
                public RestApiMonSearch createMonServerSearch() {
                    return new BodenrichtwertZoneMonSearch();
                }
            }, "Bodenrichtwerte");

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

        POTENZIALFLAECHE, TEMPLATE
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    private final PropertiesServerResource PSR = (PropertiesServerResource)
        WundaBlauServerResources.POTENZIALFLAECHEN_PROPERTIES.getValue();

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
    public byte[] executeBeforeStamp(final Object body, final ServerActionParameter... params) throws Exception {
        MetaObjectNode flaecheMon = (body != null)
            ? ((body instanceof MetaObjectNode) ? (MetaObjectNode)body
                                                : getFor(new String((byte[])body), "pf_potenzialflaeche", "nummer"))
            : null;
        MetaObjectNode templateMon = null;
        if (params != null) {
            for (final ServerActionParameter sap : params) {
                if (sap.getKey().equals(Parameter.POTENZIALFLAECHE.toString())) {
                    flaecheMon = getFor(sap.getValue(), "pf_potenzialflaeche", "nummer");
                } else if (sap.getKey().equals(Parameter.TEMPLATE.toString())) {
                    templateMon = getFor(sap.getValue(), "pf_steckbrieftemplate", "bezeichnung");
                }
            }
        }
        if (flaecheMon != null) {
            final PotenzialflaecheReportCreator.ReportConfiguration config =
                new PotenzialflaecheReportCreator.ReportConfiguration();
            config.setId(flaecheMon.getObjectId());
            config.setTemplateId((templateMon != null) ? templateMon.getObjectId() : null);
            config.setSubreportDirectory(DomainServerImpl.getServerProperties().getServerResourcesBasePath() + "/");
            config.setCacheDirectory(getProperties().getPictureCacheDirectory());
            config.setUseCache(Boolean.TRUE);
            config.setBbX1(getProperties().getHomeX1());
            config.setBbY1(getProperties().getHomeY1());
            config.setBbX2(getProperties().getHomeX2());
            config.setBbY2(getProperties().getHomeY2());
            config.setSrs(getProperties().getSrs());
//
//                final byte[] bytes = ByteArrayFactoryHandler.getInstance()
//                            .execute(
//                                getProperties().getReportFactory(),
//                                new ObjectMapper().writeValueAsString(config),
//                                getUser(),
//                                getConnectionContext());
//
//                return bytes;

//                    new PotenzialflaecheReportCreator.ReportConfiguration();
            final CidsBean flaecheBean = getMetaService().getMetaObject(
                        getUser(),
                        flaecheMon.getObjectId(),
                        flaecheMon.getClassId(),
                        getConnectionContext())
                        .getBean();
            final PotenzialflaecheReportCreatorImpl creator = new PotenzialflaecheReportCreatorImpl(
                    getProperties(),
                    flaecheBean,
                    getUser(),
                    getMetaService(),
                    getConnectionContext());

            return creator.createReport(config);
        } else {
            throw new Exception("flaeche not given");
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
        return (PotenzialflaechenProperties)ServerResourcesLoader.getInstance().get(PSR);
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
    public static class ReportProperty {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new ReportProperty object.
         */
        public ReportProperty() {
        }
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

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @param   creator  flaecheBean DOCUMENT ME!
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
        public String calculateProperty(final PotenzialflaecheReportCreator creator) {
            final RestApiMonSearch serverSearch = createMonServerSearch();
            if (serverSearch != null) {
                if (serverSearch instanceof GeometrySearch) {
                    final CidsBean flaecheBean = creator.getFlaecheBean();
                    ((GeometrySearch)serverSearch).setGeometry((Geometry)flaecheBean.getProperty(
                            "geometrie.geo_field"));
                    ((GeometrySearch)serverSearch).setBuffer(-2d);
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
                return String.join(", ", names);
            } else {
                return null;
            }
        }
    }
}
