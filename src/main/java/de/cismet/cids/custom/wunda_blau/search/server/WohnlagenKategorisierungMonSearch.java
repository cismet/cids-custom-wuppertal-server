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

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
public class WohnlagenKategorisierungMonSearch extends AbstractCidsServerSearch implements GeometrySearch,
    RestApiCidsServerSearch,
    MetaObjectNodeServerSearch,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(WohnlagenKategorisierungMonSearch.class);
    private static final String INTERSECTS_BUFFER = SearchProperties.getInstance().getIntersectsBuffer();

    //~ Instance fields --------------------------------------------------------

    @Getter @Setter private Geometry geometry = null;

    @Getter private final SearchInfo searchInfo;
    @Getter private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new WohnlagenKategorisierungSearch object.
     */
    public WohnlagenKategorisierungMonSearch() {
        this(null);
    }

    /**
     * Creates a new WohnlagenKategorisierungSearch object.
     *
     * @param  geometry  DOCUMENT ME!
     */
    public WohnlagenKategorisierungMonSearch(final Geometry geometry) {
        this.geometry = geometry;
        this.searchInfo = new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Builtin Legacy Search to delegate the operation Wohnlagenkategorisierung to the cids Pure REST Search API.",
                Arrays.asList(
                    new SearchParameterInfo[] {
                        new MySearchParameterInfo("searchFor", Type.STRING),
                        new MySearchParameterInfo("geom", Type.UNDEFINED),
                    }),
                new MySearchParameterInfo("return", Type.ENTITY_REFERENCE, true));
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

            final Geometry geometry = getGeometry();
            final String geomCondition;
            if (geometry != null) {
                final String geomString = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);
                geomCondition = "(geom.geo_field && GeometryFromText('" + geomString + "') AND intersects("
                            + "st_buffer(geo_field, " + INTERSECTS_BUFFER + "),"
                            + "GeometryFromText('"
                            + geomString
                            + "')))";
            } else {
                geomCondition = null;
            }
            final String query = ""
                        + "SELECT DISTINCT \n"
                        + "  (SELECT id FROM cs_class WHERE table_name ILIKE 'wohnlage_kategorie') AS class_id, \n"
                        + "  wohnlage_kategorie.id AS object_id, \n"
                        + "  wohnlage_kategorie.name AS object_name \n"
                        + "FROM wohnlage \n"
                        + "LEFT JOIN wohnlage_kategorisierung ON wohnlage_kategorisierung.fk_wohnlage = wohnlage.id \n"
                        + "LEFT JOIN wohnlage_kategorie ON wohnlage_kategorie.id = wohnlage_kategorisierung.fk_kategorie \n"
                        + ((geomCondition != null) ? "LEFT JOIN geom ON geom.id = wohnlage.fk_geometrie " : " ")
                        + ((geomCondition != null) ? ("WHERE " + geomCondition) : " ")
                        + ";";

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
            LOG.error("error while searching for wohnlagenkategorisierung object", ex);
            throw new RuntimeException(ex);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

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