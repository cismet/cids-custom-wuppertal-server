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

import com.vividsolutions.jts.geom.Geometry;

import org.apache.log4j.Logger;

import java.io.StringReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

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

    private SearchMode searchMode = null;
    private String nummer = null;
    private String kampagne = null;
    private String bezeichnung = null;
    private Geometry geom = null;
    private final SearchInfo searchInfo;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsAlkisSearchStatement object.
     *
     * @param  searchMode   DOCUMENT ME!
     * @param  nummer       DOCUMENT ME!
     * @param  kampagne     DOCUMENT ME!
     * @param  bezeichnung  DOCUMENT ME!
     * @param  geom         DOCUMENT ME!
     */
    public PotenzialflaecheSearch(final SearchMode searchMode,
            final String nummer,
            final String kampagne,
            final String bezeichnung,
            final Geometry geom) {
        this();
        this.searchMode = searchMode;
        this.nummer = nummer;
        this.kampagne = kampagne;
        this.bezeichnung = bezeichnung;
        this.geom = geom;
    }

    /**
     * Creates a new PotenzialflaecheSearch object.
     */
    private PotenzialflaecheSearch() {
        this.searchInfo = new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Builtin Legacy Search to delegate the operation PotenzialflaecheSearchStatement to the cids Pure REST Search API.",
                Arrays.asList(
                    new SearchParameterInfo[] {
                        new MySearchParameterInfo("searchMode", Type.STRING),
                        new MySearchParameterInfo("nummer", Type.STRING),
                        new MySearchParameterInfo("kampagne", Type.STRING),
                        new MySearchParameterInfo("bezeichnung", Type.STRING),
                    }),
                new MySearchParameterInfo("return", Type.ENTITY_REFERENCE, true));
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public SearchInfo getSearchInfo() {
        return searchInfo;
    }

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

            final Collection<String> wheres = new ArrayList<>();
            switch (searchMode) {
                case AND: {
                    wheres.add("TRUE");
                    break;
                }
                case OR: {
                    wheres.add("FALSE");
                    break;
                }
                default:
            }
            if (nummer != null) {
                wheres.add("pf_potenzialflaeche.nummer ILIKE '%" + nummer + "%'");
            }
            if (bezeichnung != null) {
                wheres.add("pf_potenzialflaeche.bezeichnung ILIKE '%" + bezeichnung + "%'");
            }
            if (kampagne != null) {
                wheres.add("pf_kampagne.bezeichnung ILIKE '%" + kampagne + "%'");
            }

            final String geomCondition;
            if (geom != null) {
                final String geomString = PostGisGeometryFactory.getPostGisCompliantDbString(geom);
                geomCondition = "(geom.geo_field && GeometryFromText('" + geomString + "') AND intersects("
                            + "st_buffer(geo_field, " + INTERSECTS_BUFFER + "),"
                            + "GeometryFromText('"
                            + geomString
                            + "')))";
            } else {
                geomCondition = null;
            }
            final String where;
            switch (searchMode) {
                case AND: {
                    if (geomCondition != null) {
                        wheres.add(geomCondition);
                    }
                    where = "WHERE " + String.join(" AND ", wheres);
                    break;
                }
                case OR: {
                    where = "WHERE " + String.join(" OR ", wheres)
                                + ((geomCondition != null) ? (" AND " + geomCondition) : "");
                    break;
                }
                default: {
                    where = ((geomCondition != null) ? ("WHERE " + geomCondition) : "");
                    break;
                }
            }
            final String query = "SELECT \n"
                        + "	(SELECT id FROM cs_class WHERE table_name ILIKE 'pf_potenzialflaeche') AS class_id, "
                        + "	pf_potenzialflaeche.id AS object_id, "
                        + "	pf_potenzialflaeche.bezeichnung AS object_name "
                        + "FROM pf_potenzialflaeche "
                        + "LEFT JOIN pf_kampagne ON pf_potenzialflaeche.kampagne = pf_kampagne.id "
                        + ((geomCondition != null) ? "LEFT JOIN geom ON pf_potenzialflaeche.geometrie = geom.id " : "")
                        + where;

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
    public ConnectionContext getConnectionContext() {
        return connectionContext;
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
