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

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.server.search.AbstractCidsServerSearch;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
public class AlboTeilflaecheSearch extends AbstractCidsServerSearch implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(AlboTeilflaecheSearch.class);
    private static final String QUERY =
        "select geodaten_id, erhebungsnummer, '0' || left(geodaten_id, 5) as laufende_nummer, lpad(right(geodaten_id, 3)::text, 4, '0') as landesregistriernummer "
                + " from albo_flaeche fl where geodaten_id is not null and  geodaten_id <> '' and "
                + " lpad(right(fl.geodaten_id, 3)::text, 4, '0') <> '0000' and not exists (select 1 from albo_flaeche where ('0' || left(geodaten_id, 5)) = ('0' || left(fl.geodaten_id, 5)) and lpad(right(geodaten_id, 3)::text, 4, '0') = '0000') ";

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Alb_BaulastChecker object.
     */
    public AlboTeilflaecheSearch() {
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Collection performServerSearch() {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
        if (ms != null) {
            try {
                final ArrayList<ArrayList> nr = ms.performCustomSearch(QUERY,
                        getConnectionContext());

                return nr;
            } catch (RemoteException ex) {
                LOG.error("Error while checking number", ex);
            }
        }

        return null;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
