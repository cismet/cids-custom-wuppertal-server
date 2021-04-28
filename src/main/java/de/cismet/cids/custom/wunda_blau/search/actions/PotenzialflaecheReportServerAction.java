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
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;

import Sirius.util.MapImageFactoryConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.vividsolutions.jts.geom.Geometry;

import lombok.Getter;
import lombok.Setter;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.ByteArrayInputStream;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import de.cismet.cids.custom.utils.ByteArrayFactoryHandler;
import de.cismet.cids.custom.utils.StampedJasperReportServerAction;
import de.cismet.cids.custom.utils.properties.PotenzialflaechenMapfactoryProperties;
import de.cismet.cids.custom.wunda_blau.search.server.AlkisLandparcelGeometryMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.BplaeneMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.FnpHauptnutzungenMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.KstGeometryMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.RpdKategorieMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.StadtraumtypMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.WohnlagenKategorisierungMonSearch;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.cids.utils.serverresources.JasperReportServerResource;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class PotenzialflaecheReportServerAction extends StampedJasperReportServerAction
        implements ConnectionContextStore,
            UserAwareServerAction {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "potenzialflaecheReport";

    private static final Map<String, JasperReportServerResource> BEAN_RESOURCE_MAP = new HashMap<>();
    private static String CURRENT_TEMPLATE = "";

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd.MM.yyyy");

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
                protected Object calculateProperty(final PotenzialflaecheReportServerAction serverAction)
                        throws Exception {
                    final PfMapConfiguration config = createPreconfiguredPfMapConfiguration();
                    config.setType(PfMapConfiguration.Type.PF_ORTHO);
                    config.setWidth(
                        PotenzialflaechenMapfactoryProperties.getInstance().getWidth(PfMapConfiguration.Type.PF_ORTHO));
                    config.setHeight(
                        PotenzialflaechenMapfactoryProperties.getInstance().getHeight(
                            PfMapConfiguration.Type.PF_ORTHO));
                    config.setBuffer(
                        PotenzialflaechenMapfactoryProperties.getInstance().getBuffer(
                            PfMapConfiguration.Type.PF_ORTHO));
                    config.setIds(Arrays.asList(serverAction.getFlaecheBean().getMetaObject().getId()));

                    final byte[] bytes = ByteArrayFactoryHandler.getInstance()
                                .execute(
                                    "de.cismet.cids.custom.reports.wunda_blau.PfMapGenerator",
                                    new ObjectMapper().writeValueAsString(config),
                                    serverAction.getUser(),
                                    serverAction.getConnectionContext());
                    return ImageIO.read(new ByteArrayInputStream(bytes));
                }
            }, "Karte (Ortho)"),
        KARTE_DGK(new VirtualReportProperty() {

                @Override
                protected Object calculateProperty(final PotenzialflaecheReportServerAction serverAction)
                        throws Exception {
                    final PfMapConfiguration config = createPreconfiguredPfMapConfiguration();
                    config.setType(PfMapConfiguration.Type.PF_DGK);
                    config.setWidth(
                        PotenzialflaechenMapfactoryProperties.getInstance().getWidth(PfMapConfiguration.Type.PF_DGK));
                    config.setHeight(
                        PotenzialflaechenMapfactoryProperties.getInstance().getHeight(PfMapConfiguration.Type.PF_DGK));
                    config.setBuffer(
                        PotenzialflaechenMapfactoryProperties.getInstance().getBuffer(PfMapConfiguration.Type.PF_DGK));

                    config.setIds(Arrays.asList(serverAction.getFlaecheBean().getMetaObject().getId()));
                    final byte[] bytes = ByteArrayFactoryHandler.getInstance()
                                .execute(
                                    "de.cismet.cids.custom.reports.wunda_blau.PfMapGenerator",
                                    new ObjectMapper().writeValueAsString(config),
                                    serverAction.getUser(),
                                    serverAction.getConnectionContext());
                    return ImageIO.read(new ByteArrayInputStream(bytes));
                }
            }, "Karte (DGK)"),
        GROESSE(new VirtualReportProperty() {

                @Override
                protected String calculateProperty(final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
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
        BEBAUUNGSPLAN(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final BplaeneMonSearch serverSearch = new BplaeneMonSearch(
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }, "BPlan"),
        STADTBEZIRK(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final KstGeometryMonSearch serverSearch = new KstGeometryMonSearch(
                            KstGeometryMonSearch.SearchFor.BEZIRK,
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }, "Stadtbezirke"),
        QUARTIER(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final KstGeometryMonSearch serverSearch = new KstGeometryMonSearch(
                            KstGeometryMonSearch.SearchFor.QUARTIER,
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }, "Quartiere"),
        FLURSTUECKE(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final AlkisLandparcelGeometryMonSearch serverSearch = new AlkisLandparcelGeometryMonSearch(
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }, "Flurstücke"),
        WOHNLAGEN(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final WohnlagenKategorisierungMonSearch serverSearch = new WohnlagenKategorisierungMonSearch(
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }, "Wohnlagen"),
        STADTRAUMTYPEN(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final StadtraumtypMonSearch serverSearch = new StadtraumtypMonSearch(
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }, "Stadtraumtypen"),
        FLAECHENNUTZUNGSPLAN(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final FnpHauptnutzungenMonSearch serverSearch = new FnpHauptnutzungenMonSearch(
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }, "Flächennutzungsplan"),
        REGIONALPLAN(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final RpdKategorieMonSearch serverSearch = new RpdKategorieMonSearch(
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }, "Regionalplan"),

        BODENRICHTWERTE(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    return null;
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

    @Getter private CidsBean flaecheBean;
    @Getter private CidsBean templateBean;

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
            MetaObjectNode flaecheMon = (body != null)
                ? ((body instanceof MetaObjectNode)
                    ? (MetaObjectNode)body : getFor(new String((byte[])body), "pf_potenzialflaeche", "nummer")) : null;
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
                flaecheBean = getMetaService().getMetaObject(
                            getUser(),
                            flaecheMon.getObjectId(),
                            flaecheMon.getClassId(),
                            getConnectionContext()).getBean();
                templateBean = (templateMon != null)
                    ? getMetaService().getMetaObject(
                                getUser(),
                                templateMon.getObjectId(),
                                templateMon.getClassId(),
                                getConnectionContext()).getBean() : null;

                final CidsBean kampagne = (CidsBean)flaecheBean.getProperty("kampagne");
                CidsBean selectedTemplateBean = null;
                if (templateBean != null) {
                    selectedTemplateBean = templateBean;
                } else {
                    if (kampagne != null) {
                        final Collection<CidsBean> templateBeans = kampagne.getBeanCollectionProperty(
                                "n_steckbrieftemplates");
                        selectedTemplateBean = ((templateBeans != null) && !templateBeans.isEmpty())
                            ? templateBeans.iterator().next() : null;
                        final Integer mainSteckbriefId = (Integer)kampagne.getProperty("haupt_steckbrieftemplate_id");
                        if (mainSteckbriefId != null) {
                            for (final CidsBean templateBean : templateBeans) {
                                if ((templateBean != null)
                                            && (mainSteckbriefId == templateBean.getMetaObject().getId())) {
                                    selectedTemplateBean = templateBean;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (selectedTemplateBean != null) {
                    final String confAttr = (String)selectedTemplateBean.getProperty("conf_attr");
                    if ((confAttr != null) && !confAttr.trim().isEmpty()
                                && (DomainServerImpl.getServerInstance().getConfigAttr(
                                        getUser(),
                                        confAttr,
                                        getConnectionContext()) == null)) {
                        throw new Exception("kein Recht an Konfigurationsattribut " + confAttr);
                    }

                    final String template = (String)selectedTemplateBean.getProperty("link");
                    if (BEAN_RESOURCE_MAP.get(template) == null) {
                        BEAN_RESOURCE_MAP.put(template, new JasperReportServerResource(template));
                    }

                    final JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(
                                flaecheBean));
                    final Map<String, Object> parameters = generateParams();
                    parameters.put(
                        "SUBREPORT_DIR",
                        DomainServerImpl.getServerProperties().getServerResourcesBasePath()
                                + "/");
                    synchronized (this) {
                        CURRENT_TEMPLATE = template;
                        return generateReport(parameters, dataSource);
                    }
                } else {
                    throw new Exception("no template found");
                }
            } else {
                return null;
            }
        } catch (final Exception ex) {
            LOG.error(ex, ex);
            return ex;
        } finally {
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    protected JasperReport getJasperReport() throws Exception {
        return ServerResourcesLoader.getInstance().loadJasperReport(BEAN_RESOURCE_MAP.get(CURRENT_TEMPLATE));
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public Map generateParams() throws Exception {
        final HashMap params = new HashMap();
        for (final Property property : Property.values()) {
            final String parameterName = property.name();
            final ReportProperty reportProperty = property.getValue();
            if (reportProperty instanceof VirtualReportProperty) {
                params.put(
                    parameterName,
                    ((VirtualReportProperty)reportProperty).calculateProperty(this));
            } else if (reportProperty instanceof MultiKeytableReportProperty) {
                final MultiKeytableReportProperty multiFieldReportProperty = (MultiKeytableReportProperty)
                    reportProperty;
                final Collection beans = flaecheBean.getBeanCollectionProperty(multiFieldReportProperty.getPath());
                if (beans != null) {
                    final Collection<String> strings = new ArrayList<>();
                    for (final Object bean : (Collection)beans) {
                        if (bean != null) {
                            strings.add(String.valueOf(bean));
                        }
                    }
                    params.put(parameterName, String.join(", ", strings));
                }
            } else if (reportProperty instanceof PathReportProperty) {
                final PathReportProperty fieldReportProperty = (PathReportProperty)reportProperty;
                final Object object = flaecheBean.getProperty(fieldReportProperty.getPath());
                final String value;
                if (object == null) {
                    value = null;
                } else if (object instanceof Date) {
                    value = SDF.format((Date)object);
                } else {
                    value = object.toString();
                }
                params.put(parameterName, value);
            } else if (reportProperty instanceof MonSearchReportProperty) {
                params.put(parameterName, "UNBEKANNTE PROPERTY");
            }
        }
        return params;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static PfMapConfiguration createPreconfiguredPfMapConfiguration() {
        final PfMapConfiguration config = new PfMapConfiguration();
        config.setBbX1(PotenzialflaechenMapfactoryProperties.getInstance().getHomeX1());
        config.setBbY1(PotenzialflaechenMapfactoryProperties.getInstance().getHomeY1());
        config.setBbX2(PotenzialflaechenMapfactoryProperties.getInstance().getHomeX2());
        config.setBbY2(PotenzialflaechenMapfactoryProperties.getInstance().getHomeY2());
        config.setSrs(PotenzialflaechenMapfactoryProperties.getInstance().getSrs());
        return config;
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
         * @param   serverAction  flaecheBean DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         *
         * @throws  Exception  DOCUMENT ME!
         */
        protected abstract Object calculateProperty(final PotenzialflaecheReportServerAction serverAction)
                throws Exception;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public abstract static class MonSearchReportProperty extends VirtualReportProperty {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new MonSearchReportProperty object.
         */
        public MonSearchReportProperty() {
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @param   serverAction  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        protected abstract MetaObjectNodeServerSearch createMonServerSearch(
                final PotenzialflaecheReportServerAction serverAction);

        @Override
        protected String calculateProperty(final PotenzialflaecheReportServerAction serverAction) {
            final MetaObjectNodeServerSearch serverSearch = createMonServerSearch(serverAction);
            if (serverSearch != null) {
                serverSearch.setUser(serverAction.getUser());
                if (serverSearch instanceof ConnectionContextStore) {
                    ((ConnectionContextStore)serverSearch).initWithConnectionContext(
                        serverAction.getConnectionContext());
                }
                final Map localServers = new HashMap<>();
                localServers.put("WUNDA_BLAU", serverAction.getMetaService());
                serverSearch.setActiveLocalServers(localServers);
                final Collection<String> names = new ArrayList<>();
                try {
                    for (final MetaObjectNode mon : serverSearch.performServerSearch()) {
                        final MetaObject mo = serverAction.getMetaService()
                                    .getMetaObject(
                                        serverAction.getUser(),
                                        mon.getObjectId(),
                                        mon.getClassId(),
                                        serverAction.getConnectionContext());
                        names.add((String)mo.getBean().getProperty("name"));
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

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    public static class PfMapConfiguration extends MapImageFactoryConfiguration {

        //~ Enums --------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @version  $Revision$, $Date$
         */
        public enum Type {

            //~ Enum constants -------------------------------------------------

            PF_ORTHO, PF_DGK,
        }

        //~ Instance fields ----------------------------------------------------

        private Type type;
        private Collection<Integer> ids;
        private Integer buffer;
    }
}
