/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class PfPotenzialflaecheNextSchluesselServerSearch extends AbstractCidsServerSearch
        implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final String QUERY_TEMPLATE = "SELECT sub.middle, count(*)::integer AS count "
                + "FROM ( "
                + "    SELECT substring(nummer, '[0-9]-([0-9]{4})-[0-9]+')::integer AS middle "
                + "    FROM pf_potenzialflaeche "
                + ") AS sub "
                + "WHERE %s "
                + "GROUP BY sub.middle "
                + "ORDER BY sub.middle DESC;";

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    private final Integer middle;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PfPotenzialflaecheNextSchluesselServerSearch object.
     *
     * @param  middle  DOCUMENT ME!
     */
    public PfPotenzialflaecheNextSchluesselServerSearch(final Integer middle) {
        this.middle = middle;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
        if (ms != null) {
            try {
                final String whereMiddle = (middle != null) ? String.format("sub.middle = %d", middle)
                                                            : "sub.middle IS NOT NULL";
                final String query = String.format(QUERY_TEMPLATE, whereMiddle);
                final ArrayList<ArrayList> lists = ms.performCustomSearch(query, getConnectionContext());
                return lists.iterator().next();
            } catch (final Exception ex) {
            }
        }
        return null;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
