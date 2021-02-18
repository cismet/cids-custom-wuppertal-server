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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import de.cismet.cids.custom.utils.StampedJasperReportServerAction;
import de.cismet.cids.custom.wunda_blau.search.server.KstSearch;
import de.cismet.cids.custom.wunda_blau.search.server.WohnlagenKategorisierungSearch;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
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
        implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "potenzialflaecheReport";

    private static final Map<String, JasperReportServerResource> BEAN_RESOURCE_MAP = new HashMap<>();
    private static String CURRENT_TEMPLATE = "";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Property {

        //~ Enum constants -----------------------------------------------------

        BEZEICHNUNG(new SingleFieldReportProperty("bezeichnung")), NUMMER(new SingleFieldReportProperty("nummer")),
        BESCHREIBUNG_FLAECHE(new SingleFieldReportProperty("beschreibung_flaeche")),
        NOTWENDIGE_MASSNAHMEN(new SingleFieldReportProperty("notwendige_massnahmen")),
        QUELLE(new SingleFieldReportProperty("fk_quelle")), STAND(new SingleFieldReportProperty("stand", true)),
        FLAECHENNUTZUNGSPLAN(new SingleFieldReportProperty("flaechennutzungsplan")),
        LAGETYP(new SingleFieldReportProperty("lagetyp")),
        VORHANDENE_BEBAUUNG(new SingleFieldReportProperty("vorhandene_bebauung")),
        TOPOGRAFIE(new SingleFieldReportProperty("topografie")), HANG(new SingleFieldReportProperty("fk_ausrichtung")),
        VERWERTBARKEIT(new SingleFieldReportProperty("aktivierbarkeit")),
        VERFUEGBBARKEIT(new SingleFieldReportProperty("verfuegbarkeit")),
        ENTWICKLUNGSAUSSSICHTEN(new SingleFieldReportProperty("fk_entwicklungsaussichten")),
        ENTWICKLUNGSSTAND(new SingleFieldReportProperty("fk_entwicklungsstand")),
        REVITALISIERUNG(new SingleFieldReportProperty("fk_revitalisierung")),
        AEUSSERE_ERSCHLIESSUNG(new SingleFieldReportProperty("fk_aeussere_erschliessung")),
        POTENZIALART(new SingleFieldReportProperty("fk_potenzialart")),
        WOHNEINHEITEN(new SingleFieldReportProperty("fk_wohneinheiten")),
        WOHNEINHEITEN_ANZAHL(new SingleFieldReportProperty("anzahl_wohneinheiten")),
        JAHR_NUTZUNGSAUFGABE(new SingleFieldReportProperty("bk_jahr_nutzungsaufgabe")),

        OEPNV_ANBINDUNG(new SingleFieldReportProperty("fk_oepnv")),

        BISHERIGE_NUTZUNG(new SingleFieldReportProperty("bisherige_nutzung")),

        BRACHFLAECHENKATEGORIE(new MultifieldReportProperty("arr_brachflaechen")),
        UMGEBUNGSNUTZUNG(new MultifieldReportProperty("umgebungsnutzung")),
        EMPFOHLENE_NUTZUNGEN(new MultifieldReportProperty("arr_empfohlene_nutzungen")),
        REGIONALPLAN(new MultifieldReportProperty("regionalplan")),
        RESTRIKTIONEN(new MultifieldReportProperty("arr_restriktionen")),
        HANDLUNGSDRUCK(new MultifieldReportProperty("handlungsdruck")),

        EIGENTUEMER(new VirtualReportProperty() {

                @Override
                protected Object calculateProperty(final PotenzialflaecheReportServerAction serverAction) {
                    return "PLATZHALTER";
                }
            }),
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
                protected Object calculateProperty(final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final Object geo = flaecheBean.getProperty("geometrie.geo_field");
                    double area = 0.0;

                    if (geo instanceof Geometry) {
                        area = ((Geometry)geo).getArea();
                    }

                    return Math.round(area * 100) / 100.0;
                }
            }),
        BEBAUUNGSPLAN(new VirtualReportProperty() {

                @Override
                protected Object calculateProperty(final PotenzialflaecheReportServerAction serverAction) {
                    return null;
                }
            }),
        STADTBEZIRK(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final KstSearch serverSearch = new KstSearch(
                            KstSearch.SearchFor.BEZIRK,
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }),
        QUARTIER(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final KstSearch serverSearch = new KstSearch(
                            KstSearch.SearchFor.QUARTIER,
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }),
        WOHNLAGEN(new MonSearchReportProperty() {

                @Override
                protected MetaObjectNodeServerSearch createMonServerSearch(
                        final PotenzialflaecheReportServerAction serverAction) {
                    final CidsBean flaecheBean = serverAction.getFlaecheBean();
                    final WohnlagenKategorisierungSearch serverSearch = new WohnlagenKategorisierungSearch(
                            (Geometry)flaecheBean.getProperty("geometrie.geo_field"));
                    return serverSearch;
                }
            }),
        STADTRAUMTYPEN(new VirtualReportProperty() {

                @Override
                protected Object calculateProperty(final PotenzialflaecheReportServerAction serverAction) {
                    return "PLATZHALTER";
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

        IMAGE_ORTHO, IMAGE_DGK
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Getter private BufferedImage orthoImage;
    @Getter private BufferedImage dgkImage;
    @Getter private CidsBean flaecheBean;

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
                flaecheBean = getMetaService().getMetaObject(
                            getUser(),
                            flaecheMon.getObjectId(),
                            flaecheMon.getClassId(),
                            getConnectionContext()).getBean();

                final String template = (String)flaecheBean.getProperty("kampagne.steckbrieftemplate.link");
                if (BEAN_RESOURCE_MAP.get(template) == null) {
                    BEAN_RESOURCE_MAP.put(template, new JasperReportServerResource(template));
                }

                orthoImage = (orthoImageBytes != null) ? ImageIO.read(new ByteArrayInputStream(orthoImageBytes)) : null;
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
            } else if (reportProperty instanceof MultifieldReportProperty) {
                final MultifieldReportProperty multiFieldReportProperty = (MultifieldReportProperty)reportProperty;
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
            } else if (reportProperty instanceof SingleFieldReportProperty) {
                final SingleFieldReportProperty fieldReportProperty = (SingleFieldReportProperty)reportProperty;
                final Object object = flaecheBean.getProperty(fieldReportProperty.getPath());
                params.put(
                    parameterName,
                    (object != null)
                        ? (((SingleFieldReportProperty)reportProperty).isRaw() ? object : object.toString()) : null);
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
    public static class SingleFieldReportProperty extends ReportProperty {

        //~ Instance fields ----------------------------------------------------

        private final String path;
        private final boolean raw;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new SingleFieldReportProperty object.
         *
         * @param  path  DOCUMENT ME!
         */
        public SingleFieldReportProperty(final String path) {
            this(path, false);
        }

        /**
         * Creates a new SingleFieldReportProperty object.
         *
         * @param  path  DOCUMENT ME!
         * @param  raw   DOCUMENT ME!
         */
        public SingleFieldReportProperty(final String path, final boolean raw) {
            this.path = path;
            this.raw = raw;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class MultifieldReportProperty extends SingleFieldReportProperty {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new MultifieldReportProperty object.
         *
         * @param  path  DOCUMENT ME!
         */
        public MultifieldReportProperty(final String path) {
            super(path);
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
        protected Object calculateProperty(final PotenzialflaecheReportServerAction serverAction) {
            final MetaObjectNodeServerSearch serverSearch = createMonServerSearch(serverAction);
            serverSearch.setUser(serverAction.getUser());
            if (serverSearch instanceof ConnectionContextStore) {
                ((ConnectionContextStore)serverSearch).initWithConnectionContext(serverAction.getConnectionContext());
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
        }
    }
}
