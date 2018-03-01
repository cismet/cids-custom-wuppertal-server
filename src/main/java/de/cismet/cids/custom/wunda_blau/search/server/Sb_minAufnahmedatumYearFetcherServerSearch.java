/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.server.connectioncontext.ServerConnectionContext;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;
import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
public class Sb_minAufnahmedatumYearFetcherServerSearch extends AbstractCidsServerSearch
        implements ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final String searchQuery =
        "select min(extract(year from aufnahmedatum)::int) from sb_stadtbildserie";

    //~ Instance fields --------------------------------------------------------

    private ServerConnectionContext serverConnectionContext = ServerConnectionContext.create(getClass()
                    .getSimpleName());

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
        if (ms != null) {
            try {
                final ArrayList<ArrayList> lists = ms.performCustomSearch(searchQuery, getServerConnectionContext());
                return lists;
            } catch (RemoteException ex) {
            }
        }
        return null;
    }

    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return serverConnectionContext;
    }

    @Override
    public void setServerConnectionContext(final ServerConnectionContext serverConnectionContext) {
        this.serverConnectionContext = serverConnectionContext;
    }
}
