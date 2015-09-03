/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import de.cismet.cids.server.search.SearchException;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class BillingJahresberichtReportServerSearch extends BillingStatisticsReportServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BillingJahresberichtReportServerSearch.class);

    public static final String KUNDEN_ABRECHNUNG_WIEDERVERKAEUFER = "kundenAbrechnungWiederverkaeuferJahrlich";
    public static final String ANZAHL_KUNDEN = "anzahlKundenPerGruppe";

    public static final String VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENPFLICHTIG =
        "anzahlGeschaeftsbuchnummernKostenpflichtig";
    public static final String VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENFREI =
        "anzahlGeschaeftsbuchnummernKostenfrei";
    public static final String VERWENDUNGSZWECK_DOWNLOADS_KOSTENPFLICHTIG = "anzahlDownloadsKostenpflichtig";
    public static final String VERWENDUNGSZWECK_DOWNLOADS_KOSTENFREI = "anzahlDownloadsGeschaeftsbuchnummerKostenfrei";
    public static final String VERWENDUNGSZWECK_ANZAHL = "anzahlVerwendungszwecke";
    public static final String VERWENDUNGSZWECK_SUMME_EINNAHMEN = "anzahlVerwendungszweckeSummeEinnahmen";
    public static final String ANZAHL_VERMESSUNGSUNTERLAGEN_TS3 = "anzahlProdukteVermessungsunterlagenTs3";
    public static final String ANZAHL_VERMESSUNGSUNTERLAGEN_TS4 = "anzahlProdukteVermessungsunterlagenTs4";

    //~ Instance fields --------------------------------------------------------

    private final String queryKundenAbrechnungWiederverkaeufer = "select kunde.name  "
                + "from billing_kunde as kunde "
                + "join billing_kunde_kundengruppe_array as verbindung on kunde.id = verbindung.kunde "
                + "join billing_kundengruppe as gruppe on verbindung.billing_kundengruppe_reference = gruppe.kunden_arr "
                + "where gruppe.name ='Abrechnung_Wiederverkaeufer_jährlich'  "
                + "and (vertragsende is null or vertragsende >= '${Jahr}$-01-01') "
                + "order by kunde.name;";

    private final String queryAnzahlKundenPerGruppe = "select gruppe.name, count(kunde.name)"
                + "from billing_kunde as kunde "
                + "join billing_kunde_kundengruppe_array as verbindung on kunde.id = verbindung.kunde "
                + "join billing_kundengruppe as gruppe on verbindung.billing_kundengruppe_reference = gruppe.kunden_arr "
                + "where (vertragsende is null or vertragsende >= '${Jahr}$-12-31') "
                + "group by gruppe.name order by gruppe.name asc;";

    private final String queryAnzahlGeschaeftsbuchnummernKostenpflichtig =
        "select sub.verwendungskey, count(*) from (select distinct geschaeftsbuchnummer,username,verwendungskey from billing_billing  where abrechnungsdatum >='${Jahr}$-01-01' and abrechnungsdatum <='${Jahr}$-12-31' and storniert is null and username not like 'NICHT-ZAEHLEN%' and not (netto_summe =0 or netto_summe is null)) as sub group by sub.verwendungskey;";
    private final String queryAnzahlGeschaeftsbuchnummernKostenfrei =
        "select sub.verwendungskey, count(*) from (select distinct geschaeftsbuchnummer,username,verwendungskey from billing_billing where ts >='${Jahr}$-01-01' and ts <='${Jahr}$-12-31' and storniert is null and username not like 'NICHT-ZAEHLEN%' and (netto_summe =0 or netto_summe is null)) as sub group by sub.verwendungskey;";
    private final String queryAnzahlDownloadsKostenpflichtig =
        "select verwendungskey, count(*) from billing_billing where abrechnungsdatum >='${Jahr}$-01-01' and abrechnungsdatum <='${Jahr}$-12-31' and storniert is null and username not like 'NICHT-ZAEHLEN%' and not (netto_summe =0 or netto_summe is null) group by verwendungskey;";
    private final String queryAnzahlDownloadsKostenfrei =
        "select verwendungskey, count(*) from billing_billing where ts >='${Jahr}$-01-01' and ts <='${Jahr}$-12-31' and storniert is null and username not like 'NICHT-ZAEHLEN%' and (netto_summe =0 or netto_summe is null) group by verwendungskey;";
    private final String queryAnzahlProVerwendungszweck = "";
    private final String querySummeProVerwendungszweck =
        "select verwendungskey, sum(netto_summe) from billing_billing where abrechnungsdatum >='${Jahr}$-01-01' and abrechnungsdatum <='${Jahr}$-12-31' and storniert is null and username not like 'NICHT-ZAEHLEN%' and not (netto_summe =0 or netto_summe is null) group by verwendungskey;";
    private final String queryAnzahlProdukteVermessungsunterlagenTs3 = "select produktbezeichnung,count(id) from ( "
                + "select "
                + "        produktbezeichnung, id "
                + "from billing_billing "
                + "where "
                + "        verwendungszweck = 'Vermessungsunterlagen (amtlicher Lageplan TS 3)' "
                + "        and ts >='${Jahr}$-01-01' "
                + "        and ts <='${Jahr}$-12-31' "
                + "        and storniert is null "
                + "        and username not like 'NICHT-ZAEHLEN%' "
                + "group by produktbezeichnung, id "
                + "order by produktbezeichnung) as temptable "
                + "group by produktbezeichnung;";
    private final String queryAnzahlProdukteVermessungsunterlagenTs4 = "select produktbezeichnung,count(id) from ( "
                + "select "
                + "        produktbezeichnung, id "
                + "from billing_billing "
                + "where "
                + "        verwendungszweck = 'Vermessungsunterlagen (hoheitliche Vermessung TS 4)' "
                + "        and ts >='${Jahr}$-01-01' "
                + "        and ts <='${Jahr}$-12-31' "
                + "        and storniert is null "
                + "        and username not like 'NICHT-ZAEHLEN%' "
                + "group by produktbezeichnung, id "
                + "order by produktbezeichnung) as temptable "
                + "group by produktbezeichnung;";

    private final int year;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GeschaeftsberichtBranchenAmounts object.
     *
     * @param  billingBeanIds  timestampEnd DOCUMENT ME!
     * @param  year            DOCUMENT ME!
     */
    public BillingJahresberichtReportServerSearch(final String billingBeanIds, final int year) {
        super(billingBeanIds);
        this.year = year;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
        if (ms != null) {
            try {
                final HashMap<String, ArrayList> results = new HashMap<String, ArrayList>();

                excuteQueryAndConvertAmountResults(
                    ms,
                    results,
                    queryKundenAbrechnungWiederverkaeufer,
                    KUNDEN_ABRECHNUNG_WIEDERVERKAEUFER);
                excuteQueryAndConvertAmountResults(ms, results, queryAnzahlKundenPerGruppe, ANZAHL_KUNDEN);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    queryAnzahlGeschaeftsbuchnummernKostenpflichtig,
                    VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENPFLICHTIG);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    queryAnzahlGeschaeftsbuchnummernKostenfrei,
                    VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENFREI);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    queryAnzahlDownloadsKostenpflichtig,
                    VERWENDUNGSZWECK_DOWNLOADS_KOSTENPFLICHTIG);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    queryAnzahlDownloadsKostenfrei,
                    VERWENDUNGSZWECK_DOWNLOADS_KOSTENFREI);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    querySummeProVerwendungszweck,
                    VERWENDUNGSZWECK_SUMME_EINNAHMEN);
                excuteQueryAndConvertAmountResults(
                    ms,
                    results,
                    queryAnzahlProdukteVermessungsunterlagenTs3,
                    ANZAHL_VERMESSUNGSUNTERLAGEN_TS3);
                excuteQueryAndConvertAmountResults(
                    ms,
                    results,
                    queryAnzahlProdukteVermessungsunterlagenTs4,
                    ANZAHL_VERMESSUNGSUNTERLAGEN_TS4);

                final Collection sup = super.performServerSearch();
                if (sup != null) {
                    results.putAll((HashMap)sup.iterator().next());
                }

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
    private void excuteQueryAndConvertAmountResults(final MetaService ms,
            final HashMap<String, ArrayList> results,
            final String query,
            final String key) throws RemoteException {
        final ArrayList<ArrayList> lists = ms.performCustomSearch(query.replace("${Jahr}$", Integer.toString(year)));
        if ((lists != null) && !lists.isEmpty()) {
            final ArrayList<AmountBean> beans = new ArrayList<AmountBean>();
            for (final Iterator it = lists.iterator(); it.hasNext();) {
                final ArrayList row = (ArrayList)it.next();

                final AmountBean bean = new AmountBean();
                if (row.size() == 1) {
                    bean.setName((String)row.get(0));
                } else {
                    bean.setName((String)row.get(0));
                    bean.setNumber((Number)row.get(1));
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
     * @param   query    DOCUMENT ME!
     * @param   key      DOCUMENT ME!
     *
     * @throws  RemoteException  DOCUMENT ME!
     */
    private void excuteQueryAndConvertAmountPerVerwendungszweckResults(final MetaService ms,
            final HashMap<String, ArrayList> results,
            final String query,
            final String key) throws RemoteException {
        LOG.fatal(query.replace("${Jahr}$", Integer.toString(year)));
        final ArrayList<ArrayList> lists = ms.performCustomSearch(query.replace("${Jahr}$", Integer.toString(year)));
        if ((lists != null)) {
            final AnzahlProVerwendungszweckBean bean = new AnzahlProVerwendungszweckBean();
            for (final Iterator it = lists.iterator(); it.hasNext();) {
                final ArrayList row = (ArrayList)it.next();

                final String name = (String)row.get(0);
                final Number number = (Number)row.get(1);
                if ("eigG".equalsIgnoreCase(name)) {
                    bean.setNumberEigG(number);
                } else if ("eigG frei".equalsIgnoreCase(name)) {
                    bean.setNumberEigG_frei(number);
                } else if ("VU aL".equalsIgnoreCase(name)) {
                    bean.setNumberVU_aL(number);
                } else if ("VU hV".equalsIgnoreCase(name)) {
                    bean.setNumberVU_hV(number);
                } else if ("VU s".equalsIgnoreCase(name)) {
                    bean.setNumberVU_s(number);
                } else if ("WV ein".equalsIgnoreCase(name)) {
                    bean.setNumberWV_ein(number);
                }
            }
            final ArrayList<AnzahlProVerwendungszweckBean> beans = new ArrayList<AnzahlProVerwendungszweckBean>();
            beans.add(bean);
            results.put(key, beans);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    public class AmountBean implements Serializable {

        //~ Instance fields ----------------------------------------------------

        private Number number = (long)0;
        private String name = "";
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    public class KundeBean implements Serializable {

        //~ Instance fields ----------------------------------------------------

        private String name = "";
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    public class AnzahlProVerwendungszweckBean implements Serializable {

        //~ Instance fields ----------------------------------------------------

        private Number numberEigG = (long)0;
        private Number numberEigG_frei = (long)0;
        private Number numberVU_aL = (long)0;
        private Number numberVU_hV = (long)0;
        private Number numberVU_s = (long)0;
        private Number numberWV_ein = (long)0;
    }
}
