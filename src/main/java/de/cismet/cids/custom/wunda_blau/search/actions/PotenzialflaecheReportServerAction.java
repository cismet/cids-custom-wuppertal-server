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

import com.vividsolutions.jts.geom.Geometry;

import lombok.Getter;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import de.cismet.cids.custom.utils.StampedJasperReportServerAction;
import de.cismet.cids.custom.wunda_blau.search.server.AlkisLandparcelGeometryMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.BplaeneMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.FnpHauptnutzungenMonSearch;
import de.cismet.cids.custom.wunda_blau.search.server.KstGeometryMonSearch;
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

        BEZEICHNUNG(new SimpleFieldReportProperty("bezeichnung", String.class.getCanonicalName())),
        NUMMER(new SimpleFieldReportProperty("nummer", String.class.getCanonicalName())),
        BESCHREIBUNG_FLAECHE(new SimpleFieldReportProperty("beschreibung_flaeche", String.class.getCanonicalName())),
        NOTWENDIGE_MASSNAHMEN(new SimpleFieldReportProperty("notwendige_massnahmen", String.class.getCanonicalName())),
        QUELLE(new SimpleFieldReportProperty("quelle", String.class.getCanonicalName())),
        WOHNEINHEITEN_ANZAHL(new SimpleFieldReportProperty("anzahl_wohneinheiten", Integer.class.getCanonicalName())),
        FESTSETZUNGEN_BPLAN(new SimpleFieldReportProperty("festsetzungen_bplan", String.class.getCanonicalName())),
        FESTSETZUNGEN_BPLAN_STAND(new SimpleFieldReportProperty(
                "stand_festsetzungen_bplan",
                Date.class.getCanonicalName())),
        STAND(new SimpleFieldReportProperty("stand", Date.class.getCanonicalName())),
        JAHR_NUTZUNGSAUFGABE(new SimpleFieldReportProperty("jahr_brachflaeche", Date.class.getCanonicalName())),

        KAMPAGNE(new KeytableReportProperty("kampagne", "pf_kampagne")),
        LAGEBEWERTUNG_VERKEHR(new KeytableReportProperty("fk_lagebewertung_verkehr", "pf_lagebewertung_verkehr")),
        SIEDLUNGSRAEUMLICHE_LAGE(new KeytableReportProperty(
                "fk_siedlungsraeumliche_lage",
                "pf_siedlungsraeumliche_lage")),
        VORHANDENE_BEBAUUNG(new KeytableReportProperty("bestand_bebauung", "???")),
        TOPOGRAFIE(new KeytableReportProperty("topografie", "pf_topografie")),
        HANG(new KeytableReportProperty("fk_ausrichtung", "???")),
        VERWERTBARKEIT(new KeytableReportProperty("fk_verwertbarkeit", "pf_verwertbarkeit")),
        VERFUEGBBARKEIT(new KeytableReportProperty("verfuegbarkeit", "pf_verfuegbarkeit")),
        ENTWICKLUNGSAUSSSICHTEN(new KeytableReportProperty("fk_entwicklungsaussichten", "pf_entwicklungsaussichten")),
        ENTWICKLUNGSSTAND(new KeytableReportProperty("fk_entwicklungsstand", "pf_entwicklungsstand")),
        REVITALISIERUNG(new KeytableReportProperty("fk_revitalisierung", "pf_revitalisierung")),
        AEUSSERE_ERSCHLIESSUNG(new KeytableReportProperty("fk_aeussere_erschliessung", "pf_aeussere_erschliessung")),
        POTENZIALART(new KeytableReportProperty("fk_potenzialart", "pf_potenzialart")),
        WOHNEINHEITEN(new KeytableReportProperty("fk_wohneinheiten", "pf_wohneinheiten")),
        OEPNV_ANBINDUNG(new KeytableReportProperty("fk_oepnv", "pf_oepnv")),
        KLIMAINFORMATIONEN(new KeytableReportProperty("fk_klimainformationen", "pf_klimainformationen")),
        VERSIEGELUNG(new KeytableReportProperty("fk_versiegelung", "pf_versiegelung")),
        BAUORDNUNGSRECHT_GENEHMIGUNG(new KeytableReportProperty(
                "fk_bauordnungsrecht_genehmigung",
                "pf_bauordnungsrecht_genehmigung")),
        BAUORDNUNGSRECHT_BAULAST(new KeytableReportProperty(
                "fk_bauordnungsrecht_baulast",
                "pf_bauordnungsrecht_baulast")),
        HANDLUNGSDRUCK(new KeytableReportProperty("handlungsdruck", "pf_handlungsdruck")),
        HANDLUNGSPRIORITAET(new KeytableReportProperty("fk_handlungsprioritaet", "pf_handlungsprioritaet")),

        EIGENTUEMER(new MultiKeytableReportProperty(
                "arr_eigentuemer",
                "pf_eigentuemer_arr.fk_eigentuemer",
                "pf_eigentuemer")),
        BISHERIGE_NUTZUNG(new MultiKeytableReportProperty(
                "bisherige_nutzung",
                "pf_potenzialflaechen_bisherige_nutzung.nutzung",
                "pf_nutzung")),
        UMGEBUNGSNUTZUNG(new MultiKeytableReportProperty(
                "umgebungsnutzung",
                "pf_potenzialflaechen_umgebungsnutzung.nutzung",
                "pf_nutzung")),
        NAEHE_ZU(new MultiKeytableReportProperty("arr_naehen_zu", "pf_naehen_zu.fk_naehe_zu", "pf_naehe_zu")),
        BRACHFLAECHENKATEGORIE(new MultiKeytableReportProperty(
                "arr_brachflaechen",
                "pf_brachflaechen.fk_brachflaeche",
                "pf_brachflaeche")),
        EMPFOHLENE_NUTZUNGEN(new MultiKeytableReportProperty(
                "arr_empfohlene_nutzungen",
                "pf_empfohlene_nutzungen.fk_empfohlene_nutzung",
                "pf_empfohlene_nutzung")),
        EMPFOHLENE_NUTZUNGEN_WOHNEN(new MultiKeytableReportProperty(
                "arr_empfohlene_nutzungen_wohnen",
                "pf_empfohlene_nutzungen_wohnen.fk_empfohlene_nutzung_wohnen",
                "pf_empfohlene_nutzung_wohnen")),
        REGIONALPLAN(new MultiKeytableReportProperty(
                "regionalplan",
                "pf_potenzialflaechen_pf_regionalplan.regionalplan",
                "pf_regionalplan")),
        RESTRIKTIONEN(new MultiKeytableReportProperty(
                "arr_restriktionen",
                "pf_restriktionen.fk_restriktion",
                "pf_restriktion")),

        KARTE_ORTHO(new VirtualReportProperty() {

                @Override
                protected Object calculateProperty(final PotenzialflaecheReportServerAction serverAction) {
                    return serverAction.getOrthoImage();
                }
            }),
        KARTE_DGK(new VirtualReportProperty() {

                @Override
                protected Object calculateProperty(final PotenzialflaecheReportServerAction serverAction) {
                    return serverAction.getDgkImage();
                }
            }),
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
            }),
        BEBAUUNGSPLAN(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final BplaeneMonSearch serverSearch = new BplaeneMonSearch(
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }),
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
            }),
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
            }),
        FLURSTUECKE(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final AlkisLandparcelGeometryMonSearch serverSearch = new AlkisLandparcelGeometryMonSearch(
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }),
        WOHNLAGEN(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final WohnlagenKategorisierungMonSearch serverSearch = new WohnlagenKategorisierungMonSearch(
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }),
        STADTRAUMTYPEN(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final StadtraumtypMonSearch serverSearch = new StadtraumtypMonSearch(
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }),
        FLAECHENNUTZUNGSPLAN(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final FnpHauptnutzungenMonSearch serverSearch = new FnpHauptnutzungenMonSearch(
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }),
        BODENRICHTWERTE(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    return null;
                }
            });

        //~ Instance fields ----------------------------------------------------

        @Getter private final ReportProperty value;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Property object.
         *
         * @param  value  DOCUMENT ME!
         */
        private Property(final ReportProperty value) {
            this.value = value;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        IMAGE_ORTHO, IMAGE_DGK, TEMPLATE
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Getter private BufferedImage orthoImage;
    @Getter private BufferedImage dgkImage;
    @Getter private CidsBean flaecheBean;
    @Getter private CidsBean templateBean;

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final MetaObjectNode flaecheMon = (MetaObjectNode)body;
        byte[] dgkImageBytes = null;
        byte[] orthoImageBytes = null;
        MetaObjectNode templateMon = null;

        try {
            if (flaecheMon != null) {
                if (params != null) {
                    for (final ServerActionParameter sap : params) {
                        if (sap.getKey().equals(Parameter.IMAGE_DGK.toString())) {
                            dgkImageBytes = (byte[])sap.getValue();
                        } else if (sap.getKey().equals(Parameter.IMAGE_ORTHO.toString())) {
                            orthoImageBytes = (byte[])sap.getValue();
                        } else if (sap.getKey().equals(Parameter.TEMPLATE.toString())) {
                            templateMon = (MetaObjectNode)sap.getValue();
                        }
                    }
                }
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

                    orthoImage = (orthoImageBytes != null) ? ImageIO.read(new ByteArrayInputStream(orthoImageBytes))
                                                           : null;
                    dgkImage = (dgkImageBytes != null) ? ImageIO.read(new ByteArrayInputStream(dgkImageBytes)) : null;

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
     */
    public Map generateParams() {
        final HashMap params = new HashMap();
        for (final Property property : Property.values()) {
            final String parameterName = property.toString();
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
         */
        protected abstract Object calculateProperty(final PotenzialflaecheReportServerAction serverAction);
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
}
