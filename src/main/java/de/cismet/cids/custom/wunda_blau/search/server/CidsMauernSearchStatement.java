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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class CidsMauernSearchStatement extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch,
    ConnectionContextStore {

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

        AND_SEARCH, OR_SEARCH;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PropertyKeys {

        //~ Enum constants -----------------------------------------------------

        ZUSTAND_HOEHE_VON, ZUSTAND_HOEHE_BIS, ZUSTAND_GELAENDER_VON, ZUSTAND_GELAENDER_BIS, ZUSTAND_ANSICHT_VON,
        ZUSTAND_ANSICHT_BIS, ZUSTAND_WANDKOPF_VON, ZUSTAND_WANDKOPF_BIS, ZUSTAND_GRUENDUNG_VON, ZUSTAND_GRUENDUNG_BIS,
        ZUSTAND_GELAENDE_OBEN_VON, ZUSTAND_GELAENDE_OBEN_BIS, ZUSTAND_GELAENDE_VON, ZUSTAND_GELAENDE_BIS,
        ZUSTAND_BAUSUBSTANZ_VON, ZUSTAND_BAUSUBSTANZ_BIS, SANIERUNG, MASSNAHME_PRUEFUNG_VON, MASSNAHME_PRUEFUNG_BIS,
        MASSNAHME_SANIERUNG_DURCHGEFUEHRT_VON, MASSNAHME_SANIERUNG_DURCHGEFUEHRT_BIS, MASSNAHME_SANIERUNG_GEPLANT_VON,
        MASSNAHME_SANIERUNG_GEPLANT_BIS, MASSNAHME_BAUWERKSBEGEHUNG_VON, MASSNAHME_BAUWERKSBEGEHUNG_BIS,
        MASSNAHME_BAUWERKSBESICHTIGUNG_VON, MASSNAHME_BAUWERKSBESICHTIGUNG_BIS, MASSNAHME_GEWERK_DURCHZU,
        MASSNAHME_GEWERK_DURCHGE, MASSNAHME_ERLEDIGT
    }

    //~ Instance fields --------------------------------------------------------

    private final Geometry geom;
    private final List<Integer> eigentuemer;
    private final List<Integer> lastKlasseIds;
    private final HashMap<PropertyKeys, Object> filter;
    private final Set<String> joins = new LinkedHashSet<>();
    private final Set<String> wheres = new LinkedHashSet<>();
    private final SearchMode searchMode;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsMauernSearchStatement object.
     *
     * @param  eigentuemerIds  DOCUMENT ME!
     * @param  lastKlasseIds   DOCUMENT ME!
     * @param  geom            DOCUMENT ME!
     * @param  searchMode      DOCUMENT ME!
     * @param  filterProps     DOCUMENT ME!
     */
    public CidsMauernSearchStatement(final List<Integer> eigentuemerIds,
            final List<Integer> lastKlasseIds,
            final Geometry geom,
            final SearchMode searchMode,
            final HashMap<PropertyKeys, Object> filterProps) {
        this.geom = geom;
        this.eigentuemer = eigentuemerIds;
        this.lastKlasseIds = lastKlasseIds;
        this.filter = filterProps;
        this.searchMode = searchMode;
    }

    //~ Methods ----------------------------------------------------------------

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

            if ((geom == null) && ((eigentuemer == null) || eigentuemer.isEmpty())
                        && ((lastKlasseIds == null) || lastKlasseIds.isEmpty())
                        && ((filter == null) || filter.isEmpty())) {
                LOG.warn("No filters provided. Cancel search.");
                return result;
            }

            joins.add("mauer AS m");
            if ((geom != null)) {
                joins.add(JOIN_GEOM);
                final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geom);

                final List<String> conditions = new ArrayList<>();
                conditions.add(String.format("g.geo_field && GeometryFromText('%s')", geostring));
                if ((geom instanceof Polygon) || (geom instanceof MultiPolygon)) { // with buffer for geostring
                    conditions.add(String.format(
                            " intersects("
                                    + "st_buffer(geo_field, "
                                    + INTERSECTS_BUFFER
                                    + "),"
                                    + "st_buffer(GeometryFromText('%s'), "
                                    + INTERSECTS_BUFFER
                                    + "))",
                            geostring));
                } else {                                                           // without buffer for
                    // geostring
                    conditions.add(String.format(
                            " and intersects("
                                    + "st_buffer(geo_field, "
                                    + INTERSECTS_BUFFER
                                    + "),"
                                    + "GeometryFromText('%s'))",
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
                            "'",
                            (List)lastKlasseIds.stream().map(new Function<Integer, String>() {

                                    @Override
                                    public String apply(final Integer value) {
                                        return (value != null) ? Integer.toString(value) : null;
                                    }
                                }).collect(Collectors.toList()))));
            }

            if ((filter.get(PropertyKeys.MASSNAHME_PRUEFUNG_VON) != null)
                        || (filter.get(PropertyKeys.MASSNAHME_PRUEFUNG_BIS) != null)) {
                wheres.add(createWhereFor(
                        "m.datum_naechste_pruefung",
                        (Date)filter.get(PropertyKeys.MASSNAHME_PRUEFUNG_VON),
                        (Date)filter.get(PropertyKeys.MASSNAHME_PRUEFUNG_BIS)));
            }

            final Boolean erledigt = (Boolean)filter.get(PropertyKeys.MASSNAHME_ERLEDIGT);

            if ((filter.get(PropertyKeys.MASSNAHME_SANIERUNG_DURCHGEFUEHRT_VON) != null)
                        || (filter.get(PropertyKeys.MASSNAHME_SANIERUNG_DURCHGEFUEHRT_BIS) != null)) {
                joins.add("mauer_massnahme AS mm1 ON m.id = mm1.fk_mauer");
                joins.add("mauer_massnahme_art AS mma1 ON mm1.fk_art = mma1.id");
                wheres.add(String.format(
                        "(%s AND mma1.schluessel LIKE 'durchgefuehrte_sanierung' AND %s)",
                        (erledigt != null) ? (erledigt ? "mm1.erledigt IS TRUE" : "mm1.erledigt IS NOT TRUE") : "TRUE",
                        createWhereFor(
                            "mm1.ziel",
                            (Date)filter.get(PropertyKeys.MASSNAHME_SANIERUNG_DURCHGEFUEHRT_VON),
                            (Date)filter.get(PropertyKeys.MASSNAHME_SANIERUNG_DURCHGEFUEHRT_BIS))));
            }
            if ((filter.get(PropertyKeys.MASSNAHME_SANIERUNG_GEPLANT_VON) != null)
                        || (filter.get(PropertyKeys.MASSNAHME_SANIERUNG_GEPLANT_BIS) != null)) {
                joins.add("mauer_massnahme AS mm2 ON m.id = mm2.fk_mauer");
                joins.add("mauer_massnahme_art AS mma2 ON mm2.fk_art = mma2.id");
                wheres.add(String.format(
                        "(%s AND mma2.schluessel LIKE 'durchzufuehrende_sanierung' AND %s)",
                        (erledigt != null) ? (erledigt ? "mm2.erledigt IS TRUE" : "mm2.erledigt IS NOT TRUE") : "TRUE",
                        createWhereFor(
                            "mm2.ziel",
                            (Date)filter.get(PropertyKeys.MASSNAHME_SANIERUNG_GEPLANT_VON),
                            (Date)filter.get(PropertyKeys.MASSNAHME_SANIERUNG_GEPLANT_BIS))));
            }
            if ((filter.get(PropertyKeys.MASSNAHME_BAUWERKSBESICHTIGUNG_VON) != null)
                        || (filter.get(PropertyKeys.MASSNAHME_BAUWERKSBESICHTIGUNG_BIS) != null)) {
                joins.add("mauer_massnahme AS mm3 ON m.id = mm3.fk_mauer");
                joins.add("mauer_massnahme_art AS mma3 ON mm3.fk_art = mma3.id");
                wheres.add(String.format(
                        "(%s AND mma3.schluessel LIKE 'bauwerksbesichtigung' AND %s)",
                        (erledigt != null) ? (erledigt ? "mm3.erledigt IS TRUE" : "mm3.erledigt IS NOT TRUE") : "TRUE",
                        createWhereFor(
                            "mm3.ziel",
                            (Date)filter.get(PropertyKeys.MASSNAHME_BAUWERKSBESICHTIGUNG_VON),
                            (Date)filter.get(PropertyKeys.MASSNAHME_BAUWERKSBESICHTIGUNG_BIS))));
            }
            if ((filter.get(PropertyKeys.MASSNAHME_BAUWERKSBEGEHUNG_VON) != null)
                        || (filter.get(PropertyKeys.MASSNAHME_BAUWERKSBEGEHUNG_BIS) != null)) {
                joins.add("mauer_massnahme AS mm4 ON m.id = mm4.fk_mauer");
                joins.add("mauer_massnahme_art AS mma4 ON mm4.fk_art = mma4.id");
                wheres.add(String.format(
                        "(%s AND mma4.schluessel LIKE 'bauwerksbegehung' AND %s)",
                        (erledigt != null) ? (erledigt ? "mm4.erledigt IS TRUE" : "mm4.erledigt IS NOT TRUE") : "TRUE",
                        createWhereFor(
                            "mm4.ziel",
                            (Date)filter.get(PropertyKeys.MASSNAHME_BAUWERKSBEGEHUNG_VON),
                            (Date)filter.get(PropertyKeys.MASSNAHME_BAUWERKSBEGEHUNG_BIS))));
            }

            if ((filter.get(PropertyKeys.ZUSTAND_HOEHE_VON) != null)
                        || (filter.get(PropertyKeys.ZUSTAND_HOEHE_BIS) != null)) {
                wheres.add(createWhereFor(
                        "m.hoehe_max",
                        (Double)filter.get(PropertyKeys.ZUSTAND_HOEHE_VON),
                        (Double)filter.get(PropertyKeys.ZUSTAND_HOEHE_BIS)));
            }
            if ((filter.get(PropertyKeys.ZUSTAND_GELAENDE_VON) != null)
                        || (filter.get(PropertyKeys.ZUSTAND_GELAENDE_BIS) != null)) {
                joins.add("mauer_zustand AS z_gelaende ON m.fk_zustand_gelaende = z_gelaende.id");
                wheres.add(createWhereFor(
                        "z_gelaende.gesamt",
                        (Double)filter.get(PropertyKeys.ZUSTAND_GELAENDE_VON),
                        (Double)filter.get(PropertyKeys.ZUSTAND_GELAENDE_BIS)));
            }
            if ((filter.get(PropertyKeys.ZUSTAND_ANSICHT_VON) != null)
                        || (filter.get(PropertyKeys.ZUSTAND_ANSICHT_BIS) != null)) {
                joins.add("mauer_zustand AS z_ansicht ON m.fk_zustand_ansicht = z_ansicht.id");
                wheres.add(createWhereFor(
                        "z_ansicht.gesamt",
                        (Double)filter.get(PropertyKeys.ZUSTAND_ANSICHT_VON),
                        (Double)filter.get(PropertyKeys.ZUSTAND_ANSICHT_BIS)));
            }
            if ((filter.get(PropertyKeys.ZUSTAND_GELAENDER_VON) != null)
                        || (filter.get(PropertyKeys.ZUSTAND_GELAENDER_BIS) != null)) {
                joins.add("mauer_zustand AS z_gelaender ON m.fk_zustand_gelaender = z_gelaender.id");
                wheres.add(createWhereFor(
                        "z_gelaender.gesamt",
                        (Double)filter.get(PropertyKeys.ZUSTAND_GELAENDER_VON),
                        (Double)filter.get(PropertyKeys.ZUSTAND_GELAENDER_BIS)));
            }
            if ((filter.get(PropertyKeys.ZUSTAND_WANDKOPF_VON) != null)
                        || (filter.get(PropertyKeys.ZUSTAND_WANDKOPF_BIS) != null)) {
                joins.add("mauer_zustand AS z_kopf ON m.fk_zustand_kopf = z_kopf.id");
                wheres.add(createWhereFor(
                        "z_kopf.gesamt",
                        (Double)filter.get(PropertyKeys.ZUSTAND_WANDKOPF_VON),
                        (Double)filter.get(PropertyKeys.ZUSTAND_WANDKOPF_BIS)));
            }
            if ((filter.get(PropertyKeys.ZUSTAND_GRUENDUNG_VON) != null)
                        || (filter.get(PropertyKeys.ZUSTAND_GRUENDUNG_BIS) != null)) {
                joins.add("mauer_zustand AS z_gruendung ON m.fk_zustand_gruendung = z_gruendung.id");
                wheres.add(createWhereFor(
                        "z_gruendung.gesamt",
                        (Double)filter.get(PropertyKeys.ZUSTAND_GRUENDUNG_VON),
                        (Double)filter.get(PropertyKeys.ZUSTAND_GRUENDUNG_BIS)));
            }
            if ((filter.get(PropertyKeys.ZUSTAND_GELAENDE_OBEN_VON) != null)
                        || (filter.get(PropertyKeys.ZUSTAND_GELAENDE_OBEN_BIS) != null)) {
                joins.add("mauer_zustand AS z_gelaende_oben ON m.fk_zustand_gelaende_oben = z_gelaende_oben.id");
                wheres.add(createWhereFor(
                        "z_gelaende_oben.gesamt",
                        (Double)filter.get(PropertyKeys.ZUSTAND_GELAENDE_OBEN_VON),
                        (Double)filter.get(PropertyKeys.ZUSTAND_GELAENDE_OBEN_BIS)));
            }
            if ((filter.get(PropertyKeys.ZUSTAND_BAUSUBSTANZ_VON) != null)
                        || (filter.get(PropertyKeys.ZUSTAND_BAUSUBSTANZ_BIS) != null)) {
                wheres.add(createWhereFor(
                        "(m.zustand_gesamt)",
                        (Double)filter.get(PropertyKeys.ZUSTAND_BAUSUBSTANZ_VON),
                        (Double)filter.get(PropertyKeys.ZUSTAND_BAUSUBSTANZ_BIS)));
            }

            if (filter.get(PropertyKeys.SANIERUNG) != null) {
                wheres.add(String.format(
                        "m.sanierung = %d",
                        (Integer)filter.get(PropertyKeys.SANIERUNG)));
            }

            if ((filter.get(PropertyKeys.MASSNAHME_GEWERK_DURCHGE) != null)) {
                joins.add("mauer_massnahme AS mm1 ON m.id = mm1.fk_mauer");
                joins.add("mauer_massnahme_art AS mma1 ON mm1.fk_art = mma1.id");
                wheres.add(String.format(
                        "(%s AND mma1.schluessel LIKE 'durchgefuehrte_sanierung' AND mm1.fk_objekt = %d)",
                        (erledigt != null) ? (erledigt ? "mm1.erledigt IS TRUE" : "mm1.erledigt IS NOT TRUE") : "TRUE",
                        (Integer)filter.get(PropertyKeys.MASSNAHME_GEWERK_DURCHGE)));
            }
            if ((filter.get(PropertyKeys.MASSNAHME_GEWERK_DURCHZU) != null)) {
                joins.add("mauer_massnahme AS mm2 ON m.id = mm2.fk_mauer");
                joins.add("mauer_massnahme_art AS mma2 ON mm2.fk_art = mma2.id");
                wheres.add(String.format(
                        "(%s AND mma2.schluessel LIKE 'durchzufuehrende_sanierung' AND mm2.fk_objekt = %d)",
                        (erledigt != null) ? (erledigt ? "mm2.erledigt IS TRUE" : "mm2.erledigt IS NOT TRUE") : "TRUE",
                        (Integer)filter.get(PropertyKeys.MASSNAHME_GEWERK_DURCHZU)));
            }
            final String sql = (String.format(
                        SQL_STMT,
                        String.join(" LEFT OUTER JOIN ", joins),
                        String.join((searchMode == SearchMode.AND_SEARCH) ? " AND " : " OR ", wheres)));

            final ArrayList<ArrayList> resultset;
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Executing SQL statement '%s'.", sql));
            }
            resultset = metaService.performCustomSearch(sql, getConnectionContext());

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
}
