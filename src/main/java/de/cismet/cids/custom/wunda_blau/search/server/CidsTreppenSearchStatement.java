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

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class CidsTreppenSearchStatement extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(CidsTreppenSearchStatement.class);
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
    public enum FilterKey {

        //~ Enum constants -----------------------------------------------------

        ZUSTAND_TREPPENLAEUFE_VON, ZUSTAND_TREPPENLAEUFE_BIS, ZUSTAND_LEITELEMENTE_VON, ZUSTAND_LEITELEMENTE_BIS,
        ZUSTAND_PODESTE_VON, ZUSTAND_PODESTE_BIS, ZUSTAND_HANDLAEUFE_VON, ZUSTAND_HANDLAEUFE_BIS,
        ZUSTAND_ENTWAESSERUNG_VON, ZUSTAND_ENTWAESSERUNG_BIS, ZUSTAND_STUETZMAUERN_VON, ZUSTAND_STUETZMAUERN_BIS,
        NAECHSTE_PRUEFUNG_VON, NAECHSTE_PRUEFUNG_BIS;
    }

    //~ Instance fields --------------------------------------------------------

    private final Boolean andConjuction;
    private final Geometry geom;
    private final HashMap<FilterKey, Object> filter;
    private final StringBuilder fromBuilder = new StringBuilder();
    private final StringBuilder whereBuilder = new StringBuilder();
    private final StringBuilder havingBuilder = new StringBuilder();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsTreppenSearchStatement object.
     *
     * @param  geom         DOCUMENT ME!
     * @param  searchMode   DOCUMENT ME!
     * @param  filterProps  DOCUMENT ME!
     */
    public CidsTreppenSearchStatement(
            final Geometry geom,
            final SearchMode searchMode,
            final HashMap<FilterKey, Object> filterProps) {
        this.geom = geom;
        this.filter = filterProps;
        this.andConjuction = (searchMode == SearchMode.AND_SEARCH);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection<MetaObjectNode> performServerSearch() throws SearchException {
        try {
            final ArrayList result = new ArrayList();
            final MetaService metaService = (MetaService)getActiveLocalServers().get(DOMAIN);
            if (metaService == null) {
                LOG.error("Could not retrieve MetaService '" + DOMAIN + "'.");
                return result;
            }

            if ((geom == null) && ((filter == null) || filter.isEmpty())) {
                LOG.warn("No filters provided. Cancel search.");
                return result;
            }

            final String sqlStatement = generateSqlStatement();

            final ArrayList<ArrayList> resultset;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing SQL statement '" + sqlStatement + "'.");
            }
            resultset = metaService.performCustomSearch(sqlStatement);

            for (final ArrayList treppe : resultset) {
                final int classID = (Integer)treppe.get(0);
                final int objectID = (Integer)treppe.get(1);
                final String name = (String)treppe.get(2);

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
     * @return  DOCUMENT ME!
     */
    private String generateSqlStatement() {
        fromBuilder.append("treppe");
        whereBuilder.append("TRUE");
        havingBuilder.append(andConjuction ? " TRUE " : " FALSE ");

        if (geom != null) {
            final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geom);

            fromBuilder.append(" LEFT OUTER JOIN geom ON treppe.geometrie = geom.id");

            whereBuilder.append(" AND geom.geo_field && GeometryFromText('").append(geostring).append("') AND");

            if ((geom instanceof Polygon) || (geom instanceof MultiPolygon)) { // with buffer for geostring
                whereBuilder.append(" intersects("
                            + "st_buffer(geo_field, " + INTERSECTS_BUFFER + "),"
                            + "st_buffer(GeometryFromText('"
                            + geostring
                            + "'), " + INTERSECTS_BUFFER + "))");
            } else {                                                           // without buffer for
                // geostring
                whereBuilder.append(" and intersects("
                            + "st_buffer(geo_field, " + INTERSECTS_BUFFER + "),"
                            + "GeometryFromText('"
                            + geostring
                            + "'))");
            }
        }

        if (filter.containsKey(FilterKey.NAECHSTE_PRUEFUNG_VON)
                    || filter.containsKey(FilterKey.NAECHSTE_PRUEFUNG_BIS)) {
            final Date vonValue = (Date)filter.get(FilterKey.NAECHSTE_PRUEFUNG_VON);
            final Date bisValue = (Date)filter.get(FilterKey.NAECHSTE_PRUEFUNG_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                havingBuilder.append(andConjuction ? " AND " : " OR ")
                        .append(" (")
                        .append((vonValue != null)
                                    ? ("max(treppe.datum_naechste_pruefung) >= '" + df.format(vonValue) + "'") : "TRUE")
                        .append(" AND ")
                        .append((bisValue != null)
                                    ? ("max(treppe.datum_naechste_pruefung)  <= '" + df.format(bisValue) + "'")
                                    : "TRUE")
                        .append(" )");
            }
        }
        if (filter.containsKey(FilterKey.ZUSTAND_TREPPENLAEUFE_VON)
                    || filter.containsKey(FilterKey.ZUSTAND_TREPPENLAEUFE_BIS)) {
            final Double vonValue = (Double)filter.get(FilterKey.ZUSTAND_TREPPENLAEUFE_VON);
            final Double bisValue = (Double)filter.get(FilterKey.ZUSTAND_TREPPENLAEUFE_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                fromBuilder.append(" LEFT JOIN treppe_treppenlauf ON treppe_treppenlauf.treppe = treppe.id")
                        .append(
                            " LEFT JOIN treppe_zustand AS zustand_treppenlauf ON treppe_treppenlauf.zustand = zustand_treppenlauf.id");
                havingBuilder.append(andConjuction ? " AND " : " OR ")
                        .append(" (")
                        .append((vonValue != null) ? ("max(zustand_treppenlauf.gesamt) >= " + vonValue) : "TRUE")
                        .append(" AND ")
                        .append((bisValue != null) ? ("max(zustand_treppenlauf.gesamt) <= " + bisValue) : "TRUE")
                        .append(" )");
            }
        }
        if (filter.containsKey(FilterKey.ZUSTAND_PODESTE_VON) || filter.containsKey(FilterKey.ZUSTAND_PODESTE_BIS)) {
            final Double vonValue = (Double)filter.get(FilterKey.ZUSTAND_PODESTE_VON);
            final Double bisValue = (Double)filter.get(FilterKey.ZUSTAND_PODESTE_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                fromBuilder.append(" LEFT JOIN treppe_podest ON treppe_podest.treppe = treppe.id")
                        .append(
                            " LEFT JOIN treppe_zustand AS zustand_podest ON treppe_podest.zustand = zustand_podest.id");
                havingBuilder.append(andConjuction ? " AND " : " OR ")
                        .append(" (")
                        .append((vonValue != null) ? ("max(zustand_podest.gesamt) >= " + vonValue) : "TRUE")
                        .append(" AND ")
                        .append((bisValue != null) ? ("max(zustand_podest.gesamt) <= " + bisValue) : "TRUE")
                        .append(" )");
            }
        }
        if (filter.containsKey(FilterKey.ZUSTAND_LEITELEMENTE_VON)
                    || filter.containsKey(FilterKey.ZUSTAND_LEITELEMENTE_BIS)) {
            final Double vonValue = (Double)filter.get(FilterKey.ZUSTAND_LEITELEMENTE_VON);
            final Double bisValue = (Double)filter.get(FilterKey.ZUSTAND_LEITELEMENTE_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                fromBuilder.append(" LEFT JOIN treppe_absturzsicherung ON treppe_absturzsicherung.treppe = treppe.id")
                        .append(
                            " LEFT JOIN treppe_zustand AS zustand_leitelement ON treppe_absturzsicherung.zustand = zustand_leitelement.id");
                havingBuilder.append(andConjuction ? " AND " : " OR ")
                        .append(" (")
                        .append((vonValue != null) ? ("max(zustand_leitelement.gesamt) >= " + vonValue) : "TRUE")
                        .append(" AND ")
                        .append((bisValue != null) ? ("max(zustand_leitelement.gesamt) <= " + bisValue) : "TRUE")
                        .append(" )");
            }
        }
        if (filter.containsKey(FilterKey.ZUSTAND_HANDLAEUFE_VON)
                    || filter.containsKey(FilterKey.ZUSTAND_HANDLAEUFE_BIS)) {
            final Double vonValue = (Double)filter.get(FilterKey.ZUSTAND_HANDLAEUFE_VON);
            final Double bisValue = (Double)filter.get(FilterKey.ZUSTAND_HANDLAEUFE_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                fromBuilder.append(" LEFT JOIN treppe_handlauf ON treppe_handlauf.treppe = treppe.id")
                        .append(
                            " LEFT JOIN treppe_zustand AS zustand_handlauf ON treppe_handlauf.zustand = zustand_handlauf.id");
                havingBuilder.append(andConjuction ? " AND " : " OR ")
                        .append(" (")
                        .append((vonValue != null) ? ("max(zustand_handlauf.gesamt) >= " + vonValue) : "TRUE")
                        .append(" AND ")
                        .append((bisValue != null) ? ("max(zustand_handlauf.gesamt) <= " + bisValue) : "TRUE")
                        .append(" )");
            }
        }
        if (filter.containsKey(FilterKey.ZUSTAND_ENTWAESSERUNG_VON)
                    || filter.containsKey(FilterKey.ZUSTAND_ENTWAESSERUNG_BIS)) {
            final Double vonValue = (Double)filter.get(FilterKey.ZUSTAND_ENTWAESSERUNG_VON);
            final Double bisValue = (Double)filter.get(FilterKey.ZUSTAND_ENTWAESSERUNG_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                fromBuilder.append(" LEFT JOIN treppe_entwaesserung ON treppe.entwaesserung = treppe_entwaesserung.id")
                        .append(
                            " LEFT JOIN treppe_zustand AS zustand_entwaesserung ON treppe_entwaesserung.zustand = zustand_entwaesserung.id");
                havingBuilder.append(andConjuction ? " AND " : " OR ")
                        .append(" (")
                        .append((vonValue != null) ? ("max(zustand_entwaesserung.gesamt) >= " + vonValue) : "TRUE")
                        .append(" AND ")
                        .append((bisValue != null) ? ("max(zustand_entwaesserung.gesamt) <= " + bisValue) : "TRUE")
                        .append(" )");
            }
        }
        if (filter.containsKey(FilterKey.ZUSTAND_STUETZMAUERN_VON)
                    || filter.containsKey(FilterKey.ZUSTAND_STUETZMAUERN_BIS)) {
            final Double vonValue = (Double)filter.get(FilterKey.ZUSTAND_STUETZMAUERN_VON);
            final Double bisValue = (Double)filter.get(FilterKey.ZUSTAND_STUETZMAUERN_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                fromBuilder.append(" LEFT JOIN treppe_stuetzmauer ON treppe_stuetzmauer.treppe = treppe.id")
                        .append(" LEFT JOIN mauer ON treppe_stuetzmauer.mauer = mauer.id");
                havingBuilder.append(andConjuction ? " AND " : " OR ")
                        .append(" (")
                        .append((vonValue != null) ? ("max(mauer.zustand_gesamt) >= " + vonValue) : "TRUE")
                        .append(" AND ")
                        .append((bisValue != null) ? ("max(mauer.zustand_gesamt) <= " + bisValue) : "TRUE")
                        .append(" )");
            }
        }

        return "SELECT <selectClause> FROM <fromClause> WHERE <whereClause> GROUP BY treppe.id HAVING <havingClause>;"
                    .replace(
                            "<selectClause>",
                            "(SELECT c.id FROM cs_class c WHERE table_name ilike 'treppe') AS class_id, treppe.id, max(treppe.name) AS name")
                    .replace("<fromClause>", fromBuilder.toString())
                    .replace("<whereClause>", whereBuilder.toString())
                    .replace("<havingClause>", havingBuilder.toString());
    }
}
