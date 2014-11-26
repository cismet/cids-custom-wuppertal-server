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
 * A server search which fetches the needed information for the charts in the billing statistics report. To fetch the
 * data, multiple queries are executed and their results are converted to beans ({@link BrancheAmountBean} or
 * {@link EinnahmeBean}). These beans are added to a HashMap, which is returned in {@code performSeverSearch}.
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
public class BillingStatisticsReportServerSearch extends AbstractCidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BillingStatisticsReportServerSearch.class);
    private static final String DOMAIN = "WUNDA_BLAU";
    public static final String BRANCHEN_AMOUNTS = "branchenAmounts";
    public static final String ANTRAEGE_AMOUNTS = "antraegeAmounts";
    public static final String DOWNLOADS_AMOUNTS = "downloadAmounts";
    public static final String KUNDEN_UMSATZ = "kundenUmsatz";
    public static final String PRODUKTE_COMMON_DOWNLOADS = "produkteCommonDownloads";
    public static String PRODUKTE_DOWNLOADS = "produkteDownloads";
    public static String PRODUKTE_EINNAHMEN = "produkteEinnahmen";
    public static String EINNAHMEN = "einnahmen";

    //~ Instance fields --------------------------------------------------------

    String whereClause = "WHERE b.id IN ($bean_ids$)\n";
    String fromBillingJoinTillKunde = "FROM billing_billing AS b\n"
                + "JOIN billing_kunden_logins AS login ON b.angelegt_durch = login.id\n"
                + "JOIN billing_kunde AS kunde ON login.kunde = kunde.id\n"
                + "JOIN billing_branche ON kunde.branche = billing_branche.id\n";

    String queryKundenBranche = "with tempTabel as (SELECT count(b.username),\n"
                + "billing_branche.name\n"
                + fromBillingJoinTillKunde
                + whereClause
                + "GROUP BY billing_branche.name,b.username\n"
                + "ORDER BY billing_branche.name DESC)\n"
                + "select count(name) as anzahl,name from tempTabel group by name order by anzahl desc;";

    String queryKundenAntraege = "with tempTable as (\n"
                + "select kunde.name,geschaeftsbuchnummer\n"
                + fromBillingJoinTillKunde
                + whereClause
                + "group by kunde.name,geschaeftsbuchnummer\n"
                + "order by kunde.name)\n"
                + "select count(name) as Anzahl,name from tempTable group by name order by Anzahl desc limit 10;";

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

    String queryProdukteCommonDownloads = "select count(*) as anzahl, produktbezeichnung, produktkey\n"
                + "        from\n"
                + "                billing_billing as b\n"
                + whereClause
                + "group by produktkey,produktbezeichnung\n"
                + "order by anzahl  desc   limit 10;";

    String queryProdukteDownloads = "select count(*) as anzahl, produktbezeichnung, produktkey\n"
                + "        from\n"
                + "                billing_billing as b\n"
                + whereClause
                + "group by produktkey,produktbezeichnung\n"
                + "order by produktbezeichnung  asc;";

    String queryProdukteEinnahmen =
        "select sum(brutto_summe) as summe, produktbezeichnung, count(brutto_summe) as anzahlProdukte\n"
                + "        from\n"
                + "                billing_billing as b\n"
                + whereClause
                + "group by produktbezeichnung\n"
                + "order by summe  desc;";

    String queryEinnahmen = "select y.gesum,y.gesum/360*2 as minsum,z.produktbezeichnung,z.summe,z.anzahl from \n"
                + "(select sum(summe) as gesum from \n"
                + "(select produktbezeichnung,sum(brutto_summe) as summe,count(brutto_summe) as anzahl\n"
                + "        from\n"
                + "                billing_billing as b\n"
                + whereClause
                + "group by produktbezeichnung) x) y,\n"
                + "(select produktbezeichnung,sum(brutto_summe) as summe,count(brutto_summe) as anzahl\n"
                + "        from\n"
                + "                billing_billing as b\n"
                + whereClause
                + "group by produktbezeichnung) z\n"
                + "where z.summe > y.gesum/360*2\n"
                + "order by z.summe desc;";

    private final User user;
    private final String billingBeanIds;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GeschaeftsberichtBranchenAmounts object.
     *
     * @param  user            DOCUMENT ME!
     * @param  billingBeanIds  timestampEnd DOCUMENT ME!
     */
    public BillingStatisticsReportServerSearch(final User user, final String billingBeanIds) {
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
                excuteQueryAndConvertResults(ms, results, queryProdukteCommonDownloads, PRODUKTE_COMMON_DOWNLOADS);
                excuteQueryAndConvertResults(ms, results, queryProdukteDownloads, PRODUKTE_DOWNLOADS);
                excuteQueryAndConvertResults(ms, results, queryProdukteEinnahmen, PRODUKTE_EINNAHMEN);

                excuteEinnahmenQuery(ms, results);

                // a collection must be returned, therefore wrap the HashMap in a Collection
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
                bean.number = (Number)row.get(0);
                bean.name = (String)row.get(1);

                if (row.size() == 3) {
                    bean.info = row.get(2);
                }

                beans.add(bean);
            }
            results.put(key, beans);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   ms       DOCUMENT ME!
     * @param   results  DOCUMENT ME!
     *
     * @throws  RemoteException  DOCUMENT ME!
     */
    private void excuteEinnahmenQuery(final MetaService ms,
            final HashMap<String, ArrayList> results) throws RemoteException {
        queryEinnahmen = queryEinnahmen.replace("$bean_ids$", billingBeanIds);
        final ArrayList<ArrayList> lists = ms.performCustomSearch(queryEinnahmen);
        if ((lists != null) && !lists.isEmpty()) {
            final ArrayList<EinnahmenBean> beans = new ArrayList<EinnahmenBean>();
            for (final Iterator it = lists.iterator(); it.hasNext();) {
                final ArrayList row = (ArrayList)it.next();

                final EinnahmenBean bean = new EinnahmenBean();
                bean.gesum = (Double)row.get(0);
                bean.minsum = (Double)row.get(1);
                bean.produktbezeichnung = (String)row.get(2);
                bean.summe = (Double)row.get(3);
                bean.anzahl = (Long)row.get(4);

                beans.add(bean);
            }
            results.put(EINNAHMEN, beans);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * A Bean which is used to create a JRDataSource for the big pie chart.
     *
     * @version  $Revision$, $Date$
     */
    public class EinnahmenBean implements Serializable {

        //~ Instance fields ----------------------------------------------------

        Double gesum;
        Double minsum;
        Double summe;
        String produktbezeichnung;
        Long anzahl;

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public Double getGesum() {
            return gesum;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public Double getMinsum() {
            return minsum;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public Double getSumme() {
            return summe;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getProduktbezeichnung() {
            return produktbezeichnung;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public Long getAnzahl() {
            return anzahl;
        }
    }

    /**
     * A Bean which is used to create a JRDataSource for the charts in the report. It got only three fields but several
     * getters for these fields with different names. Thus the fields in the report can have different, more specific
     * names.
     *
     * @version  $Revision$, $Date$
     */
    public class BrancheAmountBean implements Serializable {

        //~ Instance fields ----------------------------------------------------

        Number number = (long)0;
        String name = "";
        Object info = "";

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
         * @return  DOCUMENT ME!
         */
        public Number getSumme() {
            return number;
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
         * @return  DOCUMENT ME!
         */
        public String getProduktbezeichnung() {
            return name;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public Object getInfo() {
            return info;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public Object getProduktkey() {
            return info;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public Object getAnzahlProdukte() {
            return info;
        }
    }
}
