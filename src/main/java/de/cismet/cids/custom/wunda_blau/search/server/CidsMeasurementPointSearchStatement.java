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
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ServerConnectionContext;
import de.cismet.connectioncontext.ServerConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jweintraut
 * @version  $Revision$, $Date$
 */
public class CidsMeasurementPointSearchStatement extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch,
    ServerConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    /** LOGGER. */
    private static final transient Logger LOG = Logger.getLogger(CidsMeasurementPointSearchStatement.class);

    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String CIDSCLASS_ALKIS = "alkis_point";
    private static final String CIDSCLASS_NIVELLEMENT = "nivellement_punkt";

    private static final String SQL_ALKIS = "SELECT DISTINCT (SELECT c.id FROM cs_class c WHERE table_name ilike '"
                + CIDSCLASS_ALKIS
                + "') as class_id, ap.id, ap.pointcode as name FROM <fromClause> <whereClause>";
    private static final String SQL_ORDERBY_ALKIS = " ORDER BY ap.pointcode DESC";
    private static final String SQL_NIVELLEMENT =
        "SELECT DISTINCT (SELECT c.id FROM cs_class c WHERE table_name ilike '"
                + CIDSCLASS_NIVELLEMENT
                + "') as class_id, np.id, np.dgk_blattnummer || lpad(np.laufende_nummer, 3, '0') FROM <fromClause> <whereClause>";
    private static final String SQL_ORDERBY_NIVELLEMENT =
        " ORDER BY np.dgk_blattnummer || lpad(np.laufende_nummer, 3, '0') DESC";
    private static final String SQL_ORDERBY_BOTH = " ORDER BY name DESC";
    private static final String INTERSECTS_BUFFER = SearchProperties.getInstance().getIntersectsBuffer();

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Pointtype {

        //~ Enum constants -----------------------------------------------------

        AUFNAHMEPUNKTE(4), SONSTIGE_VERMESSUNGSPUNKTE(5), GRENZPUNKTE(1), BESONDERE_GEBAEUDEPUNKTE(2),
        BESONDERE_BAUWERKSPUNKTE(3), BESONDERE_TOPOGRAPHISCHE_PUNKTE(6), NIVELLEMENT_PUNKTE(-1);

        //~ Instance fields ----------------------------------------------------

        private int id;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Pointtype object.
         *
         * @param  id  DOCUMENT ME!
         */
        Pointtype(final int id) {
            this.id = id;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public int getId() {
            return id;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum GST {

        //~ Enum constants -----------------------------------------------------

        LE2(new int[] { 1000, 1100, 1200, 2000 }), LE3(new int[] { 1000, 1100, 1200, 2000, 2100 }),
        LE6(new int[] { 1000, 1100, 1200, 2000, 2100, 2200 }),
        LE10(new int[] { 1000, 1100, 1200, 2000, 2100, 2200, 2300 });

        //~ Instance fields ----------------------------------------------------

        private int[] condition;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new GST object.
         *
         * @param  condition  DOCUMENT ME!
         */
        GST(final int[] condition) {
            this.condition = condition;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public int[] getCondition() {
            return condition;
        }
    }

    //~ Instance fields --------------------------------------------------------

    private final String pointcode;
    private Collection<Pointtype> pointtypes;
    private GST gst;
    private Geometry geometry;

    private ServerConnectionContext connectionContext = ServerConnectionContext.create(getClass().getSimpleName());

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CustomAlkisPointSearchStatement object.
     *
     * @param  pointcode   DOCUMENT ME!
     * @param  pointtypes  DOCUMENT ME!
     * @param  gst         DOCUMENT ME!
     * @param  geometry    DOCUMENT ME!
     */
    public CidsMeasurementPointSearchStatement(final String pointcode,
            final Collection<Pointtype> pointtypes,
            final GST gst,
            final Geometry geometry) {
        this.pointcode = StringEscapeUtils.escapeSql(pointcode);
        this.pointtypes = pointtypes;
        this.gst = gst;
        this.geometry = geometry;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            LOG.info("Starting search for points. Pointcode: '" + pointcode + "', pointtypes: '" + pointtypes
                        + "', GST: '" + gst + "', geometry: '" + geometry + "'.");

            final ArrayList result = new ArrayList();

            if ((pointtypes == null) || pointtypes.isEmpty()) {
                LOG.warn("There is no pointtype specified. Cancel search..");
                return result;
            }

            final MetaService metaService = (MetaService)getActiveLocalServers().get(DOMAIN);
            if (metaService == null) {
                LOG.error("Could not retrieve MetaService '" + DOMAIN + "'.");
                return result;
            }

            final StringBuilder sqlBuilder = new StringBuilder();
            String sqlOrderBy = "";

            if (pointtypes.contains(Pointtype.AUFNAHMEPUNKTE)
                        || pointtypes.contains(Pointtype.BESONDERE_BAUWERKSPUNKTE)
                        || pointtypes.contains(Pointtype.BESONDERE_GEBAEUDEPUNKTE)
                        || pointtypes.contains(Pointtype.BESONDERE_TOPOGRAPHISCHE_PUNKTE)
                        || pointtypes.contains(Pointtype.GRENZPUNKTE)
                        || pointtypes.contains(Pointtype.SONSTIGE_VERMESSUNGSPUNKTE)) {
                final String sqlAlkis = SQL_ALKIS.replace("<fromClause>", generateFromClauseForAlkis())
                            .replace("<whereClause>", generateWhereClauseForAlkis());
                sqlBuilder.append(sqlAlkis);

                sqlOrderBy = SQL_ORDERBY_ALKIS;
            }

            if (pointtypes.contains(Pointtype.NIVELLEMENT_PUNKTE)) {
                if (sqlOrderBy.length() == 0) {
                    sqlOrderBy = SQL_ORDERBY_NIVELLEMENT;
                } else {
                    sqlBuilder.append(" UNION ");
                    sqlOrderBy = SQL_ORDERBY_BOTH;
                }

                final String sqlNivellement = SQL_NIVELLEMENT.replace(
                            "<fromClause>",
                            generateFromClauseForNivellement())
                            .replace("<whereClause>", generateWhereClauseForNivellement());
                sqlBuilder.append(sqlNivellement);
            }

            if (sqlBuilder.length() > 0) {
                sqlBuilder.append(sqlOrderBy);
            }

            final ArrayList<ArrayList> resultset;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing SQL statement '" + sqlBuilder.toString() + "'.");
            }
            resultset = metaService.performCustomSearch(sqlBuilder.toString(), getConnectionContext());

            for (final ArrayList measurementPoint : resultset) {
                final int classID = (Integer)measurementPoint.get(0);
                final int objectID = (Integer)measurementPoint.get(1);
                final String name = (String)measurementPoint.get(2);

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
    protected String generateFromClauseForAlkis() {
        String fromClause = null;

        if (geometry != null) {
            fromClause = CIDSCLASS_ALKIS.concat(" ap, geom g");
        } else {
            fromClause = CIDSCLASS_ALKIS.concat(" ap");
        }

        return fromClause;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected String generateWhereClauseForAlkis() {
        final StringBuilder whereClauseBuilder = new StringBuilder();
        String conjunction = "WHERE ";
        if ((pointcode != null) && (pointcode.trim().length() > 0)) {
            whereClauseBuilder.append(conjunction);

            whereClauseBuilder.append("ap.pointcode like '");
            whereClauseBuilder.append(pointcode);
            whereClauseBuilder.append('\'');

            conjunction = " AND ";
        }
        if ((pointtypes != null) && !pointtypes.isEmpty()) {
            whereClauseBuilder.append(conjunction);
            whereClauseBuilder.append('(');

            final Iterator<Pointtype> pointtypesIter = pointtypes.iterator();
            while (pointtypesIter.hasNext()) {
                final Pointtype pointtype = pointtypesIter.next();

                whereClauseBuilder.append("ap.pointtype=");
                whereClauseBuilder.append(pointtype.getId());

                if (pointtypesIter.hasNext()) {
                    whereClauseBuilder.append(" OR ");
                }
            }
            whereClauseBuilder.append(')');

            conjunction = " AND ";
        }
        if (gst != null) {
            whereClauseBuilder.append(conjunction);
            whereClauseBuilder.append('(');

            final int[] condition = gst.getCondition();
            for (int i = 0; i < condition.length; i++) {
                whereClauseBuilder.append("ap.gst=");
                whereClauseBuilder.append(condition[i]);

                if (i < (condition.length - 1)) {
                    whereClauseBuilder.append(" OR ");
                }
            }
            whereClauseBuilder.append(')');

            conjunction = " AND ";
        }
        if (geometry != null) {
            final String geomString = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);

            whereClauseBuilder.append(conjunction);
            conjunction = " AND ";

            whereClauseBuilder.append("ap.geom = g.id");

            whereClauseBuilder.append(conjunction);

            whereClauseBuilder.append("g.geo_field && GeometryFromText('").append(geomString).append("')");

            whereClauseBuilder.append(conjunction);

            if ((geometry instanceof Polygon) || (geometry instanceof MultiPolygon)) { // with buffer for geostring
                whereClauseBuilder.append("intersects(st_buffer(g.geo_field, " + INTERSECTS_BUFFER
                                + "), st_buffer(GeometryFromText('")
                        .append(geomString)
                        .append("'), " + INTERSECTS_BUFFER + "))");
            } else {
                whereClauseBuilder.append("intersects(st_buffer(g.geo_field, " + INTERSECTS_BUFFER
                                + "), GeometryFromText('")
                        .append(geomString)
                        .append("'))");
            }
        }
        return whereClauseBuilder.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected String generateFromClauseForNivellement() {
        String fromClause = null;

        if (geometry != null) {
            fromClause = CIDSCLASS_NIVELLEMENT.concat(" np, geom g");
        } else {
            fromClause = CIDSCLASS_NIVELLEMENT.concat(" np");
        }

        return fromClause;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected String generateWhereClauseForNivellement() {
        final StringBuilder whereClauseBuilder = new StringBuilder();
        String conjunction = "WHERE ";

        whereClauseBuilder.append(conjunction);
        whereClauseBuilder.append("np.historisch = false");
        conjunction = " AND ";

        if ((pointcode != null) && (pointcode.trim().length() > 0)) {
            whereClauseBuilder.append(conjunction);
            whereClauseBuilder.append("np.dgk_blattnummer || lpad(np.laufende_nummer, 3, '0') like '");
            whereClauseBuilder.append(pointcode);
            whereClauseBuilder.append('\'');

            conjunction = " AND ";
        }

        if (geometry != null) {
            final String geomString = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);

            whereClauseBuilder.append(conjunction);
            conjunction = " AND ";

            whereClauseBuilder.append("np.geometrie = g.id");

            whereClauseBuilder.append(conjunction);

            whereClauseBuilder.append("g.geo_field && GeometryFromText('").append(geomString).append("')");

            whereClauseBuilder.append(conjunction);

            if ((geometry instanceof Polygon) || (geometry instanceof MultiPolygon)) { // with buffer for geostring
                whereClauseBuilder.append("intersects(st_buffer(g.geo_field, " + INTERSECTS_BUFFER
                                + "), st_buffer(GeometryFromText('")
                        .append(geomString)
                        .append("'), " + INTERSECTS_BUFFER + "))");
            } else {
                whereClauseBuilder.append("intersects(st_buffer(g.geo_field, " + INTERSECTS_BUFFER
                                + "), GeometryFromText('")
                        .append(geomString)
                        .append("'))");
            }
        }

        return whereClauseBuilder.toString();
    }

    @Override
    public ServerConnectionContext getConnectionContext() {
        return connectionContext;
    }

    @Override
    public void initAfterConnectionContext() {
    }

    @Override
    public void setConnectionContext(final ServerConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }
}
