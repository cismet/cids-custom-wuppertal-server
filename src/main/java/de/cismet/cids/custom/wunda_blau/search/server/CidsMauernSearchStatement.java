/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObjectNode;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.cismet.cids.server.search.SearchException;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class CidsMauernSearchStatement extends RestApiMonGeometrySearch
        implements StorableSearch<CidsMauernSearchStatement.Configuration> {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(CidsMauernSearchStatement.class);
    private static final String SQL_STMT =
        "SELECT DISTINCT (SELECT c.id FROM cs_class c WHERE table_name ilike 'mauer') as class_id, m.id,m.lagebezeichnung as name FROM %s WHERE %s";
    private static final String JOIN_GEOM = "geom AS g ON m.georeferenz = g.id";
    private static final String JOIN_LASTKLASSE = "mauer_lastklasse AS l ON l.id = m.lastklasse";
    private static final String JOIN_EIGENTUEMER = "mauer_eigentuemer AS e ON e.id = m.eigentuemer";
    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String INTERSECTS_BUFFER = SearchProperties.getInstance().getIntersectsBuffer();

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum SearchMode {

        //~ Enum constants -----------------------------------------------------

        AND, OR;
    }

    //~ Instance fields --------------------------------------------------------

    @Getter private Configuration configuration;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsMauernSearchStatement object.
     *
     * @param  configuration  eigentuemerIds DOCUMENT ME!
     * @param  geom           DOCUMENT ME!
     */
    public CidsMauernSearchStatement(final Configuration configuration, final Geometry geom) {
        this.configuration = configuration;
        setGeometry(geom);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createQuery() {
        final Set<String> joins = new LinkedHashSet<>();
        final Set<String> wheres = new LinkedHashSet<>();
        final Geometry geom = getGeometry();

        final Configuration configuration = getConfiguration();

        final SearchMode searchMode = (configuration != null) ? configuration.getSearchMode() : null;
        final List<Integer> eigentuemer = (configuration != null) ? configuration.getEigentuemer() : null;
        final List<Integer> lastKlasseIds = (configuration != null) ? configuration.getLastKlasseIds() : null;
        final MassnahmenInfo massnahmen = (configuration != null) ? configuration.getMassnahmen() : null;
        final ZustaendeInfo zustaende = (configuration != null) ? configuration.getZustaende() : null;
        final Integer sanierung = (configuration != null) ? configuration.getSanierung() : null;

        joins.add("mauer AS m");
        if ((geom != null)) {
            joins.add(JOIN_GEOM);
            final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geom);

            final List<String> conditions = new ArrayList<>();
            conditions.add(String.format("g.geo_field && st_GeometryFromText('%s')", geostring));
            if ((geom instanceof Polygon) || (geom instanceof MultiPolygon)) { // with buffer for geostring
                conditions.add(String.format(
                        " st_intersects("
                                + "st_buffer(geo_field, "
                                + INTERSECTS_BUFFER
                                + "),"
                                + "st_buffer(st_GeometryFromText('%s'), "
                                + INTERSECTS_BUFFER
                                + "))",
                        geostring));
            } else {                                                           // without buffer for
                // geostring
                conditions.add(String.format(
                        " and st_intersects("
                                + "st_buffer(geo_field, "
                                + INTERSECTS_BUFFER
                                + "),"
                                + "st_GeometryFromText('%s'))",
                        geostring));
            }
            wheres.add(String.join(" AND ", conditions));
        }

        if ((eigentuemer != null) && !eigentuemer.isEmpty()) {
            joins.add(JOIN_EIGENTUEMER);
            wheres.add(String.format(
                    " m.eigentuemer in (%s)",
                    String.join(
                        ", ",
                        (List)eigentuemer.stream().map(new Function<Integer, String>() {

                                @Override
                                public String apply(final Integer value) {
                                    return (value != null) ? Integer.toString(value) : null;
                                }
                            }).collect(Collectors.toList()))));
        }

        if ((lastKlasseIds != null) && !lastKlasseIds.isEmpty()) {
            joins.add(JOIN_LASTKLASSE);
            wheres.add(String.format(
                    "m.lastklasse in (%s)",
                    String.join(
                        ", ",
                        (List)lastKlasseIds.stream().map(new Function<Integer, String>() {

                                @Override
                                public String apply(final Integer value) {
                                    return (value != null) ? Integer.toString(value) : null;
                                }
                            }).collect(Collectors.toList()))));
        }

        if (massnahmen != null) {
            if (isNotAllNull(massnahmen.getPruefung())) {
                final MassnahmeInfo massnahme = massnahmen.getPruefung();
                final Date von = massnahme.getVon();
                final Date bis = massnahme.getBis();
                wheres.add(createWhereFor("m.datum_naechste_pruefung", massnahme.getVon(), massnahme.getBis()));
            }

            if (isNotAllNull(massnahmen.getSanierungDurchgefuehrt())) {
                final MassnahmeInfo massnahme = massnahmen.getSanierungDurchgefuehrt();
                final Date von = massnahme.getVon();
                final Date bis = massnahme.getBis();
                final Integer gewerk = massnahme.getGewerk();
                final Boolean erledigt = massnahme.getErledigt();

                joins.add("mauer_massnahme AS mm1 ON m.id = mm1.fk_mauer");
                joins.add("mauer_massnahme_art AS mma1 ON mm1.fk_art = mma1.id");
                wheres.add("mma1.schluessel LIKE 'durchgefuehrte_sanierung'");
                wheres.add(createWhereFor("mm1.ziel", von, bis));
                wheres.add((gewerk != null) ? String.format("mm1.fk_objekt = %d", gewerk) : null);
                wheres.add((erledigt != null) ? (erledigt ? "mm1.erledigt IS TRUE" : "mm1.erledigt IS NOT TRUE")
                                              : null);
            }
            if (isNotAllNull(massnahmen.getSanierungGeplant())) {
                final MassnahmeInfo massnahme = massnahmen.getSanierungGeplant();
                final Date von = massnahme.getVon();
                final Date bis = massnahme.getBis();
                final Integer gewerk = massnahme.getGewerk();
                final Boolean erledigt = massnahme.getErledigt();

                joins.add("mauer_massnahme AS mm2 ON m.id = mm2.fk_mauer");
                joins.add("mauer_massnahme_art AS mma2 ON mm2.fk_art = mma2.id");
                wheres.add("mma2.schluessel LIKE 'durchzufuehrende_sanierung'");
                wheres.add(createWhereFor("mm2.ziel", von, bis));
                wheres.add((gewerk != null) ? String.format("(mm2.fk_objekt = %d)", gewerk) : null);
                wheres.add((erledigt != null) ? (erledigt ? "mm2.erledigt IS TRUE" : "mm2.erledigt IS NOT TRUE")
                                              : null);
            }
            if (isNotAllNull(massnahmen.getBauwerksbesichtigung())) {
                final MassnahmeInfo massnahme = massnahmen.getBauwerksbesichtigung();
                final Date von = massnahme.getVon();
                final Date bis = massnahme.getBis();
                final Boolean erledigt = massnahme.getErledigt();

                joins.add("mauer_massnahme AS mm3 ON m.id = mm3.fk_mauer");
                joins.add("mauer_massnahme_art AS mma3 ON mm3.fk_art = mma3.id");
                wheres.add("mma3.schluessel LIKE 'bauwerksbesichtigung'");
                wheres.add(createWhereFor("mm3.ziel", von, bis));
                wheres.add((erledigt != null) ? (erledigt ? "mm3.erledigt IS TRUE" : "mm3.erledigt IS NOT TRUE")
                                              : null);
            }
            if (isNotAllNull(massnahmen.getBauwerksbegehung())) {
                final MassnahmeInfo massnahme = massnahmen.getBauwerksbegehung();
                final Date von = massnahme.getVon();
                final Date bis = massnahme.getBis();
                final Boolean erledigt = massnahme.getErledigt();
                joins.add("mauer_massnahme AS mm4 ON m.id = mm4.fk_mauer");
                joins.add("mauer_massnahme_art AS mma4 ON mm4.fk_art = mma4.id");
                wheres.add("mma4.schluessel LIKE 'bauwerksbegehung'");
                wheres.add(createWhereFor("mm4.ziel", von, bis));
                wheres.add((erledigt != null) ? (erledigt ? "mm4.erledigt IS TRUE" : "mm4.erledigt IS NOT TRUE")
                                              : null);
            }
        }

        wheres.add(createWhereFor("m.hoehe_max", configuration.getHoeheVon(), configuration.getHoeheBis()));
        if (zustaende != null) {
            if (isNotAllNull(zustaende.getGelaende())) {
                joins.add("mauer_zustand AS z_gelaende ON m.fk_zustand_gelaende = z_gelaende.id");
                wheres.add(createWhereFor(
                        "z_gelaende.gesamt",
                        zustaende.getGelaende().getVon(),
                        zustaende.getGelaende().getBis()));
            }
            if (isNotAllNull(zustaende.getAnsicht())) {
                joins.add("mauer_zustand AS z_ansicht ON m.fk_zustand_ansicht = z_ansicht.id");
                wheres.add(createWhereFor(
                        "z_ansicht.gesamt",
                        zustaende.getAnsicht().getVon(),
                        zustaende.getAnsicht().getBis()));
            }
            if (isNotAllNull(zustaende.getGelaender())) {
                joins.add("mauer_zustand AS z_gelaender ON m.fk_zustand_gelaender = z_gelaender.id");
                wheres.add(createWhereFor(
                        "z_gelaender.gesamt",
                        zustaende.getGelaender().getVon(),
                        zustaende.getGelaender().getBis()));
            }
            if (isNotAllNull(zustaende.getWandkopf())) {
                joins.add("mauer_zustand AS z_kopf ON m.fk_zustand_kopf = z_kopf.id");
                wheres.add(createWhereFor(
                        "z_kopf.gesamt",
                        zustaende.getWandkopf().getVon(),
                        zustaende.getWandkopf().getBis()));
            }
            if (isNotAllNull(zustaende.getGruendung())) {
                joins.add("mauer_zustand AS z_gruendung ON m.fk_zustand_gruendung = z_gruendung.id");
                wheres.add(createWhereFor(
                        "z_gruendung.gesamt",
                        zustaende.getGruendung().getVon(),
                        zustaende.getGruendung().getBis()));
            }
            if (isNotAllNull(zustaende.getGelaendeOben())) {
                joins.add("mauer_zustand AS z_gelaende_oben ON m.fk_zustand_gelaende_oben = z_gelaende_oben.id");
                wheres.add(createWhereFor(
                        "z_gelaende_oben.gesamt",
                        zustaende.getGelaendeOben().getVon(),
                        zustaende.getGelaendeOben().getBis()));
            }
            if (isNotAllNull(zustaende.getBausubstanz())) {
                wheres.add(createWhereFor(
                        "(m.zustand_gesamt)",
                        zustaende.getBausubstanz().getVon(),
                        zustaende.getBausubstanz().getBis()));
            }
        }

        if (sanierung != null) {
            wheres.add(String.format("m.sanierung = %d", sanierung));
        }

        wheres.remove(null);
        final String sql = (String.format(
                    SQL_STMT,
                    String.join(" LEFT OUTER JOIN ", joins),
                    wheres.isEmpty() ? "TRUE" : String.join((searchMode == SearchMode.AND) ? " AND " : " OR ", wheres)));
        return sql;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   zustand  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isNotAllNull(final ZustandInfo zustand) {
        return (zustand != null) && ((zustand.getVon() != null) || (zustand.getBis() != null));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   massnahme  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isNotAllNull(final MassnahmeInfo massnahme) {
        return (massnahme != null)
                    && ((massnahme.getVon() != null) || (massnahme.getBis() != null)
                        || (massnahme.getGewerk() != null)
                        || (massnahme.getErledigt() != null));
    }

    @Override
    public void setConfiguration(final Object searchConfiguration) {
        this.configuration = (searchConfiguration instanceof Configuration) ? (Configuration)searchConfiguration : null;
    }

    @Override
    public void setConfiguration(final Configuration searchConfiguration) {
        this.configuration = searchConfiguration;
    }

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Collection<MetaObjectNode> performServerSearch() throws SearchException {
        try {
            final ArrayList result = new ArrayList();

            final MetaService metaService = (MetaService)getActiveLocalServers().get(DOMAIN);
            if (metaService == null) {
                LOG.error("Could not retrieve MetaService '" + DOMAIN + "'.");
                return result;
            }

            final String query = createQuery();
            if (query == null) {
                return result;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Executing SQL statement '%s'.", query));
            }
            final ArrayList<ArrayList> resultset = metaService.performCustomSearch(query, getConnectionContext());

            for (final ArrayList mauer : resultset) {
                final int classID = (Integer)mauer.get(0);
                final int objectID = (Integer)mauer.get(1);
                final String name = (String)mauer.get(2);

                final MetaObjectNode node = new MetaObjectNode(DOMAIN, objectID, classID, name, null, null); // TODO: Check4CashedGeomAndLightweightJson

                result.add(node);
            }
            return result;
        } catch (final Exception e) {
            LOG.error("Problem", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   property  DOCUMENT ME!
     * @param   von       DOCUMENT ME!
     * @param   bis       DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String createWhereFor(final String property, final Date von, final Date bis) {
        final List<String> conditions = new ArrayList<>();
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        if (von != null) {
            conditions.add(String.format("%s >= '%s'", property, df.format(von)));
        }
        if (bis != null) {
            conditions.add(String.format("%s <= '%s'", property, df.format(bis)));
        }
        if (!conditions.isEmpty()) {
            return String.format("(%s)", String.join(" AND ", conditions));
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   property  DOCUMENT ME!
     * @param   von       DOCUMENT ME!
     * @param   bis       DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String createWhereFor(final String property,
            final Double von,
            final Double bis) {
        final List<String> conditions = new ArrayList<>();
        if (von != null) {
            conditions.add(String.format(Locale.US, "%s >= %f", property, von));
        }
        if (bis != null) {
            conditions.add(String.format(Locale.US, "%s <= %f", property, bis));
        }
        if (!conditions.isEmpty()) {
            return String.format(Locale.US, "(%s)", String.join(" AND ", conditions));
        } else {
            return null;
        }
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
    @Setter
    @NoArgsConstructor
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class Configuration extends StorableSearch.Configuration {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private SearchMode searchMode = SearchMode.AND;
        @JsonProperty private List<Integer> eigentuemer = new ArrayList<>();
        @JsonProperty private List<Integer> lastKlasseIds = new ArrayList<>();

        @JsonProperty private ZustaendeInfo zustaende = new ZustaendeInfo();
        @JsonProperty private MassnahmenInfo massnahmen = new MassnahmenInfo();

        @JsonProperty private Integer sanierung;
        @JsonProperty private Double hoeheVon;
        @JsonProperty private Double hoeheBis;

        /*
         * @JsonProperty private HashMap<Property, Object> filters; public enum Property {
         *
         * //~ Enum constants -----------------------------------------------------
         *
         * SANIERUNG, }
         */

    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class ZustaendeInfo extends StorableSearch.Configuration {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private ZustandInfo gelaender;
        @JsonProperty private ZustandInfo ansicht;
        @JsonProperty private ZustandInfo wandkopf;
        @JsonProperty private ZustandInfo gruendung;
        @JsonProperty private ZustandInfo gelaendeOben;
        @JsonProperty private ZustandInfo gelaende;
        @JsonProperty private ZustandInfo bausubstanz;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class ZustandInfo extends StorableSearch.Configuration {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private Double von;
        @JsonProperty private Double bis;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new ZustandInfo object.
         *
         * @param  von  DOCUMENT ME!
         * @param  bis  DOCUMENT ME!
         */
        public ZustandInfo(final Double von, final Double bis) {
            this.von = von;
            this.bis = bis;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class MassnahmenInfo extends StorableSearch.Configuration {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private MassnahmeInfo pruefung;
        @JsonProperty private MassnahmeInfo sanierungDurchgefuehrt;
        @JsonProperty private MassnahmeInfo sanierungGeplant;
        @JsonProperty private MassnahmeInfo bauwerksbegehung;
        @JsonProperty private MassnahmeInfo bauwerksbesichtigung;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class MassnahmeInfo extends StorableSearch.Configuration {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private Date von;
        @JsonProperty private Date bis;
        @JsonProperty private Integer gewerk;
        @JsonProperty private Boolean erledigt;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new MassnahmeInfo object.
         *
         * @param  von  DOCUMENT ME!
         * @param  bis  DOCUMENT ME!
         */
        public MassnahmeInfo(final Date von, final Date bis) {
            this.von = von;
            this.bis = bis;
        }

        /**
         * Creates a new MassnahmeInfo object.
         *
         * @param  von       DOCUMENT ME!
         * @param  bis       DOCUMENT ME!
         * @param  gewerk    DOCUMENT ME!
         * @param  erledigt  DOCUMENT ME!
         */
        public MassnahmeInfo(final Date von, final Date bis, final Integer gewerk, final Boolean erledigt) {
            this.von = von;
            this.bis = bis;
            this.gewerk = gewerk;
            this.erledigt = erledigt;
        }
    }
}
