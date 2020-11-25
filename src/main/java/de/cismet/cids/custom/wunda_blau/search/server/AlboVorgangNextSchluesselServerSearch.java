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
public class AlboVorgangNextSchluesselServerSearch extends AbstractCidsServerSearch implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final String QUERY =
        "SELECT max(schluessel::integer) FROM albo_vorgang WHERE schluessel ~ '^[0-9]*$';";

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

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
                final ArrayList<ArrayList> lists = ms.performCustomSearch(QUERY, getConnectionContext());
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
