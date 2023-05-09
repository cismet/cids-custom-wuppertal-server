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

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class PoiSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(PoiSearch.class);

    private static final String TABLE_POI = "poi_locationinstance";
    private static final String TABLE_THEMA = "poi_locationtype";
    private static final String FIELD__ID = "id";
    private static final String FIELD__NAME = "geographicidentifier";
    private static final String FIELD__JOIN_THEMA = "mainlocationtype";
    private static final String FIELD__NUMBER = "\"number\"";
    private static final String FIELD__THEMA = "identification";

    private static final String QUERY_TEMPLATE = "SELECT "
                + "  (SELECT c.id FROM cs_class c WHERE table_name ILIKE '" + TABLE_POI + "') AS class_id, "
                + TABLE_POI + "." + FIELD__ID + ", "
                + TABLE_POI + "." + FIELD__NAME + " || ' -- ' || "
                + "COALESCE(" + TABLE_THEMA + "." + FIELD__THEMA + ", 'xx')" + " as name"
                + " FROM " + TABLE_POI
                + " LEFT JOIN " + TABLE_THEMA + " ON "
                + TABLE_THEMA + "." + FIELD__NUMBER + " = "
                + TABLE_POI + "." + FIELD__JOIN_THEMA
                + " ORDER BY " + TABLE_POI + "." + FIELD__NAME;

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Setter @Getter private Integer poiId;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PoiSearch object.
     */
    public PoiSearch() {
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

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final String query = QUERY_TEMPLATE;
            LOG.info(query);
            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
            final List<MetaObjectNode> mons = new ArrayList<>();
            final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
            for (final ArrayList al : resultList) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
                final String name = String.valueOf(al.get(2));
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null);

                mons.add(mon);
            }
            return mons;
        } catch (final RemoteException ex) {
            LOG.error("error while searching for poi_loctioninstance", ex);
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
