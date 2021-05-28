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

    @Getter @Setter private String nameProperty = DEFAULT_NAME_PROPERTY;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BplanSearch object.
     */
    public BplaeneMonSearch() {
        this(null, DEFAULT_NAME_PROPERTY);
    }

    /**
     * Creates a new BplaeneMonSearch object.
     *
     * @param  geometry  DOCUMENT ME!
     */
    public BplaeneMonSearch(final Geometry geometry) {
        this(geometry, DEFAULT_NAME_PROPERTY);
    }

    /**
     * Creates a new BplaeneMonSearch object.
     *
     * @param  nameProperty  nameFormat DOCUMENT ME!
     */
    public BplaeneMonSearch(final String nameProperty) {
        this(null, nameProperty);
    }

    /**
     * Creates a new BplanSearch object.
     *
     * @param  geometry      DOCUMENT ME!
     * @param  nameProperty  nameFormat DOCUMENT ME!
     */
    public BplaeneMonSearch(final Geometry geometry, final String nameProperty) {
        setGeometry(geometry);
        this.nameProperty = nameProperty;
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

            final String query = ""
                        + "SELECT DISTINCT \n"
                        + "  (SELECT id FROM cs_class WHERE table_name ILIKE 'bplan_verfahren') AS cid, \n"
                        + "  bplan_verfahren.id AS oid, \n"
                        + "  " + String.format("%s AS name", getNameProperty()) + " \n"
                        + "FROM bplan_verfahren \n"
                        + ((geomCondition != null) ? "LEFT JOIN geom ON geom.id = bplan_verfahren.geometrie " : " ")
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
            LOG.error("error while searching for Bplan object", ex);
            throw new RuntimeException(ex);
        }
    }
}
