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
public class AlboFlaecheErhebungsnummerSearch extends AbstractCidsServerSearch implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(AlboFlaecheErhebungsnummerSearch.class);
    private static final String ERH_NR_QUERY = "SELECT cs_albo_create_erhebungsnummer(%1$s::geometry, %2$s, %3$s)";

    //~ Instance fields --------------------------------------------------------

    private final String geometryAsText;
    private final String flaechentyp;
    private final String prop;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Alb_BaulastChecker object.
     *
     * @param  geometryAsText  blattnummer DOCUMENT ME!
     * @param  flaechentyp     landesregistriernummer DOCUMENT ME!
     * @param  prop            DOCUMENT ME!
     */
    public AlboFlaecheErhebungsnummerSearch(final String geometryAsText, final String flaechentyp, final String prop) {
        this.geometryAsText = geometryAsText;
        this.flaechentyp = flaechentyp;
        this.prop = prop;
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
                final ArrayList<ArrayList> erhNr = ms.performCustomSearch(String.format(
                            ERH_NR_QUERY,
                            ((geometryAsText == null) ? "null" : ("'" + geometryAsText + "'")),
                            ((flaechentyp == null) ? "null" : ("'" + flaechentyp + "'")),
                            ((prop == null) ? "null" : ("'" + prop + "'"))),
                        getConnectionContext());

                return erhNr;
            } catch (RemoteException ex) {
                LOG.error("Error while creating erhebungsnummer", ex);
            }
        }

        return null;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
