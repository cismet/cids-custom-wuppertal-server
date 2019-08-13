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

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.awt.Image;
import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import de.cismet.cids.custom.utils.StampedJasperReportServerAction;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

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
        implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "potenzialflaecheReport";

    private static final Map<String, JasperReportServerResource> BEAN_RESOURCE_MAP = new HashMap<>();
    private static String CURRENT_TEMPLATE = "";

    public static final String PROP_BEZEICHNUNG = "bezeichnung";
    public static final String PROP_NUMMER = "nummer";
    public static final String PROP_BESCHREIBUNG_FLAECHE = "beschreibung_flaeche";
    public static final String PROP_NOTWENDIGE_MASSNAHMEN = "notwendige_massnahmen";
    public static final String PROP_QUELLE = "quelle";
    public static final String PROP_STAND = "stand";
    public static final String PROP_STADTBEZIRK = "stadtbezirk";
    public static final String PROP_GROESSE = "groesse";
    public static final String PROP_EIGENTUEMER = "eigentuemer";
    public static final String PROP_REGIONALPLAN = "regionalplan";
    public static final String PROP_FLAECHENNUTZUNGSPLAN = "flaechennutzungsplan";
    public static final String PROP_BEBAUUNGSPLAN = "bebauungsplan";
    public static final String PROP_WBPF_NUMMER = "wbpf_nummer";
    public static final String PROP_BK_GEWERBE_INDUSTRIE = "bk_gewerbe_industrie";
    public static final String PROP_BK_MILITAER = "bk_militaer";
    public static final String PROP_BK_VERKEHR = "bk_verkehr";
    public static final String PROP_BK_INFRASTRUKTUR_SOZIAL = "bk_infrastruktur_sozial";
    public static final String PROP_BK_INFRASTRUKTUR_TECHNISCH = "bk_infrastruktur_technisch";
    public static final String PROP_BK_EINZELHANDEL = "bk_einzelhandel";
    public static final String PROP_BK_NUTZUNGSAUFGABE = "bk_jahr_nutzungsaufgabe";
    public static final String PROP_GEWERBE_PRODUKTIONSORIENTIERT = "gnn_gewerbe_produktorientiert";
    public static final String PROP_GNN_GEWERBE_DIENSTLEISTUNG = "gnn_gewerbe_dienstleistung";
    public static final String PROP_GNN_WOHNEN = "gnn_wohnen";
    public static final String PROP_GNN_FREIRAUM = "gnn_freiraum";
    public static final String PROP_GNN_FREIZEIT = "gnn_freizeit";
    public static final String PROP_GNN_EINZELHANDEL = "gnn_einzelhandel";
    public static final String PROP_GNN_SONSTIGES = "gnn_sonstiges";
    public static final String PROP_LAGETYP = "lagetyp";
    public static final String PROP_BISHERIGE_NUTZUNG = "bisherige_nutzung";
    public static final String PROP_VORHANDENE_BEBAUUNG = "vorhandene_bebauung";
    public static final String PROP_UMGEBUNGSNUTZUNG = "umgebungsnutzung";
    public static final String PROP_TOPOGRAFIE = "topografie";
    public static final String PROP_RESTRIKTIONEN = "restriktionen";
    public static final String PROP_AEUSSERE_ERSCHLIESSUNG = "aeussere_erschliessung";
    public static final String PROP_INNERE_ERSCHLIESSUNG = "innere_erschliessung";
    public static final String PROP_OEPNV_ANBINDUNG = "oepnv_anbindung";
    public static final String PROP_ZENTRENNAEHE = "zentrennaehe";
    public static final String PROP_VERFUEGBARKEIT = "verfuegbarkeit";
    public static final String PROP_ART_DER_NUTZUNG = "art_der_nutzung";
    public static final String PROP_REVITALISIERUNG = "revitalisierung";
    public static final String PROP_ENTWICKLUNGSAUSSSICHTEN = "entwicklungsaussichten";
    public static final String PROP_HANDLUNGSDRUCK = "handlungsdruck";
    public static final String PROP_AKTIVIERBARKEIT = "aktivierbarkeit";
    public static final String PROP_ZENTRALER_VERSORGUNGSBEREICH = "zentraler_versorgungsbereich";
    public static final String PROP_WBPF_NACHFOLGENUTZUNG = "wbpf_nachfolgenutzung";

    public static final Map<String, ReportProperty> REPORT_PROPERTY_MAP = new HashMap<String, ReportProperty>();

    static {
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_BEZEICHNUNG,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_BEZEICHNUNG, "lblBezeichnung"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_NUMMER,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_NUMMER, "lblNummer"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_BESCHREIBUNG_FLAECHE,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_BESCHREIBUNG_FLAECHE, "lblFlaeche"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_NOTWENDIGE_MASSNAHMEN,
            new StringReportProperty(
                PotenzialflaecheReportServerAction.PROP_NOTWENDIGE_MASSNAHMEN,
                "lblNaechsteSchritte"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_QUELLE,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_QUELLE, "lblQuelle"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_STAND,
            new ReportProperty(PotenzialflaecheReportServerAction.PROP_STAND, "lblStand"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_FLAECHENNUTZUNGSPLAN,
            new StringReportProperty(
                PotenzialflaecheReportServerAction.PROP_FLAECHENNUTZUNGSPLAN,
                "lblFlaechennutzung"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_BEBAUUNGSPLAN,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_BEBAUUNGSPLAN, "lblBebauungplan"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_WBPF_NUMMER,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_WBPF_NUMMER, "lblWbpfNummer"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_LAGETYP,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_LAGETYP, "lblLagetyp"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_VORHANDENE_BEBAUUNG,
            new StringReportProperty(
                PotenzialflaecheReportServerAction.PROP_VORHANDENE_BEBAUUNG,
                "lblVorhandeneBebauung"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_TOPOGRAFIE,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_TOPOGRAFIE, "lblTopografie"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_RESTRIKTIONEN,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_RESTRIKTIONEN, "lblRestriktionen"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_AEUSSERE_ERSCHLIESSUNG,
            new StringReportProperty(
                PotenzialflaecheReportServerAction.PROP_AEUSSERE_ERSCHLIESSUNG,
                "lblAessereErschl"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_INNERE_ERSCHLIESSUNG,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_INNERE_ERSCHLIESSUNG, "lblInnereErschl"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_ZENTRENNAEHE,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_ZENTRENNAEHE, "lblZentrennaehe"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_VERFUEGBARKEIT,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_VERFUEGBARKEIT, "lblVerfuegbarkeit"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_ART_DER_NUTZUNG,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_ART_DER_NUTZUNG, "lblArtDerNutzung"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_REVITALISIERUNG,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_REVITALISIERUNG, "lblRevitalisierung"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_WBPF_NACHFOLGENUTZUNG,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_WBPF_NACHFOLGENUTZUNG, "lblWbpfNn"));
        REPORT_PROPERTY_MAP.put(
            "entwicklungsaussischten",
            new StringReportProperty(
                PotenzialflaecheReportServerAction.PROP_ENTWICKLUNGSAUSSSICHTEN,
                "entwicklungsaussischten",
                "lblEntwicklungsausssichten"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_HANDLUNGSDRUCK,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_HANDLUNGSDRUCK, "lblHandlungsdruck"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_AKTIVIERBARKEIT,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_AKTIVIERBARKEIT, "lblAktivierbarkeit"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_STADTBEZIRK,
            new VirtualReportProperty(PotenzialflaecheReportServerAction.PROP_STADTBEZIRK, "lblStadtbezirk") {

                @Override
                protected Object calculateProperty(final CidsBean flaecheBean) {
                    return "Stadtbezirk1"; // todo: richtiger Bezirk
                }
            });
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_GROESSE,
            new VirtualReportProperty(PotenzialflaecheReportServerAction.PROP_GROESSE, "lblFlaechengroesse") {

                @Override
                protected Object calculateProperty(final CidsBean flaecheBean) {
                    final Object geo = flaecheBean.getProperty("geometrie.geo_field");
                    double area = 0.0;

                    if (geo instanceof Geometry) {
                        area = ((Geometry)geo).getArea();
                    }

                    return Math.round(area * 100) / 100.0;
                }
            });
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_EIGENTUEMER,
            new VirtualReportProperty(PotenzialflaecheReportServerAction.PROP_EIGENTUEMER, null) {

                @Override
                protected Object calculateProperty(final CidsBean flaecheBean) {
                    return "privat"; // todo: richtiger Eigentümer
                }
            });
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_ZENTRALER_VERSORGUNGSBEREICH,
            new VirtualReportProperty(PotenzialflaecheReportServerAction.PROP_ZENTRALER_VERSORGUNGSBEREICH, null) {

                @Override
                protected Object calculateProperty(final CidsBean flaecheBean) {
                    return true; // todo: richtigen Wert ermitteln
                }
            });

        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_REGIONALPLAN,
            new StringListReportProperty(PotenzialflaecheReportServerAction.PROP_REGIONALPLAN, "lblRegionalplan"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_BISHERIGE_NUTZUNG,
            new StringListReportProperty(
                PotenzialflaecheReportServerAction.PROP_BISHERIGE_NUTZUNG,
                "lblBisherigeNutzung"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_UMGEBUNGSNUTZUNG,
            new StringListReportProperty(
                PotenzialflaecheReportServerAction.PROP_UMGEBUNGSNUTZUNG,
                "lblUmgebungsnutzung"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_OEPNV_ANBINDUNG,
            new StringListReportProperty(PotenzialflaecheReportServerAction.PROP_OEPNV_ANBINDUNG, "lblOepnv"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_BK_GEWERBE_INDUSTRIE,
            new ReportProperty(PotenzialflaecheReportServerAction.PROP_BK_GEWERBE_INDUSTRIE, "cbBfGewerbe"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_BK_MILITAER,
            new ReportProperty(PotenzialflaecheReportServerAction.PROP_BK_MILITAER, "cbBfMilitaer"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_BK_VERKEHR,
            new ReportProperty(PotenzialflaecheReportServerAction.PROP_BK_VERKEHR, "cbBfVerkehr"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_BK_INFRASTRUKTUR_TECHNISCH,
            new ReportProperty(
                PotenzialflaecheReportServerAction.PROP_BK_INFRASTRUKTUR_TECHNISCH,
                "cbInfrastrukturTechnisch"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_BK_INFRASTRUKTUR_SOZIAL,
            new ReportProperty(
                PotenzialflaecheReportServerAction.PROP_BK_INFRASTRUKTUR_SOZIAL,
                "cbInfrastrukturSozial"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_BK_EINZELHANDEL,
            new ReportProperty(PotenzialflaecheReportServerAction.PROP_BK_EINZELHANDEL, "cbFbEinzelhandel"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_BK_NUTZUNGSAUFGABE,
            new ReportProperty(PotenzialflaecheReportServerAction.PROP_BK_NUTZUNGSAUFGABE, "lblNutzungsaufgabe"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_GEWERBE_PRODUKTIONSORIENTIERT,
            new ReportProperty(
                PotenzialflaecheReportServerAction.PROP_GEWERBE_PRODUKTIONSORIENTIERT,
                "cbNnGewerbeProdukt"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_GNN_GEWERBE_DIENSTLEISTUNG,
            new ReportProperty(
                PotenzialflaecheReportServerAction.PROP_GNN_GEWERBE_DIENSTLEISTUNG,
                "cbGewerbeDienstleistung"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_GNN_WOHNEN,
            new ReportProperty(PotenzialflaecheReportServerAction.PROP_GNN_WOHNEN, "cbWohnen"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_GNN_FREIRAUM,
            new ReportProperty(PotenzialflaecheReportServerAction.PROP_GNN_FREIRAUM, "cbFreiraum"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_GNN_FREIZEIT,
            new ReportProperty(PotenzialflaecheReportServerAction.PROP_GNN_FREIZEIT, "cbFreizeit"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_GNN_EINZELHANDEL,
            new ReportProperty(PotenzialflaecheReportServerAction.PROP_GNN_EINZELHANDEL, "cbEinzelhandel"));
        REPORT_PROPERTY_MAP.put(
            PotenzialflaecheReportServerAction.PROP_GNN_SONSTIGES,
            new StringReportProperty(PotenzialflaecheReportServerAction.PROP_GNN_SONSTIGES, "lblGnnSonstiges"));
    }

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        IMAGE_ORTHO, IMAGE_DGK
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   cidsBean    DOCUMENT ME!
     * @param   orthoImage  DOCUMENT ME!
     * @param   dgkImage    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Map generateParamters(final CidsBean cidsBean, final Image orthoImage, final Image dgkImage) {
        final HashMap params = new HashMap();
        for (final String dbProp : REPORT_PROPERTY_MAP.keySet()) {
            final ReportProperty rp = REPORT_PROPERTY_MAP.get(dbProp);
            rp.addParameterToMap(params, cidsBean);
        }
        params.put("KARTE_ORTHO", orthoImage);
        params.put("KARTE_DGK", dgkImage);
        return params;
    }

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final MetaObjectNode flaecheMon = (MetaObjectNode)body;
        byte[] dgkImageBytes = null;
        byte[] orthoImageBytes = null;

        try {
            if (flaecheMon != null) {
                if (params != null) {
                    for (final ServerActionParameter sap : params) {
                        if (sap.getKey().equals(Parameter.IMAGE_DGK.toString())) {
                            dgkImageBytes = (byte[])sap.getValue();
                        } else if (sap.getKey().equals(Parameter.IMAGE_ORTHO.toString())) {
                            orthoImageBytes = (byte[])sap.getValue();
                        }
                    }
                }
                final CidsBean flaecheBean = getMetaService().getMetaObject(
                            getUser(),
                            flaecheMon.getObjectId(),
                            flaecheMon.getClassId(),
                            getConnectionContext())
                            .getBean();

                final String template = (String)flaecheBean.getProperty("kampagne.steckbrieftemplate.link");
                if (BEAN_RESOURCE_MAP.get(template) == null) {
                    BEAN_RESOURCE_MAP.put(template, new JasperReportServerResource(template));
                }

                final BufferedImage orthoImage = (orthoImageBytes != null)
                    ? ImageIO.read(new ByteArrayInputStream(orthoImageBytes)) : null;
                final BufferedImage dgkImage = (dgkImageBytes != null)
                    ? ImageIO.read(new ByteArrayInputStream(dgkImageBytes)) : null;

                final JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(
                            flaecheBean));
                final Map<String, Object> parameters = generateParamters(flaecheBean, orthoImage, dgkImage);
                parameters.put(
                    "SUBREPORT_DIR",
                    DomainServerImpl.getServerProperties().getServerResourcesBasePath()
                            + "/");
                synchronized (this) {
                    CURRENT_TEMPLATE = template;
                    return generateReport(parameters, dataSource);
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

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class ReportProperty {

        //~ Instance fields ----------------------------------------------------

        protected final String parameterName;
        protected final String editorLabelName;
        private final String dbName;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new ReportProperty object.
         *
         * @param  parameterName    DOCUMENT ME!
         * @param  editorLabelName  DOCUMENT ME!
         */
        public ReportProperty(final String parameterName, final String editorLabelName) {
            this.parameterName = parameterName.toUpperCase();
            this.dbName = parameterName;
            this.editorLabelName = editorLabelName;
        }

        /**
         * Creates a new ReportProperty object.
         *
         * @param  parameterName    DOCUMENT ME!
         * @param  dbName           DOCUMENT ME!
         * @param  editorLabelName  DOCUMENT ME!
         */
        public ReportProperty(final String parameterName, final String dbName, final String editorLabelName) {
            this.parameterName = parameterName.toUpperCase();
            this.dbName = dbName;
            this.editorLabelName = editorLabelName;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  the dbName
         */
        public String getDbName() {
            return dbName;
        }

        /**
         * DOCUMENT ME!
         *
         * @param   cidsBean  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public Map generateParams(final CidsBean cidsBean) {
            final HashMap params = new HashMap();
            params.put("", LOG);

            for (final String dbProp : REPORT_PROPERTY_MAP.keySet()) {
                final ReportProperty rp = REPORT_PROPERTY_MAP.get(dbProp);
                rp.addParameterToMap(params, cidsBean);
            }

            return params;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  map          DOCUMENT ME!
         * @param  flaecheBean  DOCUMENT ME!
         */
        public void addParameterToMap(final Map map, final CidsBean flaecheBean) {
            map.put(parameterName, flaecheBean.getProperty(dbName));
        }

        /**
         * DOCUMENT ME!
         *
         * @return  the editorLabel
         */
        public String getEditorLabelName() {
            return editorLabelName;
        }

        /**
         * DOCUMENT ME!
         *
         * @param   o  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        protected String toString(final Object o) {
            if (o == null) {
                return null;
            } else {
                return o.toString();
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class StringReportProperty extends ReportProperty {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new StringReportProperty object.
         *
         * @param  parameterName    DOCUMENT ME!
         * @param  editorLabelName  DOCUMENT ME!
         */
        public StringReportProperty(final String parameterName, final String editorLabelName) {
            super(parameterName, editorLabelName);
        }

        /**
         * Creates a new StringReportProperty object.
         *
         * @param  parameterName    DOCUMENT ME!
         * @param  dbName           DOCUMENT ME!
         * @param  editorLabelName  DOCUMENT ME!
         */
        public StringReportProperty(final String parameterName, final String dbName, final String editorLabelName) {
            super(parameterName, dbName, editorLabelName);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void addParameterToMap(final Map map, final CidsBean flaecheBean) {
            map.put(parameterName, toString(flaecheBean.getProperty(getDbName())));
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
         *
         * @param  parameterName    DOCUMENT ME!
         * @param  editorLabelName  DOCUMENT ME!
         */
        public VirtualReportProperty(final String parameterName, final String editorLabelName) {
            super(parameterName, editorLabelName);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void addParameterToMap(final Map map, final CidsBean flaecheBean) {
            map.put(parameterName, calculateProperty(flaecheBean));
        }

        /**
         * DOCUMENT ME!
         *
         * @param   flaecheBean  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        protected abstract Object calculateProperty(CidsBean flaecheBean);
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class StringListReportProperty extends ReportProperty {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new StringListReportProperty object.
         *
         * @param  parameterName    DOCUMENT ME!
         * @param  editorLabelName  DOCUMENT ME!
         */
        public StringListReportProperty(final String parameterName, final String editorLabelName) {
            super(parameterName, editorLabelName);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void addParameterToMap(final Map map, final CidsBean flaecheBean) {
            map.put(parameterName,
                collectionToList((Collection)flaecheBean.getProperty(getDbName())));
        }

        /**
         * DOCUMENT ME!
         *
         * @param   c  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        protected String collectionToList(final Collection c) {
            final StringBuilder sb = new StringBuilder();
            boolean firstString = true;

            for (final Object o : c) {
                if (firstString) {
                    firstString = false;
                } else {
                    sb.append(", ");
                }
                sb.append(o.toString());
            }

            return sb.toString();
        }
    }
}