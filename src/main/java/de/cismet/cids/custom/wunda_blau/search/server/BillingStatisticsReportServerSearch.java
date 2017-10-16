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
import de.cismet.cids.server.connectioncontext.ConnectionContext;
import de.cismet.cids.server.connectioncontext.ConnectionContextProvider;

import lombok.Getter;
import lombok.Setter;

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
public class BillingStatisticsReportServerSearch extends AbstractCidsServerSearch implements ConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BillingStatisticsReportServerSearch.class);

    public static final String BRANCHEN_AMOUNTS = "branchenAmounts";
    public static final String ANTRAEGE_AMOUNTS = "antraegeAmounts";
    public static final String DOWNLOADS_AMOUNTS = "downloadAmounts";
    public static final String KUNDEN_UMSATZ = "kundenUmsatz";
    public static final String PRODUKTE_COMMON_DOWNLOADS = "produkteCommonDownloads";
    public static final String PRODUKTE_DOWNLOADS = "produkteDownloads";
    public static final String PRODUKTE_EINNAHMEN = "produkteEinnahmen";
    public static final String EINNAHMEN = "einnahmen";

    //~ Instance fields --------------------------------------------------------

    private final String whereClause = "WHERE b.id IN ($bean_ids$) ";
    private final String fromBillingJoinTillKunde = "FROM billing_billing AS b "
                + "JOIN billing_kunden_logins AS login ON b.angelegt_durch = login.id "
                + "JOIN billing_kunde AS kunde ON login.kunde = kunde.id "
                + "JOIN billing_branche ON kunde.branche = billing_branche.id ";

    private final String queryKundenBranche = "with tempTabel as (SELECT count(b.username), "
                + "billing_branche.name "
                + fromBillingJoinTillKunde
                + whereClause
                + "GROUP BY billing_branche.name,b.username "
                + "ORDER BY billing_branche.name DESC) "
                + "select count(name) as anzahl,name from tempTabel group by name order by anzahl desc;";

    private final String queryKundenAntraege = "with tempTable as ( "
                + "select kunde.name,geschaeftsbuchnummer "
                + fromBillingJoinTillKunde
                + whereClause
                + "group by kunde.name,geschaeftsbuchnummer "
                + "order by kunde.name) "
                + "select count(name) as Anzahl,name from tempTable group by name order by Anzahl desc limit 10;";

    private final String queryKundenAnzahlDownloads = "select count (*) as amount,kunde.name "
                + fromBillingJoinTillKunde
                + whereClause
                + "group by kunde.name "
                + "order by amount desc limit 10;";

    private final String queryKundenUmsatz = "select sum(netto_summe) as summe, kunde.name "
                + fromBillingJoinTillKunde
                + whereClause
                + "group by kunde.name "
                + "order by summe desc limit 10";

    private final String queryProdukteCommonDownloads = "select count(*) as anzahl, produktbezeichnung, produktkey "
                + "        from "
                + "                billing_billing as b "
                + whereClause
                + "group by produktkey,produktbezeichnung "
                + "order by anzahl  desc   limit 10;";

    private final String queryProdukteDownloads = "select count(*) as anzahl, produktbezeichnung, produktkey "
                + "        from "
                + "                billing_billing as b "
                + whereClause
                + "group by produktkey,produktbezeichnung "
                + "order by produktbezeichnung  asc;";

    private final String queryProdukteEinnahmen =
        "select sum(brutto_summe) as summe, produktbezeichnung, count(brutto_summe) as anzahlProdukte "
                + "        from "
                + "                billing_billing as b "
                + whereClause
                + "group by produktbezeichnung "
                + "order by summe  desc;";

    private final String queryEinnahmen =
        "select y.gesum,y.gesum/360*2 as minsum,z.produktbezeichnung,z.summe,z.anzahl from  "
                + "(select sum(summe) as gesum from  "
                + "(select produktbezeichnung,sum(brutto_summe) as summe,count(brutto_summe) as anzahl "
                + "        from "
                + "                billing_billing as b "
                + whereClause
                + "group by produktbezeichnung) x) y, "
                + "(select produktbezeichnung,sum(brutto_summe) as summe,count(brutto_summe) as anzahl "
                + "        from "
                + "                billing_billing as b "
                + whereClause
                + "group by produktbezeichnung) z "
                + "where z.summe > y.gesum/360*2 "
                + "order by z.summe desc;";

    private final String billingBeanIds;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GeschaeftsberichtBranchenAmounts object.
     *
     * @param  billingBeanIds  timestampEnd DOCUMENT ME!
     */
    public BillingStatisticsReportServerSearch(final String billingBeanIds) {
        this.billingBeanIds = billingBeanIds;
    }

    /**
     * Creates a new BillingStatisticsReportServerSearch object.
     *
     * @param  user            DOCUMENT ME!
     * @param  billingBeanIds  DOCUMENT ME!
     */
    public BillingStatisticsReportServerSearch(final User user, final String billingBeanIds) {
        this(billingBeanIds);
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
            final String query,
            final String key) throws RemoteException {
        final ArrayList<ArrayList> lists = ms.performCustomSearch(query.replace("$bean_ids$", billingBeanIds), getConnectionContext());
        if ((lists != null) && !lists.isEmpty()) {
            final ArrayList<BrancheAmountBean> beans = new ArrayList<BrancheAmountBean>();
            for (final Iterator it = lists.iterator(); it.hasNext();) {
                final ArrayList row = (ArrayList)it.next();

                final BrancheAmountBean bean = new BrancheAmountBean();
                bean.setNumber((Number)row.get(0));
                bean.setName((String)row.get(1));

                if (row.size() == 3) {
                    bean.setInfo(row.get(2));
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
        final ArrayList<ArrayList> lists = ms.performCustomSearch(queryEinnahmen.replace("$bean_ids$", billingBeanIds), getConnectionContext());
        if ((lists != null) && !lists.isEmpty()) {
            final ArrayList<EinnahmenBean> beans = new ArrayList<EinnahmenBean>();
            for (final Iterator it = lists.iterator(); it.hasNext();) {
                final ArrayList row = (ArrayList)it.next();

                final EinnahmenBean bean = new EinnahmenBean();
                bean.setGesum((Double)row.get(0));
                bean.setMinsum((Double)row.get(1));
                bean.setProduktbezeichnung((String)row.get(2));
                bean.setSumme((Double)row.get(3));
                bean.setAnzahl((Long)row.get(4));

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
    @Getter
    @Setter
    public class EinnahmenBean implements Serializable {

        //~ Instance fields ----------------------------------------------------

        private Double gesum;
        private Double minsum;
        private Double summe;
        private String produktbezeichnung;
        private Long anzahl;
    }

    /**
     * A Bean which is used to create a JRDataSource for the charts in the report. It got only three fields but several
     * getters for these fields with different names. Thus the fields in the report can have different, more specific
     * names.
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    public class BrancheAmountBean implements Serializable {

        //~ Instance fields ----------------------------------------------------

        private Number number = (long)0;
        private String name = "";
        private Object info = "";
    }
    
    @Override
    public ConnectionContext getConnectionContext() {
        return ConnectionContext.create(BillingStatisticsReportServerSearch.class.getSimpleName());
    }                    
        
}
