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

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.connectioncontext.ServerConnectionContext;
import de.cismet.connectioncontext.ServerConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
public class Sb_maxBildnummerFetcherServerSearch extends AbstractCidsServerSearch
        implements ServerConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final String searchQuery =
        "select max(bildnummer::int) from sb_stadtbild where bildnummer ~ '^\\\\d{6}$'";

    //~ Instance fields --------------------------------------------------------

    private ServerConnectionContext connectionContext = ServerConnectionContext.create(getClass().getSimpleName());

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
        if (ms != null) {
            try {
                final ArrayList<ArrayList> lists = ms.performCustomSearch(searchQuery, getConnectionContext());
                return lists;
            } catch (RemoteException ex) {
            }
        }
        return null;
    }

    @Override
    public ServerConnectionContext getConnectionContext() {
        return connectionContext;
    }

    @Override
    public void initAfterConnectionContext() {
    }

    @Override
    public void setConnectionContext(final ServerConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }
}
