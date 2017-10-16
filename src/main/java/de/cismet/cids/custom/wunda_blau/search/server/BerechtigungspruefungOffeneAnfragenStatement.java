/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import de.cismet.cids.server.connectioncontext.ConnectionContext;
import de.cismet.cids.server.connectioncontext.ConnectionContextProvider;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class BerechtigungspruefungOffeneAnfragenStatement extends AbstractCidsServerSearch implements ConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungOffeneAnfragenStatement.class);

    private static final String QUERY_TEMPLATE =
        "SELECT schluessel FROM berechtigungspruefung WHERE pruefstatus IS NULL AND produkttyp IN (%s) ORDER BY anfrage_timestamp DESC";
    private static final String QUERY_PRUEFER_TEMPLATE =
        "SELECT schluessel FROM berechtigungspruefung WHERE pruefstatus AND produkttyp IN (%s)' AND (pruefer IS NULL OR pruefer like '%s') IS NULL ORDER BY anfrage_timestamp DESC";

    //~ Instance fields --------------------------------------------------------

    private final boolean checkPruefer;
    private final Collection<String> produkttypList;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GeschaeftsberichtBranchenAmounts object.
     *
     * @param  produkttypList  DOCUMENT ME!
     */
    public BerechtigungspruefungOffeneAnfragenStatement(final Collection<String> produkttypList) {
        this(false, produkttypList);
    }

    /**
     * Creates a new BerechtigungspruefungOffeneAnfragenStatement object.
     *
     * @param  checkPruefer    DOCUMENT ME!
     * @param  produkttypList  DOCUMENT ME!
     */
    public BerechtigungspruefungOffeneAnfragenStatement(final boolean checkPruefer,
            final Collection<String> produkttypList) {
        this.checkPruefer = checkPruefer;
        this.produkttypList = produkttypList;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
        if (ms != null) {
            try {
                final StringBuffer sb = new StringBuffer();
                for (final String produkttyp : produkttypList) {
                    if (sb.length() != 0) {
                        sb.append(", ");
                    }
                    sb.append("'").append(produkttyp).append("'");
                }
                final String in = sb.toString();
                final String query = (checkPruefer ? String.format(QUERY_PRUEFER_TEMPLATE, in, getUser().getName())
                                                   : String.format(QUERY_TEMPLATE, in));
                final ArrayList<ArrayList> lists = ms.performCustomSearch(query, getConnectionContext());
                final List<String> schluesselListe = new ArrayList();
                if ((lists != null) && !lists.isEmpty()) {
                    for (final List list : lists) {
                        schluesselListe.add((String)list.iterator().next());
                    }
                }
                return schluesselListe;
            } catch (RemoteException ex) {
                LOG.error(ex, ex);
            }
        }
        return null;
    }
    
    @Override
    public ConnectionContext getConnectionContext() {
        return ConnectionContext.create(BerechtigungspruefungOffeneAnfragenStatement.class.getSimpleName());
    }                    
    
}
