/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import de.cismet.cids.custom.tostringconverter.wunda_blau.BillingBillingToStringConverter;

import org.apache.log4j.Logger;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
public class CidsBillingSearchStatement extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(CidsBillingSearchStatement.class);
    private static final String CIDSCLASS = "billing";
    private static final String DOMAIN = "WUNDA_BLAU";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Kostentyp {

        //~ Enum constants -----------------------------------------------------

        KOSTENPFLICHTIG, KOSTENFREI, IGNORIEREN
    }

    //~ Instance fields --------------------------------------------------------

    private String geschaeftsbuchnummer;
    private String projekt;
    private String userID;
    private String abrechnungsturnusID;
    private ArrayList<String> verwendungszweckKeys = new ArrayList<String>();
    private Kostentyp kostentyp = Kostentyp.IGNORIEREN;
    private Date from;
    private Date till;
    private Date abrechnungsdatumFrom;
    private Date abrechnungsdatumTill;
    private StringBuilder query;
    private final SimpleDateFormat postgresDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private ArrayList<MetaObject> kundeMetaObjects = new ArrayList<MetaObject>();
    private String kundenname;

    private Boolean showStornierteBillings = false;

    /**
     * <ul>
     *   <li>True: show only Abgerechnete Billings</li>
     *   <li>False: hide Abgerechnete Billings</li>
     *   <li>Null: do not consider the abgerechnet-field in the where condition</li>
     * </ul>
     */
    private Boolean showAbgerechneteBillings = false;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsBillingSearchStatement object.
     */
    public CidsBillingSearchStatement() {
    }

    /**
     * Creates a new CidsBillingSearchStatement object.
     *
     * @param  kundeMetaObject  kundeBean DOCUMENT ME!
     */
    public CidsBillingSearchStatement(final MetaObject kundeMetaObject) {
        this.kundeMetaObjects.add(kundeMetaObject);
    }

    /**
     * Creates a new CidsBillingSearchStatement object.
     *
     * @param  kundeMetaObjects  DOCUMENT ME!
     */
    public CidsBillingSearchStatement(final ArrayList<MetaObject> kundeMetaObjects) {
        this.kundeMetaObjects = kundeMetaObjects;
    }

    /**
     * Creates a new CidsBillingSearchStatement object.
     *
     * @param  kundenname  DOCUMENT ME!
     */
    public CidsBillingSearchStatement(final String kundenname) {
        this.kundenname = kundenname;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection<MetaObjectNode> performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get(DOMAIN);
        if (ms != null) {
            try {
                final List<MetaObjectNode> result = new ArrayList<MetaObjectNode>();

                generateQuery();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The used query is: " + query.toString());
                }

                final ArrayList<ArrayList> searchResult = ms.performCustomSearch(query.toString());
                for (final ArrayList al : searchResult) {
                    final int cid = (Integer)al.get(0);
                    final int oid = (Integer)al.get(1);
                    final String geschaeftsbuchnummer = (String)al.get(2);
                    final String kundenname = (String)al.get(3);
                    final String username = (String)al.get(4);
                    final Date angelegt = (Date)al.get(5);
                    final String name = BillingBillingToStringConverter.createString(geschaeftsbuchnummer, kundenname, username, angelegt);
                    final MetaObjectNode mon = new MetaObjectNode(DOMAIN, oid, cid, name, null, null);
                    result.add(mon);
                }

                return result;
            } catch (RemoteException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        } else {
            LOG.error("active local server not found"); // NOI18N
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Date getAbrechnungsdatumFrom() {
        return abrechnungsdatumFrom;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  abrechnungsdatumFrom  DOCUMENT ME!
     */
    public void setAbrechnungsdatumFrom(final Date abrechnungsdatumFrom) {
        this.abrechnungsdatumFrom = abrechnungsdatumFrom;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Date getAbrechnungsdatumTill() {
        return abrechnungsdatumTill;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  abrechnungsdatumTill  DOCUMENT ME!
     */
    public void setAbrechnungsdatumTill(final Date abrechnungsdatumTill) {
        this.abrechnungsdatumTill = abrechnungsdatumTill;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String generateQuery() {
        query = new StringBuilder();
        query.append("SELECT " + "(SELECT id "
                    + "                FROM    cs_class "
                    + "                WHERE   name ilike '" + CIDSCLASS + "' "
                    + "                ), b.id, b.geschaeftsbuchnummer, kunde.name, b.username, b.ts ");
        query.append(" FROM billing_billing b");
        query.append(" JOIN billing_kunden_logins as logins");
        query.append("     ON b.angelegt_durch = logins.id");
        query.append(" JOIN billing_kunde as kunde");
        query.append("     ON logins.kunde = kunde.id");
        query.append(" WHERE ");
        appendKunde();
        appendUserIds();
        appendGeschaeftsbuchnummer();
        appendProjekt();
        appendVerwendungszweckKeys();
        appendKostentyp();
        appendDates();
        appendAbrechnungsturnus();
        appendStornoAndAbgerechnet();

        return query.toString();
    }

    /**
     * DOCUMENT ME!
     */
    private void appendKunde() {
        if (kundenname == null) {
            if (kundeMetaObjects.isEmpty()) {
                query.append(" true ");
            } else {
                // create the following structure: (id_1, id_2, ... ,  id_n)
                final StringBuilder customerListString = new StringBuilder(" kunde.id in (");
                for (final MetaObject kundeMetaObject : kundeMetaObjects) {
                    customerListString.append(kundeMetaObject.getBean().getProperty("id"));
                    customerListString.append(",");
                }
                // remove last comma
                customerListString.deleteCharAt(customerListString.length() - 1);
                customerListString.append(")");
                query.append(customerListString.toString());
            }
        } else {
            query.append(" kunde.name ilike '%").append(kundenname).append("%' ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendUserIds() {
        if ((userID != null) && !userID.equals("")) {
            // filter only for one userID
            query.append("and angelegt_durch  = ").append(userID).append(" ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendGeschaeftsbuchnummer() {
        if ((geschaeftsbuchnummer != null) && !geschaeftsbuchnummer.equals("")) {
            query.append("and geschaeftsbuchnummer ilike '%").append(geschaeftsbuchnummer).append("%' ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendProjekt() {
        if ((projekt != null) && !projekt.equals("")) {
            query.append("and projektbezeichnung ilike '%").append(projekt).append("%' ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendVerwendungszweckKeys() {
        if (!verwendungszweckKeys.isEmpty()) {
            final StringBuilder verwendungszweckListString = new StringBuilder("(");
            for (final String verwendungszweckKey : verwendungszweckKeys) {
                verwendungszweckListString.append(" '");
                verwendungszweckListString.append(verwendungszweckKey);
                verwendungszweckListString.append("',");
            }
            // remove last comma
            verwendungszweckListString.deleteCharAt(verwendungszweckListString.length() - 1);
            verwendungszweckListString.append(")");
            query.append("and verwendungskey in ").append(verwendungszweckListString.toString()).append(" ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendKostentyp() {
        switch (kostentyp) {
            case KOSTENFREI: {
                query.append("and netto_summe = 0 ");
                break;
            }
            case KOSTENPFLICHTIG: {
                query.append("and netto_summe > 0 ");
                break;
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendDates() {
        if (from == null) {
            // do nothing, time filters are ignored
        } else if ((till == null) || postgresDateFormat.format(from).equals(postgresDateFormat.format(till))) { // check if there is a second date or if they are the same day
            query.append(" and date_trunc('day',ts) = '");
            query.append(postgresDateFormat.format(from));
            query.append("' ");
        } else {                                                                                                // create query for a time period
            query.append(" and date_trunc('day',ts) >= '");
            query.append(postgresDateFormat.format(from));
            query.append("' ");
            query.append(" and date_trunc('day',ts) <= '");
            query.append(postgresDateFormat.format(till));
            query.append("' ");
        }

        if (abrechnungsdatumFrom == null) {
            // do nothing, time filters are ignored
        } else if ((abrechnungsdatumTill == null)
                    || postgresDateFormat.format(abrechnungsdatumFrom).equals(
                        postgresDateFormat.format(abrechnungsdatumTill))) { // check if there is a second date or if
                                                                            // they are the same day
            query.append(" and date_trunc('day',abrechnungsdatum) = '");
            query.append(postgresDateFormat.format(abrechnungsdatumFrom));
            query.append("' ");
        } else {                                                            // create query for a time period
            query.append(" and date_trunc('day',abrechnungsdatum) >= '");
            query.append(postgresDateFormat.format(abrechnungsdatumFrom));
            query.append("' ");
            query.append(" and date_trunc('day',abrechnungsdatum) <= '");
            query.append(postgresDateFormat.format(abrechnungsdatumTill));
            query.append("' ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendAbrechnungsturnus() {
        if ((abrechnungsturnusID != null) && !abrechnungsturnusID.equals("")) {
            query.append(" and kunde.abrechnungsturnus = ").append(abrechnungsturnusID).append(" ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendStornoAndAbgerechnet() {
        if (Boolean.TRUE.equals(showStornierteBillings)) {
            query.append(" and b.storniert is true ");
        } else if (Boolean.FALSE.equals(showStornierteBillings)) {
            query.append(" and b.storniert is not true ");
        }
        if (Boolean.TRUE.equals(showAbgerechneteBillings)) {
            query.append(" and b.abgerechnet is true ");
        } else if (Boolean.FALSE.equals(showAbgerechneteBillings)) { // hide abgerechnete billings
            query.append(" and b.abgerechnet is not true ");
        }                                                            // else - do nothing - to ignore the field
                                                                     // abgerechnet
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getGeschaeftsbuchnummer() {
        return geschaeftsbuchnummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  geschaeftsbuchnummer  DOCUMENT ME!
     */
    public void setGeschaeftsbuchnummer(final String geschaeftsbuchnummer) {
        this.geschaeftsbuchnummer = geschaeftsbuchnummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ArrayList<MetaObject> getKundeMetaObjects() {
        return kundeMetaObjects;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  kundeMetaObjects  kundeBean DOCUMENT ME!
     */
    public void setKundeMetaObjects(final ArrayList<MetaObject> kundeMetaObjects) {
        this.kundeMetaObjects = kundeMetaObjects;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getProjekt() {
        return projekt;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  projekt  DOCUMENT ME!
     */
    public void setProjekt(final String projekt) {
        this.projekt = projekt;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getUserID() {
        return userID;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userID  DOCUMENT ME!
     */
    public void setUserID(final String userID) {
        this.userID = userID;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ArrayList<String> getVerwendungszweckKeys() {
        return verwendungszweckKeys;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  verwendungszweckKeys  DOCUMENT ME!
     */
    public void setVerwendungszweckKeys(final ArrayList<String> verwendungszweckKeys) {
        this.verwendungszweckKeys = verwendungszweckKeys;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Kostentyp getKostentyp() {
        return kostentyp;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  kostentyp  DOCUMENT ME!
     */
    public void setKostentyp(final Kostentyp kostentyp) {
        this.kostentyp = kostentyp;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Date getFrom() {
        return from;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  from  DOCUMENT ME!
     */
    public void setFrom(final Date from) {
        this.from = from;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Date getTill() {
        return till;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  till  DOCUMENT ME!
     */
    public void setTill(final Date till) {
        this.till = till;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getAbrechnungsturnusID() {
        return abrechnungsturnusID;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  abrechnungsturnusID  DOCUMENT ME!
     */
    public void setAbrechnungsturnusID(final String abrechnungsturnusID) {
        this.abrechnungsturnusID = abrechnungsturnusID;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    @Deprecated
    public boolean isShowOnlyStornierteBillings() {
        return Boolean.TRUE.equals(isShowStornierteBillings());
    }
    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Boolean isShowStornierteBillings() {
        return showStornierteBillings;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  showOnlyStornierteBillings  DOCUMENT ME!
     */
    public void setShowOnlyStornierteBillings(final boolean showOnlyStornierteBillings) {
        setShowStornierteBillings(showOnlyStornierteBillings);
    }
    /**
     * DOCUMENT ME!
     *
     * @param  showStornierteBillings  DOCUMENT ME!
     */
    public void setShowStornierteBillings(final Boolean showStornierteBillings) {
        this.showStornierteBillings = showStornierteBillings;
    }

    /**
     * <ul>
     *   <li>True: show only Abgerechnete Billings</li>
     *   <li>False: hide Abgerechnete Billings</li>
     *   <li>Null: do not consider the abgerechnet-field in the where condition</li>
     * </ul>
     *
     * @return  DOCUMENT ME!
     */
    public Boolean isShowAbgerechneteBillings() {
        return showAbgerechneteBillings;
    }

    /**
     * <ul>
     *   <li>True: show only Abgerechnete Billings</li>
     *   <li>False: hide Abgerechnete Billings</li>
     *   <li>Null: do not consider the abgerechnet-field in the where condition</li>
     * </ul>
     *
     * @param  showAbgerechneteBillings  DOCUMENT ME!
     */
    public void setShowAbgerechneteBillings(final Boolean showAbgerechneteBillings) {
        this.showAbgerechneteBillings = showAbgerechneteBillings;
    }
}
