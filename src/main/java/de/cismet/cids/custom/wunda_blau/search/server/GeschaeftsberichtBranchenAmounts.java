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

import java.io.Serializable;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

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
    public static final String BRANCHEN_AMOUNTS = "branchenAmounts";
    public static final String ANTRAEGE_AMOUNTS = "antraegeAmounts";
    public static final String DOWNLOADS_AMOUNTS = "downloadAmounts";
    public static final String KUNDEN_UMSATZ = "kundenUmsatz";

    //~ Instance fields --------------------------------------------------------

    String whereClause = "WHERE b.id IN ($bean_ids$)\n";
    String fromBillingJoinTillKunde = "FROM billing_billing AS b\n"
                + "JOIN billing_kunden_logins AS login ON b.angelegt_durch = login.id\n"
                + "JOIN billing_kunde AS kunde ON login.kunde = kunde.id\n";

    String queryKundenBranche = "SELECT count(*) AS amount,\n"
                + "       billing_branche.name\n"
                + fromBillingJoinTillKunde
                + "JOIN billing_branche ON kunde.branche = billing_branche.id\n"
                + whereClause
                + "GROUP BY billing_branche.name\n"
                + "ORDER BY amount DESC;";

    String queryKundenAntraege = "select count(*) as amount,kunde.name, b.geschaeftsbuchnummer\n"
                + fromBillingJoinTillKunde
                + whereClause
                + "group by kunde.name,b.geschaeftsbuchnummer\n"
                + "order by amount desc limit 10;";

    String queryKundenAnzahlDownloads = "select count (*) as amount,kunde.name\n"
                + fromBillingJoinTillKunde
                + whereClause
                + "group by kunde.name\n"
                + "order by amount desc limit 10;";

    String queryKundenUmsatz = "select sum(netto_summe) as summe, kunde.name\n"
                + fromBillingJoinTillKunde
                + whereClause
                + "group by kunde.name\n"
                + "order by summe desc limit 10";

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
                final HashMap<String, ArrayList> results = new HashMap<String, ArrayList>();

                excuteQueryAndConvertResults(ms, results, queryKundenBranche, BRANCHEN_AMOUNTS);
                excuteQueryAndConvertResults(ms, results, queryKundenAntraege, ANTRAEGE_AMOUNTS);
                excuteQueryAndConvertResults(ms, results, queryKundenAnzahlDownloads, DOWNLOADS_AMOUNTS);
                excuteQueryAndConvertResults(ms, results, queryKundenUmsatz, KUNDEN_UMSATZ);

                final ArrayList resultWrapper = new ArrayList(1);
                resultWrapper.add(results);
                return resultWrapper;
            } catch (RemoteException ex) {
                LOG.error(ex, ex);
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   ms       DOCUMENT ME!
     * @param   results  DOCUMENT ME!
     * @param   query    DOCUMENT ME!
     * @param   key      DOCUMENT ME!
     *
     * @throws  RemoteException  DOCUMENT ME!
     */
    private void excuteQueryAndConvertResults(final MetaService ms,
            final HashMap<String, ArrayList> results,
            String query,
            final String key) throws RemoteException {
        query = query.replace("$bean_ids$", billingBeanIds);
        final ArrayList<ArrayList> lists = ms.performCustomSearch(query);
        if ((lists != null) && !lists.isEmpty()) {
            final ArrayList<BrancheAmountBean> beans = new ArrayList<BrancheAmountBean>();
            for (final Iterator it = lists.iterator(); it.hasNext();) {
                final ArrayList row = (ArrayList)it.next();

                final BrancheAmountBean bean = new BrancheAmountBean();
                final Number amount = (Number)row.get(0);
                bean.number = amount;

                final String branche = (String)row.get(1);
                bean.name = branche;

                if (row.size() == 3) {
                    bean.info = (String)row.get(2);
                }

                beans.add(bean);
            }
            results.put(key, beans);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public class BrancheAmountBean implements Serializable {

        //~ Instance fields ----------------------------------------------------

        Number number = (long)0;
        String name = "";
        String info = "";

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new BrancheAmountBean object.
         */
        public BrancheAmountBean() {
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public Number getAnzahl() {
            return number;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  number  DOCUMENT ME!
         */
        public void setAnzahl(final Number number) {
            this.number = number;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public Number getSumme() {
            return number;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  number  anzahl DOCUMENT ME!
         */
        public void setSumme(final Number number) {
            this.number = number;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getName() {
            return name;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  name  DOCUMENT ME!
         */
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getInfo() {
            return info;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  info  DOCUMENT ME!
         */
        public void setInfo(final String info) {
            this.info = info;
        }
    }
}
