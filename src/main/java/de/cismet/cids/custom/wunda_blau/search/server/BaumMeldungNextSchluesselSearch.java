/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;
import java.rmi.RemoteException;

/**
 * Search next value for the schluessel property of a new baum object.
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
public class BaumMeldungNextSchluesselSearch extends AbstractCidsServerSearch implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumMeldungNextSchluesselSearch.class);
    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String QUERY = "select nextval('baum_meldung_schluessel_seq')";

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LightweightMetaObjectsByQuerySearch object.
     */
    public BaumMeldungNextSchluesselSearch() {
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Collection performServerSearch() throws SearchException {
        try {
            final MetaService metaService = (MetaService)this.getActiveLocalServers().get(DOMAIN);

            if (metaService != null) {
                final ArrayList<ArrayList> list = metaService.performCustomSearch(QUERY, getConnectionContext());

                if ((list.size() > 0) && (list.get(0).size() > 0)) {
                    return list.get(0);
                }
            } else {
                LOG.error("active local server not found"); // NOI18N
            }

            return null;
        } catch (final RemoteException ex) {
            throw new SearchException("error while loading meldung objects", ex);
        }
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
