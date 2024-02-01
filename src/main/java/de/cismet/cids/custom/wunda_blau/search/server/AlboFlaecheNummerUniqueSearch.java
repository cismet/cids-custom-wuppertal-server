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
public class AlboFlaecheNummerUniqueSearch extends AbstractCidsServerSearch implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(AlboFlaecheErhebungsnummerSearch.class);
    private static final String ERH_NR_QUERY =
        "SELECT id from albo_flaeche where id <> %1$s and erhebungsnummer = '%2$s'";
    private static final String GEO_NR_QUERY = "SELECT id from albo_flaeche where id <> %1$s and geodaten_id = '%2$s'";

    //~ Instance fields --------------------------------------------------------

    private final String nummer;
    private final Integer id;
    private final boolean checkErhebungsnummer;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Alb_BaulastChecker object.
     *
     * @param  nummer                DOCUMENT ME!
     * @param  id                    flaechentyp landesregistriernummer DOCUMENT ME!
     * @param  checkErhebungsnummer  geometryAsText blattnummer DOCUMENT ME!
     */
    public AlboFlaecheNummerUniqueSearch(final String nummer, final Integer id, final boolean checkErhebungsnummer) {
        this.nummer = nummer;
        this.checkErhebungsnummer = checkErhebungsnummer;
        this.id = id;
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
                ArrayList<ArrayList> nr;

                if (checkErhebungsnummer) {
                    nr = ms.performCustomSearch(String.format(
                                ERH_NR_QUERY,
                                id,
                                nummer),
                            getConnectionContext());
                } else {
                    nr = ms.performCustomSearch(String.format(
                                GEO_NR_QUERY,
                                id,
                                nummer),
                            getConnectionContext());
                }

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
