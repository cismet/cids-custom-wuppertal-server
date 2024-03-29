/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObjectNode;

import com.vividsolutions.jts.geom.Geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.cismet.cidsx.base.types.Type;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class BplaeneMonSearch extends RestApiMonGeometrySearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(BplaeneMonSearch.class);

    private static final String DEFAULT_NAME_PROPERTY = "bplan_verfahren.nummer";

    //~ Instance fields --------------------------------------------------------

    @Getter @Setter private List<String> nameProperties = Arrays.asList(DEFAULT_NAME_PROPERTY);
    @Getter @Setter private SubUnion[] subUnions;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BplaeneMonSearch object.
     *
     * @param  subUnions  DOCUMENT ME!
     */
    public BplaeneMonSearch(final SubUnion... subUnions) {
        this(null, null, subUnions);
    }

    /**
     * Creates a new BplaeneMonSearch object.
     *
     * @param  buffer  DOCUMENT ME!
     */
    public BplaeneMonSearch(final Double buffer) {
        this(null, buffer);
    }

    /**
     * Creates a new BplaeneMonSearch object.
     *
     * @param  geometry   DOCUMENT ME!
     * @param  subUnions  DOCUMENT ME!
     */
    public BplaeneMonSearch(final Geometry geometry, final SubUnion... subUnions) {
        this(geometry, null, subUnions);
    }

    /**
     * Creates a new BplaeneMonSearch object.
     *
     * @param  buffer     DOCUMENT ME!
     * @param  subUnions  DOCUMENT ME!
     */
    public BplaeneMonSearch(final Double buffer, final SubUnion... subUnions) {
        this(null, buffer, subUnions);
    }

    /**
     * Creates a new BplanSearch object.
     *
     * @param  geometry   DOCUMENT ME!
     * @param  buffer     DOCUMENT ME!
     * @param  subUnions  DOCUMENT ME!
     */
    public BplaeneMonSearch(final Geometry geometry, final Double buffer, final SubUnion... subUnions) {
        setGeometry(geometry);
        setBuffer(buffer);
        setSubUnions(subUnions);
        setSearchInfo(new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Builtin Legacy Search to delegate the operation Bplan to the cids Pure REST Search API.",
                Arrays.asList(
                    new SearchParameterInfo[] {
                        new MySearchParameterInfo("searchFor", Type.STRING),
                        new MySearchParameterInfo("geom", Type.UNDEFINED),
                    }),
                new MySearchParameterInfo("return", Type.ENTITY_REFERENCE, true)));
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final List<MetaObjectNode> result = new ArrayList<>();

            final String geomCondition = getGeomCondition();

            final List<String> queries = new ArrayList<>();
            if (getNameProperties() != null) {
                final String query;
                final SubUnion[] subUnions = getSubUnions();
                if ((subUnions != null) && (subUnions.length != 0)) {
                    for (final SubUnion subUnion : subUnions) {
                        if (subUnion != null) {
                            final List<String> wheres = new ArrayList<>();
                            if (geomCondition != null) {
                                wheres.add(geomCondition);
                            }
                            final String whereClause = subUnion.getWhereClause();
                            if (whereClause != null) {
                                wheres.add(whereClause);
                            }
                            queries.add(""
                                        + "SELECT DISTINCT \n"
                                        + "  (SELECT id FROM cs_class WHERE table_name ILIKE 'bplan_verfahren') AS cid, "
                                        + "  bplan_verfahren.id AS oid, "
                                        + "  " + String.format("%s AS name", subUnion.getFieldProperty()) + ", "
                                        + "  "
                                        + String.format(
                                            "%s AS orderField ",
                                            (((subUnion.getOrderField() != null)
                                                    && !subUnion.getOrderField().trim().isEmpty())
                                                ? subUnion.getOrderField().trim() : "0"))
                                        + "FROM bplan_verfahren "
                                        + ((geomCondition != null) ? subUnion.getGeomJoin() : "") + " "
                                        + ((!wheres.isEmpty()) ? ("WHERE " + String.join(" AND ", wheres)) : ""));
                        }
                    }
                    query = String.format(
                            "SELECT * FROM (%s) AS unioned ORDER BY orderField ASC;",
                            String.join(" UNION ", queries));
                } else {
                    query = "SELECT DISTINCT \n"
                                + "  (SELECT id FROM cs_class WHERE table_name ILIKE 'bplan_verfahren') AS cid, "
                                + "  bplan_verfahren.id AS oid, "
                                + "  nummer "
                                + "FROM bplan_verfahren "
                                + ((geomCondition != null) ? "LEFT JOIN geom ON geom.id = bplan_verfahren.geometrie "
                                                           : " ")
                                + ((geomCondition != null) ? ("WHERE " + geomCondition) : " ")
                                + " ORDER BY nummer DESC";
                }
                final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

                final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
                for (final ArrayList al : resultList) {
                    final int cid = (Integer)al.get(0);
                    final int oid = (Integer)al.get(1);
                    final String name = (String)al.get(2);
                    result.add(new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null));
                }
            }
            return result;
        } catch (final Exception ex) {
            LOG.error("error while searching for Bplan object", ex);
            throw new RuntimeException(ex);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @AllArgsConstructor
    public static class SubUnion implements Serializable {

        //~ Instance fields ----------------------------------------------------

        private String fieldProperty;
        private String geomJoin;
        private String whereClause;
        private String orderField;
    }
}
