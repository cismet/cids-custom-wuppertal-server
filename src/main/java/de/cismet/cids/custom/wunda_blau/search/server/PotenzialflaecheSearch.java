/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.ActionService;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.LightweightMetaObject;
import Sirius.server.middleware.types.MetaObjectNode;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import com.vividsolutions.jts.geom.Geometry;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.io.IOException;
import java.io.StringReader;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.wunda_blau.search.actions.PotenzialflaecheReportServerAction;

import de.cismet.cids.server.actions.GetServerResourceServerAction;

import de.cismet.cidsx.base.types.Type;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = RestApiCidsServerSearch.class)
public class PotenzialflaecheSearch extends RestApiMonGeometrySearch
        implements StorableSearch<PotenzialflaecheSearch.Configuration> {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(PotenzialflaecheSearch.class);
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(FilterInfo.class, new FilterInfoDeserializer());
        module.addSerializer(FilterInfo.class, new FilterInfoSerializer());
        OBJECT_MAPPER.registerModule(module);
    }

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum SearchMode {

        //~ Enum constants -----------------------------------------------------

        AND, OR,
    }

    //~ Instance fields --------------------------------------------------------

    @Getter private Configuration configuration;
    @Getter private final boolean monSearch;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PotenzialflaecheSearch object.
     */
    public PotenzialflaecheSearch() {
        initBuffer();
        this.monSearch = false;
        setSearchInfo(new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Builtin Legacy Search to delegate the operation PotenzialflaecheSearchStatement to the cids Pure REST Search API.",
                Arrays.asList(
                    new SearchParameterInfo[] { new MySearchParameterInfo("filters", Type.UNDEFINED), }),
                new MySearchParameterInfo("return", Type.ENTITY_REFERENCE, true)));
    }

    /**
     * Creates a new PotenzialflaecheSearch object.
     *
     * @param  monSearch  DOCUMENT ME!
     */
    public PotenzialflaecheSearch(final boolean monSearch) {
        initBuffer();
        this.monSearch = true;
        setSearchInfo(null);
    }

    /**
     * Creates a new PotenzialflaecheSearch object.
     *
     * @param  searchConfiguration  DOCUMENT ME!
     * @param  geom                 DOCUMENT ME!
     */
    public PotenzialflaecheSearch(final Configuration searchConfiguration, final Geometry geom) {
        this(true);
        this.configuration = searchConfiguration;
        setGeometry(geom);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     */
    private void initBuffer() {
        final String searchPropBuffer = SearchProperties.getInstance().getIntersectsBuffer();
        if (searchPropBuffer != null) {
            try {
                setBuffer(Double.parseDouble(searchPropBuffer));
            } catch (final Exception ex) {
                LOG.warn(String.format("could not set buffer to %s", searchPropBuffer), ex);
            }
        }
    }

    @Override
    public Collection performServerSearch() {
        try {
            final List result = new ArrayList<>();
            final Properties properties = new Properties();
            final ActionService as = (ActionService)getActiveLocalServers().get("WUNDA_BLAU");
            properties.load(new StringReader(
                    (String)as.executeTask(
                        getUser(),
                        GetServerResourceServerAction.TASK_NAME,
                        WundaBlauServerResources.POTENZIALFLAECHEN_PROPERTIES.getValue(),
                        getConnectionContext())));

            final String query = createQuery();

            if (query != null) {
                final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

                final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
                for (final ArrayList al : resultList) {
                    final int cid = (Integer)al.get(0);
                    final int oid = (Integer)al.get(1);
                    final String name = (String)al.get(2);
                    if (isMonSearch()) {
                        result.add(new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null));
                    } else {
                        result.add(new LightweightMetaObject(cid, oid, "WUNDA_BLAU", getUser()));
                    }
                }
            }
            return result;
        } catch (final Exception ex) {
            LOG.error("error while searching for potenzialflaeche", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String createQuery() {
        final Collection<String> leftJoins = new LinkedHashSet<>();
        final Collection<String> wheresMain = new LinkedHashSet<>();

        leftJoins.add("geom ON pf_potenzialflaeche.geometrie = geom.id");
        leftJoins.add("pf_restriktionen ON pf_potenzialflaeche.id = pf_restriktionen.pf_potenzialflaeche_reference");
        leftJoins.add("pf_brachflaechen ON pf_potenzialflaeche.id = pf_brachflaechen.pf_potenzialflaeche_reference");
        leftJoins.add(
            "pf_potenzialflaechen_umgebungsnutzung ON pf_potenzialflaeche.id = pf_potenzialflaechen_umgebungsnutzung.pf_potenzialflaeche_reference");
        leftJoins.add(
            "pf_empfohlene_nutzungen_wohnen ON pf_potenzialflaeche.id = pf_empfohlene_nutzungen_wohnen.pf_potenzialflaeche_reference");
        leftJoins.add(
            "pf_potenzialflaechen_pf_regionalplan ON pf_potenzialflaeche.id = pf_potenzialflaechen_pf_regionalplan.pf_potenzialflaeche_reference");
        leftJoins.add("pf_naehen_zu ON pf_potenzialflaeche.id = pf_naehen_zu.pf_potenzialflaeche_reference");
        leftJoins.add(
            "pf_potenzialflaechen_bisherige_nutzung ON pf_potenzialflaeche.id = pf_potenzialflaechen_bisherige_nutzung.pf_potenzialflaeche_reference");
        leftJoins.add(
            "pf_eigentuemer_arr ON pf_potenzialflaeche.id = pf_eigentuemer_arr.pf_potenzialflaeche_reference");
        leftJoins.add(
            "pf_empfohlene_nutzungen ON pf_potenzialflaeche.id = pf_empfohlene_nutzungen.pf_potenzialflaeche_reference");
        leftJoins.add("pf_kampagne ON pf_potenzialflaeche.kampagne = pf_kampagne.id");
        leftJoins.add(
            "pf_entwicklungsaussichten ON pf_potenzialflaeche.fk_entwicklungsaussichten = pf_entwicklungsaussichten.id");
        leftJoins.add("pf_baulueckenart ON pf_potenzialflaeche.fk_baulueckenart = pf_baulueckenart.id");
        leftJoins.add(
            "pf_handlungsprioritaet ON pf_potenzialflaeche.fk_handlungsprioritaet = pf_handlungsprioritaet.id");
        leftJoins.add("pf_verwertbarkeit ON pf_potenzialflaeche.fk_verwertbarkeit = pf_verwertbarkeit.id");
        leftJoins.add("pf_wohneinheiten ON pf_potenzialflaeche.fk_wohneinheiten = pf_wohneinheiten.id");
        leftJoins.add("pf_entwicklungsstand ON pf_potenzialflaeche.fk_entwicklungsstand = pf_entwicklungsstand.id");
        leftJoins.add("pf_aktivierbarkeit ON pf_potenzialflaeche.aktivierbarkeit = pf_aktivierbarkeit.id");
        leftJoins.add("pf_ausrichtung ON pf_potenzialflaeche.fk_ausrichtung = pf_ausrichtung.id");
        leftJoins.add("pf_oepnv ON pf_potenzialflaeche.fk_oepnv = pf_oepnv.id");
        leftJoins.add("pf_handlungsdruck ON pf_potenzialflaeche.handlungsdruck = pf_handlungsdruck.id");
        leftJoins.add("pf_verfuegbarkeit ON pf_potenzialflaeche.verfuegbarkeit = pf_verfuegbarkeit.id");
        leftJoins.add("pf_topografie ON pf_potenzialflaeche.topografie = pf_topografie.id");
        leftJoins.add(
            "pf_aeussere_erschliessung ON pf_potenzialflaeche.fk_aeussere_erschliessung = pf_aeussere_erschliessung.id");
        leftJoins.add("pf_revitalisierung ON pf_potenzialflaeche.fk_revitalisierung = pf_revitalisierung.id");
        leftJoins.add(
            "pf_siedlungsraeumliche_lage ON pf_potenzialflaeche.fk_siedlungsraeumliche_lage = pf_siedlungsraeumliche_lage.id");
        leftJoins.add(
            "pf_lagebewertung_verkehr ON pf_potenzialflaeche.fk_lagebewertung_verkehr = pf_lagebewertung_verkehr.id");
        leftJoins.add("pf_potenzialart ON pf_potenzialflaeche.fk_potenzialart = pf_potenzialart.id");
        leftJoins.add("pf_versiegelung ON pf_potenzialflaeche.fk_versiegelung = pf_versiegelung.id");
        leftJoins.add(
            "pf_bauordnungsrecht_baulast ON pf_potenzialflaeche.fk_bauordnungsrecht_baulast = pf_bauordnungsrecht_baulast.id");
        leftJoins.add(
            "pf_bauordnungsrecht_genehmigung ON pf_potenzialflaeche.fk_bauordnungsrecht_genehmigung = pf_bauordnungsrecht_genehmigung.id");
        leftJoins.add("pf_klimainformationen ON pf_potenzialflaeche.fk_klimainformationen = pf_klimainformationen.id");
        leftJoins.add(
            "pf_veroeffentlichkeitsstatus ON pf_kampagne.veroeffentlichkeitsstatus = pf_veroeffentlichkeitsstatus.id");
        leftJoins.add(
            "pf_nutzung AS bisherige_nutzung ON pf_potenzialflaechen_bisherige_nutzung.nutzung = bisherige_nutzung.id");
        leftJoins.add(
            "pf_nutzung AS umgebungs_nutzung ON pf_potenzialflaechen_umgebungsnutzung.nutzung = umgebungs_nutzung.id");
        leftJoins.add("pf_regionalplan ON pf_potenzialflaechen_pf_regionalplan.regionalplan = pf_regionalplan.id");
        leftJoins.add("pf_restriktion ON pf_restriktionen.fk_restriktion = pf_restriktion.id");
        leftJoins.add(
            "pf_empfohlene_nutzung ON pf_empfohlene_nutzungen.fk_empfohlene_nutzung = pf_empfohlene_nutzung.id");
        leftJoins.add("pf_brachflaeche ON pf_brachflaechen.fk_brachflaeche = pf_brachflaeche.id");
        leftJoins.add("pf_eigentuemer ON pf_eigentuemer_arr.fk_eigentuemer = pf_eigentuemer.id");
        leftJoins.add(
            "pf_empfohlene_nutzung_wohnen ON pf_empfohlene_nutzungen_wohnen.fk_empfohlene_nutzung_wohnen = pf_empfohlene_nutzung_wohnen.id");
        leftJoins.add("pf_naehe_zu ON pf_naehen_zu.fk_naehe_zu = pf_naehe_zu.id");

        SearchMode searchMode = SearchMode.AND;
        if ((getConfiguration() != null) && (getConfiguration().getFilters() != null)) {
            searchMode = getConfiguration().getSearchMode();
            for (final FilterInfo filterInfo : getConfiguration().getFilters()) {
                if (filterInfo != null) {
                    final Object value = filterInfo.getValue();
                    final PotenzialflaecheReportServerAction.Property property = filterInfo.getProperty();
                    if (property != null) {
                        if (PotenzialflaecheReportServerAction.Property.GROESSE.equals(property)) {
                            if (value instanceof Date) {
                                wheresMain.add(String.format(
                                        "st_area(geom.geo_field) = %s",
                                        Double.toString((Double)value)));
                            } else if (value instanceof Double[]) {
                                final Double[] doubles = (Double[])value;
                                final String conditionFrom = (doubles[0] != null)
                                    ? String.format("st_area(geom.geo_field) >= '%s'", Double.toString(doubles[0]))
                                    : "TRUE";
                                final String conditionTo = (doubles[1] != null)
                                    ? String.format("st_area(geom.geo_field) <= '%s'", Double.toString(doubles[1]))
                                    : "TRUE";
                                wheresMain.add(String.format("(%s AND %s)", conditionFrom, conditionTo));
                            }
                        } else if (property.getValue()
                                    instanceof PotenzialflaecheReportServerAction.PathReportProperty) {
                            final PotenzialflaecheReportServerAction.PathReportProperty pathProp =
                                (PotenzialflaecheReportServerAction.PathReportProperty)property.getValue();
                            final String path = String.format("pf_potenzialflaeche.%s", pathProp.getPath());
                            if (value != null) {
                                final PotenzialflaecheReportServerAction.ReportProperty reportProperty =
                                    property.getValue();
                                if (reportProperty
                                            instanceof PotenzialflaecheReportServerAction.SimpleFieldReportProperty) {
                                    final String className =
                                        ((PotenzialflaecheReportServerAction.SimpleFieldReportProperty)reportProperty)
                                                .getClassName();
                                    if (String.class.getCanonicalName().equals(className)) {
                                        wheresMain.add(String.format("%s LIKE '%%%s%%'", path, value));
                                    } else if (Date.class.getCanonicalName().equals(className)) {
                                        if (value instanceof Date) {
                                            wheresMain.add(String.format(
                                                    "%s = '%s'",
                                                    path,
                                                    new SimpleDateFormat("yyyy-MM-dd").format((Date)value)));
                                        } else if (value instanceof Date[]) {
                                            final Date[] dates = (Date[])value;
                                            final String conditionFrom = (dates[0] != null)
                                                ? String.format(
                                                    "%s >= '%s'",
                                                    path,
                                                    new SimpleDateFormat("yyyy-MM-dd").format(dates[0])) : "TRUE";
                                            final String conditionTo = (dates[1] != null)
                                                ? String.format(
                                                    "%s <= '%s'",
                                                    path,
                                                    new SimpleDateFormat("yyyy-MM-dd").format(dates[1])) : "TRUE";
                                            wheresMain.add(String.format("(%s AND %s)", conditionFrom, conditionTo));
                                        }
                                    } else {
                                        wheresMain.add(String.format("%s = %s", path, String.valueOf(value)));
                                    }
                                } else if (reportProperty
                                            instanceof PotenzialflaecheReportServerAction.KeytableReportProperty) {
                                    wheresMain.add(String.format(
                                            "%s = %d",
                                            path,
                                            ((MetaObjectNode)value).getObjectId()));
                                }
                            } else {
                                wheresMain.add(String.format("%s IS NULL", path, value));
                            }
                        } else if (property.getValue()
                                    instanceof PotenzialflaecheReportServerAction.MonSearchReportProperty) {
                            String subject = null;
                            switch (property) {
                                case WOHNLAGEN: {
                                    subject = "wohnlage_kategorie.id";
                                    leftJoins.add(
                                        "( SELECT wohnlage_kategorie.id, sub_geom.geo_field FROM wohnlage_flaeche LEFT JOIN geom sub_geom ON sub_geom.id = wohnlage_flaeche.fk_geom LEFT JOIN wohnlage_kategorie ON wohnlage_kategorie.id = wohnlage_flaeche.fk_wohnlage_kategorie ) wohnlage_kategorie ON wohnlage_kategorie.geo_field && geom.geo_field AND st_intersects(wohnlage_kategorie.geo_field, geom.geo_field)");
                                }
                                break;
                                case STADTRAUMTYPEN: {
                                    subject = "srt_kategorie.id";
                                    leftJoins.add(
                                        "( SELECT srt_kategorie.id, sub_geom.geo_field FROM srt_kategorie LEFT JOIN srt_flaeche ON srt_flaeche.fk_kategorie = srt_kategorie.id LEFT JOIN geom sub_geom ON sub_geom.id = srt_flaeche.fk_geom ) srt_kategorie ON srt_kategorie.geo_field && geom.geo_field AND st_intersects(srt_kategorie.geo_field, geom.geo_field)");
                                }
                                break;
                                case QUARTIER: {
                                    subject = "kst_quartier.id";
                                    leftJoins.add(
                                        "( SELECT kst_quartier.id, sub_geom.geo_field FROM kst_quartier LEFT JOIN geom sub_geom ON sub_geom.id = kst_quartier.georeferenz ) kst_quartier ON kst_quartier.geo_field && geom.geo_field AND st_intersects(kst_quartier.geo_field, geom.geo_field)");
                                }
                                break;
                                case STADTBEZIRK: {
                                    subject = "kst_stadtbezirk.id";
                                    leftJoins.add(
                                        "( SELECT kst_stadtbezirk.id, sub_geom.geo_field FROM kst_stadtbezirk LEFT JOIN geom sub_geom ON sub_geom.id = kst_stadtbezirk.georeferenz ) kst_stadtbezirk ON kst_stadtbezirk.geo_field && geom.geo_field AND st_intersects(kst_stadtbezirk.geo_field, geom.geo_field)");
                                }
                                break;
                                case FLAECHENNUTZUNGSPLAN: {
                                    subject = "fnp_hn_kategorie.id";
                                    leftJoins.add(
                                        "( SELECT fnp_hn_kategorie.id, sub_geom.geo_field FROM fnp_hn_kategorie LEFT JOIN fnp_hn_flaeche ON fnp_hn_flaeche.fk_fnp_hn_kategorie = fnp_hn_kategorie.id LEFT JOIN geom sub_geom ON sub_geom.id = fnp_hn_flaeche.fk_geom) fnp_hn_kategorie ON fnp_hn_kategorie.geo_field && geom.geo_field AND st_intersects(fnp_hn_kategorie.geo_field, geom.geo_field)");
                                }
                                break;
                                case REGIONALPLAN: {
                                    subject = "rpd_kategorie.id";
                                    leftJoins.add(
                                        "( SELECT rpd_kategorie.id, sub_geom.geo_field FROM rpd_flaeche LEFT JOIN rpd_kategorie ON rpd_flaeche.fk_kategorie = rpd_kategorie.id LEFT JOIN geom sub_geom ON sub_geom.id = rpd_flaeche.fk_geom ) rpd_kategorie ON rpd_kategorie.geo_field && geom.geo_field AND st_intersects(rpd_kategorie.geo_field, geom.geo_field)");
                                }
                                break;
                            }
                            if (value instanceof Collection) {
                                final List<String> subWheres = new ArrayList<>();
                                for (final MetaObjectNode mon : (Collection<MetaObjectNode>)value) {
                                    subWheres.add(String.format("%s = %d", subject, mon.getObjectId()));
                                }
                                wheresMain.add(String.format("(%s)", String.join(" OR ", subWheres)));
                            } else if (value instanceof MetaObjectNode) {
                                wheresMain.add(String.format(
                                        "%s = %d",
                                        subject,
                                        ((MetaObjectNode)value).getObjectId()));
                            }
                        }
                    }
                }
            }
        }

        final String where;
        switch (searchMode) {
            case AND: {
                where = (!wheresMain.isEmpty()) ? String.join(" AND ", wheresMain) : "TRUE";
                break;
            }
            case OR: {
                where = (!wheresMain.isEmpty()) ? String.join(" OR ", wheresMain) : "FALSE";
                break;
            }
            default: {
                where = "TRUE";
                break;
            }
        }

        final String geomCondition = getGeomCondition();
        if (geomCondition != null) {
            wheresMain.add(geomCondition);
        }

        final String select = String.format(
                "DISTINCT %s AS class_id, %s AS object_id, %s AS object_name",
                "(SELECT id FROM cs_class WHERE table_name ILIKE 'pf_potenzialflaeche')",
                "pf_potenzialflaeche.id",
                "pf_potenzialflaeche.bezeichnung");
        final String from = String.format(
                "pf_potenzialflaeche %s",
                (!leftJoins.isEmpty()) ? String.format("LEFT JOIN %s", String.join(" LEFT JOIN ", leftJoins)) : "");
        final String whereWithGeomCondition = String.format(
                "(%s) AND %s",
                where,
                (geomCondition != null) ? geomCondition : "TRUE");
        final String query = String.format("SELECT %s FROM %s WHERE %s",
                select,
                from,
                whereWithGeomCondition);
        return query;
    }

    @Override
    public void setConfiguration(final Object searchConfiguration) {
        this.configuration = (searchConfiguration instanceof Configuration) ? (Configuration)searchConfiguration : null;
    }

    @Override
    public void setConfiguration(final Configuration searchConfiguration) {
        this.configuration = searchConfiguration;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class Configuration extends StorableSearch.Configuration {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private SearchMode searchMode = SearchMode.AND;
        @JsonProperty private Collection<FilterInfo> filters = new ArrayList<>();

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @param  property  DOCUMENT ME!
         * @param  value     DOCUMENT ME!
         */
        public void addFilter(final PotenzialflaecheReportServerAction.Property property, final Object value) {
            filters.add(new FilterInfo(property, value));
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class FilterInfo extends StorableSearch.Configuration {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private final Object value;
        @JsonProperty private final PotenzialflaecheReportServerAction.Property property;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new FilterInfo object.
         *
         * @param  property  DOCUMENT ME!
         * @param  value     DOCUMENT ME!
         */
        public FilterInfo(final PotenzialflaecheReportServerAction.Property property, final Object value) {
            this.property = property;
            this.value = value;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private class MySearchParameterInfo extends SearchParameterInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new MySearchParameterInfo object.
         *
         * @param  key   DOCUMENT ME!
         * @param  type  DOCUMENT ME!
         */
        private MySearchParameterInfo(final String key, final Type type) {
            this(key, type, null);
        }
        /**
         * Creates a new MySearchParameterInfo object.
         *
         * @param  key    DOCUMENT ME!
         * @param  type   DOCUMENT ME!
         * @param  array  DOCUMENT ME!
         */
        private MySearchParameterInfo(final String key, final Type type, final Boolean array) {
            super.setKey(key);
            super.setType(type);
            if (array != null) {
                super.setArray(array);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class FilterInfoSerializer extends StdSerializer<FilterInfo> {

        //~ Instance fields ----------------------------------------------------

        private final ObjectMapper defaultMapper = new ObjectMapper();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new FilterInfoSerializer object.
         */
        public FilterInfoSerializer() {
            super(FilterInfo.class);

            defaultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void serialize(final FilterInfo filterInfo, final JsonGenerator jg, final SerializerProvider sp)
                throws IOException, JsonGenerationException {
            final PotenzialflaecheReportServerAction.Property property = filterInfo.getProperty();
            final Object value = filterInfo.getValue();
            jg.writeStartObject();
            jg.writeStringField("property", (property != null) ? property.name() : null);
            if (value instanceof Collection) {
                jg.writeArrayFieldStart("value");
                for (final Object subValue : (Collection)value) {
                    if (subValue instanceof MetaObjectNode) {
                        final MetaObjectNode mon = (MetaObjectNode)subValue;
                        jg.writeStartObject();
                        jg.writeNumberField("classId", mon.getClassId());
                        jg.writeNumberField("objectId", mon.getObjectId());
                        jg.writeStringField("domain", mon.getDomain());
                        jg.writeEndObject();
                    }
                }
                jg.writeEndArray();
            } else if (value instanceof MetaObjectNode) {
                final MetaObjectNode mon = (MetaObjectNode)value;
                jg.writeObjectFieldStart("value");
                jg.writeNumberField("classId", mon.getClassId());
                jg.writeNumberField("objectId", mon.getObjectId());
                jg.writeStringField("domain", mon.getDomain());
                jg.writeEndObject();
            }
            jg.writeEndObject();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class FilterInfoDeserializer extends StdDeserializer<FilterInfo> {

        //~ Instance fields ----------------------------------------------------

        private final ObjectMapper defaultMapper = new ObjectMapper();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new CidsBeanJsonDeserializer object.
         */
        public FilterInfoDeserializer() {
            super(FilterInfo.class);

            defaultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public FilterInfo deserialize(final JsonParser jp, final DeserializationContext dc) throws IOException,
            JsonProcessingException {
            final ObjectNode on = jp.readValueAsTree();
            final String propertyName = (String)defaultMapper.treeToValue(on.get("property"), String.class);
            final PotenzialflaecheReportServerAction.Property property = PotenzialflaecheReportServerAction.Property
                        .valueOf(propertyName);

            final Object value;
            if (property != null) {
                final PotenzialflaecheReportServerAction.ReportProperty repProp = property.getValue();
                if ((repProp instanceof PotenzialflaecheReportServerAction.KeytableReportProperty)
                            || (repProp instanceof PotenzialflaecheReportServerAction.MonSearchReportProperty)) {
                    final JsonNode node = on.get("value");
                    try {
                        if (node instanceof ArrayNode) {
                            final ArrayNode arrayNode = (ArrayNode)node;
                            final List<MetaObjectNode> mons = new ArrayList<>();
                            final Iterator<JsonNode> iterator = arrayNode.elements();
                            while (iterator.hasNext()) {
                                final JsonNode subNode = iterator.next();
//                                final JsonNode monNode = subNode.get("value").get(propertyName)
                                final JsonNode valueNode = subNode;
                                final MetaObjectNode mon = new MetaObjectNode(valueNode.get("domain").asText(),
                                        valueNode.get("objectId").asInt(),
                                        valueNode.get("classId").asInt());
                                mons.add(mon);
                            }
                            value = mons;
                        } else {
                            final JsonNode valueNode = node;
                            final MetaObjectNode mon = new MetaObjectNode(valueNode.get("domain").asText(),
                                    valueNode.get("objectId").asInt(),
                                    valueNode.get("classId").asInt());
                            value = mon;
                        }
                    } catch (final Exception ex) {
                        throw new IOException(ex.getMessage(), ex);
                    }
                } else if (repProp instanceof PotenzialflaecheReportServerAction.SimpleFieldReportProperty) {
                    try {
                        value = defaultMapper.treeToValue(on.get("value"),
                                Class.forName(
                                    ((PotenzialflaecheReportServerAction.SimpleFieldReportProperty)repProp)
                                                .getClassName()));
                    } catch (final Exception ex) {
                        throw new IOException(ex.getMessage(), ex);
                    }
                } else {
                    value = null;
                }
            } else {
                value = null;
            }
            final FilterInfo filterInfo = new FilterInfo(property, value);
            return filterInfo;
        }
    }
}
