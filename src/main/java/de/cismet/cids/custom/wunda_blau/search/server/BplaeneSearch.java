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

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class BplaeneSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(BplaeneSearch.class);

    private static final String QUERY = "SELECT "
                + "(SELECT c.id FROM cs_class c WHERE table_name ILIKE 'bplan_verfahren') AS class_id, bplan_verfahren.id, bplan_verfahren.nummer || ' ' || bplan_verfahren.name AS name "
                + "FROM bplan_verfahren "
                + "LEFT JOIN geom ON (geometrie = geom.id) "
                + "WHERE st_intersects(geo_field, st_setSrid('%s', 25832))";

    //~ Instance fields --------------------------------------------------------

    @Setter @Getter private Geometry geom;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BplaeneSearch object.
     */
    public BplaeneSearch() {
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
            if (getGeom() == null) {
                return null;
            }
            final List<MetaObjectNode> mons = new ArrayList<>();
            final String query = String.format(QUERY, getGeom().toText());
            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

            final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
            for (final ArrayList al : resultList) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
                final String name = (String)al.get(2);
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null);

                mons.add(mon);
            }
            return mons;
        } catch (final Exception ex) {
            LOG.error("error while searching for bplan_verfahren", ex);
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
