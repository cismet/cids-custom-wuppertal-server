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
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class BerechtigungspruefungOffeneAnfragenStatement extends AbstractCidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungOffeneAnfragenStatement.class);

    private static final String QUERY_TEMPLATE =
        "SELECT schluessel FROM berechtigungspruefung WHERE pruefstatus IS NULL ORDER BY anfrage_timestamp DESC";
    private static final String QUERY_PRUEFER_TEMPLATE =
        "SELECT schluessel FROM berechtigungspruefung WHERE pruefstatus AND (pruefer IS NULL OR pruefer like '%s')  IS NULL ORDER BY anfrage_timestamp DESC";

    //~ Instance fields --------------------------------------------------------

    private final boolean checkPruefer;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GeschaeftsberichtBranchenAmounts object.
     */
    public BerechtigungspruefungOffeneAnfragenStatement() {
        this(false);
    }

    /**
     * Creates a new BerechtigungspruefungOffeneAnfragenStatement object.
     *
     * @param  checkPruefer  DOCUMENT ME!
     */
    public BerechtigungspruefungOffeneAnfragenStatement(final boolean checkPruefer) {
        this.checkPruefer = checkPruefer;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
        if (ms != null) {
            try {
                final String query = (checkPruefer ? String.format(QUERY_PRUEFER_TEMPLATE, getUser().getName())
                                                   : QUERY_TEMPLATE);
                final ArrayList<ArrayList> lists = ms.performCustomSearch(query);
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
}
