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
import Sirius.server.middleware.types.MetaObjectNode;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.vividsolutions.jts.geom.Geometry;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.io.StringReader;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.wunda_blau.search.actions.PotenzialflaecheReportServerAction;

import de.cismet.cids.server.actions.GetServerResourceServerAction;
import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.cidsx.base.types.Type;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class PotenzialflaecheSearch extends AbstractCidsServerSearch implements RestApiCidsServerSearch,
    MetaObjectNodeServerSearch,
    StorableSearch<PotenzialflaecheSearch.Configuration>,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(PotenzialflaecheSearch.class);
    private static final String INTERSECTS_BUFFER = SearchProperties.getInstance().getIntersectsBuffer();

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

    @Getter @Setter private SearchMode searchMode = SearchMode.AND;
    @Getter private Configuration configuration;
    @Getter @Setter private Geometry geom = null;
    @Getter private final SearchInfo searchInfo;
    @Getter private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PotenzialflaecheSearch object.
     */
    public PotenzialflaecheSearch() {
        this.searchInfo = new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Builtin Legacy Search to delegate the operation PotenzialflaecheSearchStatement to the cids Pure REST Search API.",
                Arrays.asList(
                    new SearchParameterInfo[] {
                        new MySearchParameterInfo("searchMode", Type.UNDEFINED),
                        new MySearchParameterInfo("filters", Type.UNDEFINED),
                    }),
                new MySearchParameterInfo("return", Type.ENTITY_REFERENCE, true));
    }

    /**
     * Creates a new PotenzialflaecheSearch object.
     *
     * @param  searchMode           DOCUMENT ME!
     * @param  searchConfiguration  DOCUMENT ME!
     * @param  geom                 DOCUMENT ME!
     */
    public PotenzialflaecheSearch(final SearchMode searchMode,
            final Configuration searchConfiguration,
            final Geometry geom) {
        this();
        this.searchMode = searchMode;
        this.configuration = searchConfiguration;
        this.geom = geom;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final List<MetaObjectNode> result = new ArrayList<>();
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
                    final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null);

                    result.add(mon);
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

        switch (searchMode) {
            case AND: {
                wheresMain.add("TRUE");
                break;
            }
            case OR: {
                wheresMain.add("FALSE");
                break;
            }
            default:
        }

        if (getConfiguration().getFilters() != null) {
            for (final FilterInfo filterInfo : getConfiguration().getFilters()) {
                if (filterInfo != null) {
                    final Object value = filterInfo.getValue();
                    final PotenzialflaecheReportServerAction.Property property = filterInfo.getProperty();
                    if ((property != null)
                                && (property.getValue()
                                    instanceof PotenzialflaecheReportServerAction.PathReportProperty)) {
                        final PotenzialflaecheReportServerAction.PathReportProperty pathProp =
                            (PotenzialflaecheReportServerAction.PathReportProperty)property.getValue();
                        final String path = String.format("pf_potenzialflaeche.%s", pathProp.getPath());
                        if (value != null) {
                            if (property.getValue()
                                        instanceof PotenzialflaecheReportServerAction.SimpleFieldReportProperty) {
                                final String className =
                                    ((PotenzialflaecheReportServerAction.SimpleFieldReportProperty)property.getValue())
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
                            } else if (property.getValue()
                                        instanceof PotenzialflaecheReportServerAction.KeytableReportProperty) {
                                final String filterPath =
                                    ((PotenzialflaecheReportServerAction.KeytableReportProperty)property.getValue())
                                            .getFilterPath();
                                if (value instanceof Collection) {
                                    final List<String> subWheres = new ArrayList<>();
                                    for (final MetaObjectNode mon : (Collection<MetaObjectNode>)value) {
                                        subWheres.add(String.format("%s = %d", filterPath, mon.getObjectId()));
                                    }
                                    wheresMain.add(String.format("(%s)", String.join(" OR ", subWheres)));
                                } else if (value instanceof MetaObjectNode) {
                                    wheresMain.add(String.format(
                                            "%s = %d",
                                            filterPath,
                                            ((MetaObjectNode)value).getObjectId()));
                                }
                            }
                        } else {
                            wheresMain.add(String.format("%s IS NULL", path, value));
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

        final String geomCondition;
        if (geom != null) {
            final String geomString = PostGisGeometryFactory.getPostGisCompliantDbString(geom);
            geomCondition = String.format("(geom.geo_field && GeometryFromText('%1$s') AND intersects("
                            + "st_buffer(geo_field, %2$s),"
                            + "GeometryFromText('%1$s')))",
                    geomString,
                    INTERSECTS_BUFFER);
            leftJoins.add("geom ON pf_potenzialflaeche.geometrie = geom.id");
            wheresMain.add(geomCondition);
        } else {
            geomCondition = null;
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
                (geom != null) ? geomCondition : "TRUE");
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
        @JsonProperty private final Collection<FilterInfo> filters = new ArrayList<>();

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
}
