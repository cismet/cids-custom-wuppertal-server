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

import de.cismet.cids.server.connectioncontext.ServerConnectionContext;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;
import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * Search next value for the schluessel property of a new kk_kompensation object.
 *
 * @author   Thorsten Herter
 * @version  $Revision$, $Date$
 */
public class KkKompensationNextSchluesselSearch extends AbstractCidsServerSearch
        implements ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(KkKompensationNextSchluesselSearch.class);
    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String QUERY = "select nextval('kk_kompensation_schluessel_seq')";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LightweightMetaObjectsByQuerySearch object.
     */
    public KkKompensationNextSchluesselSearch() {
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        try {
            final MetaService metaService = (MetaService)this.getActiveLocalServers().get(DOMAIN);

            if (metaService != null) {
                final ArrayList<ArrayList> list = metaService.performCustomSearch(QUERY, getServerConnectionContext());

                if ((list.size() > 0) && (list.get(0).size() > 0)) {
                    return list.get(0);
                }
            } else {
                LOG.error("active local server not found"); // NOI18N
            }

            return null;
        } catch (final Exception ex) {
            throw new SearchException("error while loading verfahren objects", ex);
        }
    }

    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return ServerConnectionContext.create(KkKompensationNextSchluesselSearch.class.getSimpleName());
    }
}
