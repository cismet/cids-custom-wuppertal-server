/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.newuser.User;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
public class GeschaeftsberichtBranchenAmounts extends AbstractCidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            GeschaeftsberichtBranchenAmounts.class);
    private static final String DOMAIN = "WUNDA_BLAU";

    //~ Instance fields --------------------------------------------------------

    String query = "SELECT count(*) AS amount,\n"
                + "       billing_branche.name\n"
                + "FROM billing_billing AS b\n"
                + "JOIN billing_kunden_logins AS login ON b.angelegt_durch = login.id\n"
                + "JOIN billing_kunde AS kunde ON login.kunde = kunde.id\n"
                + "JOIN billing_branche ON kunde.branche = billing_branche.id\n"
                + "WHERE b.id IN ($bean_ids$)\n"
                + "GROUP BY billing_branche.name\n"
                + "ORDER BY amount DESC;";
    private final User user;
    private final String billingBeanIds;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GeschaeftsberichtBranchenAmounts object.
     *
     * @param  user            DOCUMENT ME!
     * @param  billingBeanIds  timestampEnd DOCUMENT ME!
     */
    public GeschaeftsberichtBranchenAmounts(final User user, final String billingBeanIds) {
        this.user = user;
        this.billingBeanIds = billingBeanIds;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
        if (ms != null) {
            try {
                query = query.replace("$bean_ids$", billingBeanIds);

                final ArrayList<ArrayList> lists = ms.performCustomSearch(query);
                return lists;
            } catch (RemoteException ex) {
            }
        }
        return null;
    }
}
