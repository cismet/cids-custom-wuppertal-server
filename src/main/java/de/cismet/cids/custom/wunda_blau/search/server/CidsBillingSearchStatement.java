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
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
public class CidsBillingSearchStatement extends AbstractCidsServerSearch {

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
    public enum Kostenart {

        //~ Enum constants -----------------------------------------------------

        KOSTENPFLICHTIG, KOSTENFREI, IGNORIEREN
    }

    //~ Instance fields --------------------------------------------------------

    private String geschaeftsbuchnummer;
    private String projekt;
    private String userID;
    private ArrayList<String> verwendungszweckKeys = new ArrayList<String>();
    private Kostenart kostenart = Kostenart.IGNORIEREN;
    private Date from = new Date();
    private Date till;
    private StringBuilder query;
    private SimpleDateFormat postgresDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private User user;
    private ArrayList<MetaObject> kundeMetaObjects = new ArrayList<MetaObject>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsBillingSearchStatement object.
     *
     * @param  user             DOCUMENT ME!
     * @param  kundeMetaObject  kundeBean DOCUMENT ME!
     */
    public CidsBillingSearchStatement(final User user, final MetaObject kundeMetaObject) {
        this.user = user;
        this.kundeMetaObjects.add(kundeMetaObject);
    }

    /**
     * Creates a new CidsBillingSearchStatement object.
     *
     * @param  user              DOCUMENT ME!
     * @param  kundeMetaObjects  DOCUMENT ME!
     */
    public CidsBillingSearchStatement(final User user, final ArrayList<MetaObject> kundeMetaObjects) {
        this.user = user;
        this.kundeMetaObjects = kundeMetaObjects;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection<MetaObject> performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get(DOMAIN);
        if (ms != null) {
            try {
                LOG.warn(query.toString());
                final MetaObject[] billingMetaObjects = ms.getMetaObject(user, query.toString());
                final ArrayList<MetaObject> billingCollection = new ArrayList<MetaObject>(Arrays.asList(
                            billingMetaObjects));
                return billingCollection;
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
    public String generateQuery() {
        query = new StringBuilder();
        query.append("SELECT " + "(SELECT id "
                    + "                FROM    cs_class "
                    + "                WHERE   name ilike 'billing' "
                    + "                ), b.id ");
        query.append(" FROM billing_billing b");
        query.append(" WHERE ");
        appendUserIds();
        appendGeschaeftsbuchnummer();
        appendProjekt();
        appendVerwendungszweckKeys();
        appendKostenart();
        appendDates();

        return query.toString();
    }

    /**
     * DOCUMENT ME!
     */
    private void appendUserIds() {
        if ((userID == null) || userID.equals("")) {
            final List<CidsBean> benutzerBeans = new ArrayList<CidsBean>();
            for (final MetaObject kundeMetaObject : kundeMetaObjects) {
                benutzerBeans.addAll(kundeMetaObject.getBean().getBeanCollectionProperty("benutzer"));
            }
            LOG.fatal("Benutzerbeans: " + benutzerBeans.size());
            if (!benutzerBeans.isEmpty()) {
                // create the following structure: (id_1, id_2, ... ,  id_n)
                final StringBuilder userListString = new StringBuilder(" angelegt_durch in (");
                for (final CidsBean benutzer : benutzerBeans) {
                    userListString.append(benutzer.getProperty("id"));
                    userListString.append(",");
                }
                // remove last comma
                userListString.deleteCharAt(userListString.length() - 1);
                userListString.append(")");
                query.append(userListString.toString());
            } else {
                query.append(" false ");
                LOG.error("This customer has no users, that should not happen.");
            }
        } else { // filter only for one userID
            query.append(" angelegt_durch  = " + userID + " ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendGeschaeftsbuchnummer() {
        if ((geschaeftsbuchnummer != null) && !geschaeftsbuchnummer.equals("")) {
            query.append("and geschaeftsbuchnummer ilike '%" + geschaeftsbuchnummer + "%' ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendProjekt() {
        if ((projekt != null) && !projekt.equals("")) {
            query.append("and projektbezeichnung ilike '%" + projekt + "%' ");
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
            query.append("and verwendungskey in " + verwendungszweckListString.toString() + " ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendKostenart() {
        switch (kostenart) {
            case KOSTENFREI: {
                query.append("and gebuehrenpflichtig is false");
                break;
            }
            case KOSTENPFLICHTIG: {
                query.append("and gebuehrenpflichtig is true");
                break;
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendDates() {
        // check if there is a second date or if they are the same day
        if ((till == null) || postgresDateFormat.format(from).equals(postgresDateFormat.format(till))) {
            query.append(" and date_trunc('day',ts) = '");
            query.append(postgresDateFormat.format(from));
            query.append("' ");
        } else { // create query for a time period
            query.append(" and date_trunc('day',ts) >= '");
            query.append(postgresDateFormat.format(from));
            query.append("' ");
            query.append(" and date_trunc('day',ts) <= '");
            query.append(postgresDateFormat.format(till));
            query.append("' ");
        }
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
    public Kostenart getKostenart() {
        return kostenart;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  kostenart  DOCUMENT ME!
     */
    public void setKostenart(final Kostenart kostenart) {
        this.kostenart = kostenart;
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
}
