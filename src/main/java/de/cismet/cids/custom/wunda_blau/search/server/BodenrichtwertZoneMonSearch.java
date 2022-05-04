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

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.cismet.cidsx.base.types.Type;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class BodenrichtwertZoneMonSearch extends RestApiMonGeometrySearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(BodenrichtwertZoneMonSearch.class);

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new WohnlagenKategorisierungSearch object.
     */
    public BodenrichtwertZoneMonSearch() {
        this(null, null);
    }

    public BodenrichtwertZoneMonSearch(final Double buffer) {
        this(null, buffer);
    }

    /**
     * Creates a new WohnlagenKategorisierungSearch object.
     *
     * @param  geometry  DOCUMENT ME!
     */
    public BodenrichtwertZoneMonSearch(final Geometry geometry, final Double buffer) {
        setGeometry(geometry);
        setBuffer(buffer);
        setSearchInfo(new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Builtin Legacy Search to delegate the operation Wohnlagenkategorisierung to the cids Pure REST Search API.",
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

            final Geometry geometry = getGeometry();
            final String geomCondition = getGeomCondition();
            final String area;
            if (geometry != null) {
                area = String.format(
                        "st_area(st_intersection(geom.geo_field, st_GeometryFromText('%1$s')))",
                        PostGisGeometryFactory.getPostGisCompliantDbString(geometry));
            } else {
                area = "st_area(geom.geo_field)";
            }
            final String query = ""
                        + "SELECT "
                        + "  (SELECT id FROM cs_class WHERE table_name ILIKE 'brw_zone') AS class_id, "
                        + "  object_id, "
                        + "  min(object_name), "
                        + "  sum(area) "
                        + "FROM ( "
                        + "  SELECT "
                        + "    " + area + " AS area, "
                        + "    (SELECT id FROM cs_class WHERE table_name ILIKE 'brw_zone') AS class_id, "
                        + "    brw_zone.id AS object_id, "
                        + "    CASE WHEN brw_zone.bodenrichtwert IS NOT NULL AND brw_zone.bodenrichtwert NOT LIKE '' THEN brw_zone.bodenrichtwert || '€/m² ' ELSE '' END || '(' || brw_zone.entwicklungszustand || CASE WHEN brw_zone.geschosszahl IS NOT NULL THEN ', ' || brw_zone.geschosszahl ELSE '' END || ')' AS object_name "
                        + "  FROM brw_zone \n"
                        + "  " + ((geomCondition != null) ? "LEFT JOIN geom ON geom.id = brw_zone.fk_geom " : " ")
                        + "  " + ((geomCondition != null) ? ("WHERE " + geomCondition) : " ")
                        + ") AS sub "
                        + "GROUP BY object_id "
                        + "ORDER BY sum(area) DESC;";

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
            LOG.error("error while searching for brw_zone object", ex);
            throw new RuntimeException(ex);
        }
    }
}
