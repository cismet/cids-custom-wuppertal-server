/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.sql.PreparableStatement;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.util.Locale;

import de.cismet.cids.server.search.builtin.DefaultGeoSearch;
import de.cismet.cids.server.search.builtin.GeoSearch;

/**
 * DOCUMENT ME!
 *
 * @author   martin.scholl@cismet.de
 * @version  $Revision$, $Date$
 */
@ServiceProvider(
    service = GeoSearch.class,
    position = 1000
)
public final class BufferingGeosearch extends DefaultGeoSearch {

    //~ Static fields/initializers ---------------------------------------------

    /** LOGGER. */
    private static final transient Logger LOG = Logger.getLogger(BufferingGeosearch.class);

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static enum GeomMode {

        //~ Enum constants -----------------------------------------------------

        INTERSECTS, WITHIN, CONTAINS
    }

    //~ Instance fields --------------------------------------------------------

    private GeomMode geomMode = GeomMode.INTERSECTS;
    private Double buffer;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BufferingGeosearch object.
     */
    public BufferingGeosearch() {
        if (SearchProperties.getInstance().getIntersectsBuffer() != null) {
            try {
                buffer = Double.parseDouble(SearchProperties.getInstance().getIntersectsBuffer());
            } catch (final Exception ex) {
                LOG.error("error while parsing IntersectsBuffer from SearchProperties to double", ex);
            }
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param  buffer  DOCUMENT ME!
     */
    public void setBuffer(final Double buffer) {
        this.buffer = buffer;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  geomMode  DOCUMENT ME!
     */
    public void setGeomMode(final GeomMode geomMode) {
        this.geomMode = geomMode;
    }

    @Override
    public PreparableStatement getSearchSql(final String domainKey) {
        final String sql = ""                                                                                      // NOI18N
                    + "SELECT DISTINCT i.class_id ocid, "                                                          // NOI18N
                    + "                i.object_id oid, "                                                          // NOI18N
                    + "                s.stringrep,s.geometry,s.lightweight_json "                                 // NOI18N
                    + "FROM            geom g, "                                                                   // NOI18N
                    + "                cs_attr_object_derived i "                                                  // NOI18N
                    + "                LEFT OUTER JOIN cs_cache s "                                                // NOI18N
                    + "                ON              ( "                                                         // NOI18N
                    + "                                                s.class_id =i.class_id "                    // NOI18N
                    + "                                AND             s.object_id=i.object_id "                   // NOI18N
                    + "                                ) "                                                         // NOI18N
                    + "WHERE           i.attr_class_id = "                                                         // NOI18N
                    + "                ( SELECT cs_class.id "                                                      // NOI18N
                    + "                FROM    cs_class "                                                          // NOI18N
                    + "                WHERE   cs_class.table_name::text = 'GEOM'::text "                          // NOI18N
                    + "                ) "                                                                         // NOI18N
                    + "AND             i.attr_object_id = g.id "                                                   // NOI18N
                    + "AND i.class_id IN <cidsClassesInStatement> "                                                // NOI18N
                    + "AND geo_field && st_geomfromtext('SRID=<cidsSearchGeometrySRID>;<cidsSearchGeometryWKT>') " // NOI18N
                    + "AND <geomStatement> "                                                                       // NOI18N
                    + "ORDER BY 1,2,3";                                                                            // NOI18N

        final Geometry searchGeometry = getGeometry();
        final String geomFunction;
        switch (geomMode) {
            case CONTAINS: {
                geomFunction = "st_contains";
            }
            break;
            case WITHIN: {
                geomFunction = "st_within";
            }
            break;
            case INTERSECTS:
            default: {
                geomFunction = "st_intersects";
            }
        }
        final String searchGeomFromText = "st_geomfromtext('SRID=<cidsSearchGeometrySRID>;<cidsSearchGeometryWKT>')";
        final String bufferedSearchGeomFromText = (buffer != null)
            ? String.format(Locale.US, "st_buffer(%s, %f)", searchGeomFromText, buffer) : searchGeomFromText;
        final String bufferedTargetGeomText = (buffer != null)
            ? String.format(Locale.US, "st_buffer(geo_field, %f)", buffer) : "geo_field";

        final String geomFunctionString = "%s(%s, %s)";
        final String geomFunctionParamA;
        final String geomFunctionParamB;
        if (searchGeometry.getSRID() == 4326) {
            geomFunctionParamA = "geo_field";
            geomFunctionParamB = searchGeomFromText;
        } else {
            if ((searchGeometry instanceof Polygon) || (searchGeometry instanceof MultiPolygon)) {    // with buffer for searchGeometry
                geomFunctionParamA = bufferedTargetGeomText;
                geomFunctionParamB = bufferedSearchGeomFromText;
            } else {                                                                                  // without buffer for searchGeometry
                geomFunctionParamA = bufferedTargetGeomText;
                geomFunctionParamB = searchGeomFromText;
            }
        }
        final String geomStatement = String.format(
                geomFunctionString,
                geomFunction,
                geomFunctionParamA,
                geomFunctionParamB);

        final String cidsSearchGeometryWKT = searchGeometry.toText();
        final String sridString = Integer.toString(searchGeometry.getSRID());
        final String classesInStatement = getClassesInSnippetsPerDomain().get(domainKey);
        if ((cidsSearchGeometryWKT == null) || (cidsSearchGeometryWKT.trim().length() == 0)
                    || (sridString == null)
                    || (sridString.trim().length() == 0)) {
            // TODO: Notify user?
            LOG.error(
                "Search geometry or srid is not given. Can't perform a search without those information."); // NOI18N

            return null;
        }

        if ((classesInStatement == null) || (classesInStatement.trim().length() == 0)) {
            LOG.warn("There are no search classes defined for domain '" + domainKey // NOI18N
                        + "'. This domain will be skipped."); // NOI18N

            return null;
        }

        final PreparableStatement ps = new PreparableStatement(
                sql.replaceAll("<geomStatement>", geomStatement)              // NOI18N
                .replaceAll("<cidsClassesInStatement>", classesInStatement)   // NOI18N
                .replaceAll("<cidsSearchGeometryWKT>", cidsSearchGeometryWKT) // NOI18N
                .replaceAll("<cidsSearchGeometrySRID>", sridString),
                new int[0]);                                                  // NOI18N

        ps.setObjects(new Object[0]);

        return ps;
    }
}
