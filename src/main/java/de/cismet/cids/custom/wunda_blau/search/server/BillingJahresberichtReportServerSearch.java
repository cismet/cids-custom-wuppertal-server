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

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import de.cismet.cids.server.search.SearchException;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class BillingJahresberichtReportServerSearch extends BillingStatisticsReportServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BillingJahresberichtReportServerSearch.class);

    private static final String[] PRODUCTS_LK = new String[] {
            "fsnw",
            "fsuenw",
            "benw",
            "bestnw",
            "grnw",
            "fknw4",
            "fknw3",
            "fknw2",
            "fknw1",
            "fknw0",
            "schknw4",
            "schknw3",
            "schknw2",
            "schknw1",
            "schknw0",
            "abknw4",
            "abknw3",
            "abknw2",
            "abknw1",
            "abknw0",
            "pktlsttxt",
            "pktlstpdf",
            "fnp4",
            "fnp3",
            "fnp2",
            "fnp1",
            "fnp0",
            "appdf",
            "nivppdf",
            "vrpdf",
            "vrpdf_a2",
            "doklapdf",
            "doklapdf_a2",
            "nasoeig",
            "naspkt",
            "abktiff",
            "dgm1",
            "dgm5",
            "dgm10",
            "gebu",
            "flu",
        };
    private static final String[] PRODUCTS_BL = new String[] { "bla", "blab_be" };
    private static final String[] PRODUCTS_KO = new String[] {
            "ofkom0",
            "ofkom1",
            "ofkom2",
            "ofkom3",
            "ofkom4",
            "abkhkom0",
            "abkhkom1",
            "abkhkom2",
            "abkhkom3",
            "abkhkom4",
            "skhkom0",
            "skhkom1",
            "skhkom2",
            "skhkom3",
            "skhkom4",
            "ofkkom0",
            "ofkkom1",
            "ofkkom2",
            "ofkkom3",
            "ofkkom4",
            "sptiff",
            "ortho5",
            "ortho15",
            "hoeli1",
            "hoeli5",
            "hoept",
            "adr",
            "stb",
            "fsuekom",
            "bekom",
            "skmekom4",
            "skmekom3",
            "skmekom2",
            "skmekom1",
            "skmekom0",
            "skkom4",
            "skkom3",
            "skkom2",
            "skkom1",
            "skkom0",
            "dgkkom4",
            "dgkkom3",
            "dgkkom2",
            "dgkkom1",
            "dgkkom0",
            "nivpükom4",
            "nivpükom3",
            "nivpükom2",
            "nivpükom1",
            "nivpükom0",
            "apükom4",
            "apükom3",
            "apükom2",
            "apükom1",
            "apükom0",
            "pnükom4",
            "pnükom3",
            "pnükom2",
            "pnükom1",
            "pnükom0",
            "eiglkom",
            "skmekomdxf",
            "skmekomtiff",
            "naskom",
        };

    public static final String KUNDEN_ABRECHNUNG_WIEDERVERKAEUFER = "kundenAbrechnungWiederverkaeuferJahrlich";
    public static final String ANZAHL_KUNDEN = "anzahlKundenPerGruppe";

    public static final String VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENPFLICHTIG_LK =
        "anzahlGeschaeftsbuchnummernKostenpflichtigLk";
    public static final String VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENPFLICHTIG_BL =
        "anzahlGeschaeftsbuchnummernKostenpflichtigBl";
    public static final String VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENPFLICHTIG_KO =
        "anzahlGeschaeftsbuchnummernKostenpflichtigKo";

    public static final String VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENFREI_LK =
        "anzahlGeschaeftsbuchnummernKostenfreiLk";
    public static final String VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENFREI_BL =
        "anzahlGeschaeftsbuchnummernKostenfreiBl";
    public static final String VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENFREI_KO =
        "anzahlGeschaeftsbuchnummernKostenfreiKo";

    public static final String VERWENDUNGSZWECK_DOWNLOADS_KOSTENPFLICHTIG_LK = "anzahlDownloadsKostenpflichtigLk";
    public static final String VERWENDUNGSZWECK_DOWNLOADS_KOSTENPFLICHTIG_BL = "anzahlDownloadsKostenpflichtigBl";
    public static final String VERWENDUNGSZWECK_DOWNLOADS_KOSTENPFLICHTIG_KO = "anzahlDownloadsKostenpflichtigKo";

    public static final String VERWENDUNGSZWECK_DOWNLOADS_KOSTENFREI_LK =
        "anzahlDownloadsGeschaeftsbuchnummerKostenfreiLk";
    public static final String VERWENDUNGSZWECK_DOWNLOADS_KOSTENFREI_BL =
        "anzahlDownloadsGeschaeftsbuchnummerKostenfreiBl";
    public static final String VERWENDUNGSZWECK_DOWNLOADS_KOSTENFREI_KO =
        "anzahlDownloadsGeschaeftsbuchnummerKostenfreiKo";

    public static final String VERWENDUNGSZWECK_ANZAHL_LK = "anzahlVerwendungszweckeLk";
    public static final String VERWENDUNGSZWECK_ANZAHL_KO = "anzahlVerwendungszweckeBl";
    public static final String VERWENDUNGSZWECK_ANZAHL_BL = "anzahlVerwendungszweckeKo";

    public static final String VERWENDUNGSZWECK_SUMME_EINNAHMEN_LK = "anzahlVerwendungszweckeSummeEinnahmenLk";
    public static final String VERWENDUNGSZWECK_SUMME_EINNAHMEN_BL = "anzahlVerwendungszweckeSummeEinnahmenBl";
    public static final String VERWENDUNGSZWECK_SUMME_EINNAHMEN_KO = "anzahlVerwendungszweckeSummeEinnahmenKo";
    public static final String ANZAHL_VERMESSUNGSUNTERLAGEN_TS3 = "anzahlProdukteVermessungsunterlagenTs3";
    public static final String ANZAHL_VERMESSUNGSUNTERLAGEN_TS4 = "anzahlProdukteVermessungsunterlagenTs4";

    private static final SimpleDateFormat POSTGRES_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final String QUERY_KUNDEN_ABRECHNUNG_WIEDERVERKAUEFER = "select kunde.name  "
                + "from billing_kunde as kunde "
                + "join billing_kunde_kundengruppe_array as verbindung on kunde.id = verbindung.kunde "
                + "join billing_kundengruppe as gruppe on verbindung.billing_kundengruppe_reference = gruppe.kunden_arr "
                + "where gruppe.name ='Abrechnung_Wiederverkaeufer_jährlich'  "
                + "and (vertragsende is null or date_trunc('day',vertragsende) >= '${from}' and date_trunc('day',vertragsende) <= '${till}') "
                + "order by kunde.name;";

    private static final String QUERY_ANZAHL_KUNDEN_PER_GRUPPE = "select gruppe.name, count(kunde.name)"
                + "from billing_kunde as kunde "
                + "join billing_kunde_kundengruppe_array as verbindung on kunde.id = verbindung.kunde "
                + "join billing_kundengruppe as gruppe on verbindung.billing_kundengruppe_reference = gruppe.kunden_arr "
                + "where (vertragsende is null or vertragsende >= '${till}') "
                + "group by gruppe.name order by gruppe.name asc;";

    private static final String QUERY_ANZAHL_GESCHAEFTSBUCHNUMMER_KOSTENPLICHTIG =
        "select sub.verwendungskey, count(*) from (select distinct geschaeftsbuchnummer,username,verwendungskey from billing_billing  where ${productKeys} and date_trunc('day',abrechnungsdatum) >= '${from}' and date_trunc('day',abrechnungsdatum) <= '${till}' and storniert is null and username not like 'NICHT-ZAEHLEN%' and not (netto_summe =0 or netto_summe is null)) as sub group by sub.verwendungskey;";
    private static final String QUERY_ANZAHL_GESCHAEFTSBUCHNUMMER_KOSTENFREI =
        "select sub.verwendungskey, count(*) from (select distinct geschaeftsbuchnummer,username,verwendungskey from billing_billing where ${productKeys} and date_trunc('day',ts) >= '${from}' and date_trunc('day',ts) <= '${till}' and storniert is null and username not like 'NICHT-ZAEHLEN%' and (netto_summe =0 or netto_summe is null)) as sub group by sub.verwendungskey;";
    private static final String QUERY_ANZAHL_DOWNLOADS_KOSTENPFLICHTIG =
        "select verwendungskey, count(*) from billing_billing where ${productKeys} and date_trunc('day',abrechnungsdatum) >= '${from}' and date_trunc('day',abrechnungsdatum) <= '${till}' and storniert is null and username not like 'NICHT-ZAEHLEN%' and not (netto_summe =0 or netto_summe is null) group by verwendungskey;";
    private static final String QUERY_ANZAHL_DOWNLOADS_KOSTENFREI =
        "select verwendungskey, count(*) from billing_billing where ${productKeys} and date_trunc('day',abrechnungsdatum) >= '${from}' and date_trunc('day',abrechnungsdatum) <= '${till}' and storniert is null and username not like 'NICHT-ZAEHLEN%' and (netto_summe =0 or netto_summe is null) group by verwendungskey;";
    private static final String QUERY_SUMME_PRO_VERWENDUNGSZWECK =
        "select verwendungskey, sum(netto_summe) from billing_billing where ${productKeys} and date_trunc('day',abrechnungsdatum) >= '${from}' and date_trunc('day',abrechnungsdatum) <= '${till}' and storniert is null and username not like 'NICHT-ZAEHLEN%' and not (netto_summe =0 or netto_summe is null) group by verwendungskey;";

    private static final String QUERY_ANZAHL_PRODUKTE_VERMESSUNGSUNTERLAGEN_TS3 =
        "select produktbezeichnung,count(id) from ( "
                + "select "
                + "        produktbezeichnung, id "
                + "from billing_billing "
                + "where "
                + "        verwendungszweck = 'Vermessungsunterlagen (amtlicher Lageplan TS 3)' "
                + "        and date_trunc('day',ts) >= '${from}' and date_trunc('day',ts) <= '${till}' "
                + "        and storniert is null "
                + "        and username not like 'NICHT-ZAEHLEN%' "
                + "group by produktbezeichnung, id "
                + "order by produktbezeichnung) as temptable "
                + "group by produktbezeichnung;";
    private static final String QUERY_ANZAHL_PRODUKTE_VERMESSUNGSUNTERLAGEN_TS4 =
        "select produktbezeichnung,count(id) from ( "
                + "select "
                + "        produktbezeichnung, id "
                + "from billing_billing "
                + "where "
                + "        verwendungszweck = 'Vermessungsunterlagen (hoheitliche Vermessung TS 4)' "
                + "        and date_trunc('day',ts) >= '${from}' and date_trunc('day',ts) <= '${till}' "
                + "        and storniert is null "
                + "        and username not like 'NICHT-ZAEHLEN%' "
                + "group by produktbezeichnung, id "
                + "order by produktbezeichnung) as temptable "
                + "group by produktbezeichnung;";

    //~ Instance fields --------------------------------------------------------

    private final Date from;
    private final Date till;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GeschaeftsberichtBranchenAmounts object.
     *
     * @param  billingBeanIds  timestampEnd DOCUMENT ME!
     * @param  from            year DOCUMENT ME!
     * @param  till            DOCUMENT ME!
     */
    public BillingJahresberichtReportServerSearch(final String billingBeanIds, final Date from, final Date till) {
        super(billingBeanIds);
        this.from = from;
        this.till = till;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
        if (ms != null) {
            try {
                final HashMap<String, ArrayList> results = new HashMap<>();

                excuteQueryAndConvertAmountResults(
                    ms,
                    results,
                    QUERY_KUNDEN_ABRECHNUNG_WIEDERVERKAUEFER,
                    KUNDEN_ABRECHNUNG_WIEDERVERKAEUFER);
                excuteQueryAndConvertAmountResults(ms, results, QUERY_ANZAHL_KUNDEN_PER_GRUPPE, ANZAHL_KUNDEN);

                // QUERY_ANZAHL_GESCHAEFTSBUCHNUMMER_KOSTENPLICHTIG
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_ANZAHL_GESCHAEFTSBUCHNUMMER_KOSTENPLICHTIG,
                    Arrays.asList(PRODUCTS_LK),
                    VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENPFLICHTIG_LK);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_ANZAHL_GESCHAEFTSBUCHNUMMER_KOSTENPLICHTIG,
                    Arrays.asList(PRODUCTS_BL),
                    VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENPFLICHTIG_BL);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_ANZAHL_GESCHAEFTSBUCHNUMMER_KOSTENPLICHTIG,
                    Arrays.asList(PRODUCTS_KO),
                    VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENPFLICHTIG_KO);

                // QUERY_ANZAHL_GESCHAEFTSBUCHNUMMER_KOSTENFREI
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_ANZAHL_GESCHAEFTSBUCHNUMMER_KOSTENFREI,
                    Arrays.asList(PRODUCTS_LK),
                    VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENFREI_LK);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_ANZAHL_GESCHAEFTSBUCHNUMMER_KOSTENFREI,
                    Arrays.asList(PRODUCTS_BL),
                    VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENFREI_BL);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_ANZAHL_GESCHAEFTSBUCHNUMMER_KOSTENFREI,
                    Arrays.asList(PRODUCTS_KO),
                    VERWENDUNGSZWECK_GESCHAEFTSBUCHNUMMERN_KOSTENFREI_KO);

                // QUERY_ANZAHL_DOWNLOADS_KOSTENPFLICHTIG
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_ANZAHL_DOWNLOADS_KOSTENPFLICHTIG,
                    Arrays.asList(PRODUCTS_LK),
                    VERWENDUNGSZWECK_DOWNLOADS_KOSTENPFLICHTIG_LK);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_ANZAHL_DOWNLOADS_KOSTENFREI,
                    Arrays.asList(PRODUCTS_LK),
                    VERWENDUNGSZWECK_DOWNLOADS_KOSTENFREI_LK);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_ANZAHL_DOWNLOADS_KOSTENPFLICHTIG,
                    Arrays.asList(PRODUCTS_BL),
                    VERWENDUNGSZWECK_DOWNLOADS_KOSTENPFLICHTIG_BL);

                // QUERY_ANZAHL_DOWNLOADS_KOSTENFREI
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_ANZAHL_DOWNLOADS_KOSTENFREI,
                    Arrays.asList(PRODUCTS_BL),
                    VERWENDUNGSZWECK_DOWNLOADS_KOSTENFREI_BL);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_ANZAHL_DOWNLOADS_KOSTENPFLICHTIG,
                    Arrays.asList(PRODUCTS_KO),
                    VERWENDUNGSZWECK_DOWNLOADS_KOSTENPFLICHTIG_KO);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_ANZAHL_DOWNLOADS_KOSTENFREI,
                    Arrays.asList(PRODUCTS_KO),
                    VERWENDUNGSZWECK_DOWNLOADS_KOSTENFREI_KO);

                // QUERY_SUMME_PRO_VERWENDUNGSZWECK
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_SUMME_PRO_VERWENDUNGSZWECK,
                    Arrays.asList(PRODUCTS_LK),
                    VERWENDUNGSZWECK_SUMME_EINNAHMEN_LK);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_SUMME_PRO_VERWENDUNGSZWECK,
                    Arrays.asList(PRODUCTS_BL),
                    VERWENDUNGSZWECK_SUMME_EINNAHMEN_BL);
                excuteQueryAndConvertAmountPerVerwendungszweckResults(
                    ms,
                    results,
                    QUERY_SUMME_PRO_VERWENDUNGSZWECK,
                    Arrays.asList(PRODUCTS_KO),
                    VERWENDUNGSZWECK_SUMME_EINNAHMEN_KO);

                // QUERY_ANZAHL_PRODUKTE_VERMESSUNGSUNTERLAGEN_TS*
                excuteQueryAndConvertAmountResults(
                    ms,
                    results,
                    QUERY_ANZAHL_PRODUKTE_VERMESSUNGSUNTERLAGEN_TS3,
                    ANZAHL_VERMESSUNGSUNTERLAGEN_TS3);
                excuteQueryAndConvertAmountResults(
                    ms,
                    results,
                    QUERY_ANZAHL_PRODUKTE_VERMESSUNGSUNTERLAGEN_TS4,
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
        final ArrayList<ArrayList> lists = ms.performCustomSearch(query.replace(
                    "${from}",
                    POSTGRES_DATE_FORMAT.format(from)).replace("${till}", POSTGRES_DATE_FORMAT.format(till)),
                getConnectionContext());
        if ((lists != null) && !lists.isEmpty()) {
            final ArrayList<AmountBean> beans = new ArrayList<>();
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
     * @param   productKeys  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String generateProductKeysPart(final Collection<String> productKeys) {
        if ((productKeys != null) && !productKeys.isEmpty()) {
            final Collection<String> quotedProductKeys = new ArrayList<>(productKeys.size());
            for (final String productKey : productKeys) {
                quotedProductKeys.add("'" + productKey + "'");
            }
            return String.format("produktkey in (%s)", String.join(",", quotedProductKeys));
        } else {
            return "true";
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   ms           DOCUMENT ME!
     * @param   results      DOCUMENT ME!
     * @param   query        DOCUMENT ME!
     * @param   productKeys  DOCUMENT ME!
     * @param   key          DOCUMENT ME!
     *
     * @throws  RemoteException  DOCUMENT ME!
     */
    private void excuteQueryAndConvertAmountPerVerwendungszweckResults(final MetaService ms,
            final HashMap<String, ArrayList> results,
            final String query,
            final Collection<String> productKeys,
            final String key) throws RemoteException {
        final ArrayList<ArrayList> lists = ms.performCustomSearch(query.replace(
                    "${productKeys}",
                    generateProductKeysPart(productKeys)).replace("${from}", POSTGRES_DATE_FORMAT.format(from)).replace(
                    "${till}",
                    POSTGRES_DATE_FORMAT.format(till)),
                getConnectionContext());
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
                } else if ("GDZ".equalsIgnoreCase(name)) {
                    bean.setNumberGDZ(number);
                }
            }
            final ArrayList<AnzahlProVerwendungszweckBean> beans = new ArrayList<>();
            beans.add(bean);
            results.put(key, beans);
        }
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return ConnectionContext.createDummy();
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
        private Number numberGDZ = (long)0;
    }
}
