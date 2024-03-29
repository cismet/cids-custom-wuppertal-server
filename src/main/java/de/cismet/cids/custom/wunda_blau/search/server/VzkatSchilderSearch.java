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
import java.util.Date;
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
@Setter
@Getter
public class VzkatSchilderSearch extends AbstractCidsServerSearch implements RestApiCidsServerSearch,
    MetaObjectNodeServerSearch,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(VzkatSchilderSearch.class);
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

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum SearchFor {

        //~ Enum constants -----------------------------------------------------

        SCHILD, STANDORT
    }

    //~ Instance fields --------------------------------------------------------

    private SearchMode searchMode;
    private SearchFor searchFor = SearchFor.SCHILD;
    private Integer standortId = null;
    private Integer zeichenId = null;
    private Date activeDate = null;
    private Geometry geom = null;
    private final SearchInfo searchInfo;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PotenzialflaecheSearch object.
     */
    public VzkatSchilderSearch() {
        this.searchInfo = new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Builtin Legacy Search to delegate the operation VzkatSchilderSearch to the cids Pure REST Search API.",
                Arrays.asList(
                    new SearchParameterInfo[] { new MySearchParameterInfo("standortId", Type.INTEGER) }),
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
            final List<String> wheres = new ArrayList<>();
            final List<String> leftJoins = new ArrayList<>();

            leftJoins.add("vzkat_richtung ON vzkat_richtung.id = vzkat_schild.fk_richtung");
            leftJoins.add("vzkat_standort ON vzkat_standort.id = vzkat_schild.fk_standort");

            if (standortId != null) {
                wheres.add("vzkat_schild.fk_standort = " + standortId + "");
            }

            if (zeichenId != null) {
                wheres.add("vzkat_zeichen.id = " + zeichenId + "");
                leftJoins.add("vzkat_zeichen ON vzkat_zeichen.id = vzkat_schild.fk_zeichen");
            }

            if (geom != null) {
                final String geomString = PostGisGeometryFactory.getPostGisCompliantDbString(geom);
                wheres.add("(geom.geo_field && st_GeometryFromText('" + geomString + "') AND st_intersects("
                            + "st_buffer(geo_field, " + INTERSECTS_BUFFER + "),"
                            + "st_GeometryFromText('"
                            + geomString
                            + "')))");
                leftJoins.add("geom ON vzkat_standort.fk_geom = geom.id");
            }

            if (activeDate == null) {
                activeDate = new Date();
            }

            wheres.add(String.format(
                    "  "
                            + "(vzkat_schild.gueltig_von IS NULL OR '%1$s'::date >= gueltig_von::date ) AND "
                            + "(vzkat_schild.gueltig_bis IS NULL OR '%1$s'::date <= gueltig_bis::date )",
                    activeDate.toString()));

            final String where = (!wheres.isEmpty()) ? String.format(" WHERE (%s)", String.join(") AND (", wheres))
                                                     : ")";
            final String leftJoin = (!leftJoins.isEmpty())
                ? (String.format(" LEFT JOIN %s", String.join(" LEFT JOIN ", leftJoins))) : "";

            final String fields = SearchFor.STANDORT.equals(searchFor)
                ? "(SELECT id FROM cs_class WHERE table_name ILIKE 'vzkat_standort') AS class_id, vzkat_schild.fk_standort AS object_id, vzkat_standort.import_id::text AS object_name"
                : "(SELECT id FROM cs_class WHERE table_name ILIKE 'vzkat_schild') AS class_id, vzkat_schild.id AS object_id, 'Standort ' || vzkat_standort.import_id::text || ', ' || vzkat_richtung.schluessel || ' ' || vzkat_schild.reihenfolge AS object_name";

            final String query = String.format("SELECT %s FROM vzkat_schild %s %s", fields, leftJoin, where);

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
            LOG.error("error while searching for vzkat_schild", ex);
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
