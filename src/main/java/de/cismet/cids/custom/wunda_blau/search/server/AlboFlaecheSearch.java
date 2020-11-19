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
import java.util.Collection;
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class AlboFlaecheSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(AlboFlaecheSearch.class);

    private static final String QUERY_TEMPLATE = "SELECT "
                + "(SELECT c.id FROM cs_class c WHERE table_name ILIKE 'albo_flaeche') AS class_id, flaeche.id, 'Fläche: ' || flaeche.erhebungsnummer || ' [' || art.schluessel || ']' AS name "
                + "FROM albo_flaeche AS flaeche "
                + "LEFT JOIN albo_flaechenart AS art ON flaeche.fk_art = art.id "
                + "%s "
                + "%s "
                + "ORDER BY flaeche.erhebungsnummer";

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

    @Setter @Getter private Integer vorgangId;
    @Setter @Getter private String vorgangSchluessel;
    @Setter @Getter private Integer artId;
    @Setter @Getter private String erhebungsNummer;
    @Setter @Getter private Geometry geometry;
    @Getter private final SearchMode searchMode;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlboFlaecheSearch object.
     *
     * @param  searchMode  DOCUMENT ME!
     */
    public AlboFlaecheSearch(final SearchMode searchMode) {
        this.searchMode = searchMode;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param  connectionContext  DOCUMENT ME!
     */
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final String buffer = SearchProperties.getInstance().getIntersectsBuffer();
            final List<String> leftJoins = new ArrayList<>();
            final Collection<String> wheres = new ArrayList<>();

            leftJoins.add("albo_vorgang_flaeche AS arr ON flaeche.id = arr.fk_flaeche");

            if (getErhebungsNummer() != null) {
                wheres.add(String.format("flaeche.erhebungsnummer ILIKE '%%%s%%'", getErhebungsNummer()));
            }
            if (getArtId() != null) {
                wheres.add(String.format("flaeche.fk_art = %d", getArtId()));
            }

            if (getVorgangId() != null) {
                wheres.add(String.format("arr.vorgang_reference = %d", getVorgangId()));
            }

            if (getVorgangSchluessel() != null) {
                leftJoins.add("albo_vorgang AS vorgang ON vorgang.arr_flaechen = arr.vorgang_reference");
                wheres.add(String.format("vorgang.schluessel LIKE '%%%s%%'", getVorgangSchluessel()));
            }

            final String geomWhere;
            if (getGeometry() != null) {
                final String geomString = PostGisGeometryFactory.getPostGisCompliantDbString(getGeometry());
                leftJoins.add("geom ON flaeche.fk_geom = geom.id");
                geomWhere = "(geom.geo_field && GeometryFromText('" + geomString + "') AND intersects("
                            + "st_buffer(geo_field, " + buffer + "),"
                            + "GeometryFromText('"
                            + geomString
                            + "')))";
            } else {
                geomWhere = null;
            }
            final String where;
            switch (searchMode) {
                case AND: {
                    if (geomWhere != null) {
                        wheres.add(geomWhere);
                    }
                    where = "WHERE " + String.join(" AND ", wheres);
                    break;
                }
                case OR: {
                    where = "WHERE (" + String.join(" OR ", wheres) + ")"
                                + ((geomWhere != null) ? (" AND " + geomWhere) : "");
                    break;
                }
                default: {
                    where = ((geomWhere != null) ? ("WHERE " + geomWhere) : "");
                    break;
                }
            }
            final String leftJoin = (!leftJoins.isEmpty()) ? (" LEFT JOIN " + String.join(" LEFT JOIN ", leftJoins))
                                                           : "";
            final String query = String.format(QUERY_TEMPLATE, leftJoin, where.isEmpty() ? "" : where);

            final List<MetaObjectNode> mons = new ArrayList<>();
            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

            final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
            for (final ArrayList al : resultList) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
                final String name = String.valueOf(al.get(2));
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null);

                mons.add(mon);
            }
            return mons;
        } catch (final Exception ex) {
            LOG.error("error while searching for albo_flaeche", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
