/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.utils.billing;

import Sirius.server.MetaClassCache;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.newuser.User;

import java.rmi.RemoteException;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class BillingUtils {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(BillingUtils.class);

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   projektBezeichnung    DOCUMENT ME!
     * @param   geschaeftsbuchNummer  DOCUMENT ME!
     * @param   request               DOCUMENT ME!
     * @param   billingBerechnung     DOCUMENT ME!
     * @param   billingModus          DOCUMENT ME!
     * @param   billingUsage          DOCUMENT ME!
     * @param   billingProduct        DOCUMENT ME!
     * @param   billingPrice          DOCUMENT ME!
     * @param   user                  DOCUMENT ME!
     * @param   metaService           DOCUMENT ME!
     * @param   connectionContext     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public CidsBean createBilling(final String projektBezeichnung,
            final String geschaeftsbuchNummer,
            final String request,
            final String billingBerechnung,
            final BillingModus billingModus,
            final BillingUsage billingUsage,
            final BillingProduct billingProduct,
            final BillingPrice billingPrice,
            final User user,
            final MetaService metaService,
            final ConnectionContext connectionContext) throws Exception {
        final CidsBean cb = CidsBean.createNewCidsBeanFromTableName(
                "WUNDA_BLAU",
                "Billing_Billing",
                connectionContext);
        cb.setProperty("username", user.toString());
        cb.setProperty("angelegt_durch", getExternalUser(metaService, user, connectionContext));
        cb.setProperty("ts", new java.sql.Timestamp(System.currentTimeMillis()));
        cb.setProperty("angeschaeftsbuch", Boolean.FALSE);
        cb.setProperty("modus", billingModus.getKey());
        cb.setProperty("produktkey", billingProduct.getId());
        cb.setProperty("produktbezeichnung", billingProduct.getName());
        cb.setProperty("berechnung", billingBerechnung);
        cb.setProperty("netto_summe", (billingPrice != null) ? billingPrice.getNetto() : null);
        cb.setProperty("mwst_satz", billingProduct.getMwst());
        cb.setProperty("brutto_summe", (billingPrice != null) ? billingPrice.getBrutto() : null);
        cb.setProperty("geschaeftsbuchnummer", geschaeftsbuchNummer);
        cb.setProperty("modusbezeichnung", billingModus.getName());
        cb.setProperty("verwendungszweck", billingUsage.getName());
        cb.setProperty("verwendungskey", billingUsage.getKey());
        cb.setProperty("projektbezeichnung", projektBezeichnung);
        cb.setProperty("request", request);
        cb.setProperty("abgerechnet", Boolean.FALSE);
        return cb;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   tableName          DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public MetaClass getMetaClass(final String tableName, final ConnectionContext connectionContext) {
        return MetaClassCache.getInstance().getMetaClass("WUNDA_BLAU", "billing_kunden_logins");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   query              DOCUMENT ME!
     * @param   metaService        DOCUMENT ME!
     * @param   user               DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public MetaObject[] getMetaObjects(final String query,
            final MetaService metaService,
            final User user,
            final ConnectionContext connectionContext) throws Exception {
        return metaService.getMetaObject(user, query, connectionContext);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   metaService        DOCUMENT ME!
     * @param   user               DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public CidsBean getExternalUser(final MetaService metaService,
            final User user,
            final ConnectionContext connectionContext) throws Exception {
        return getExternalUser(user.getName(), metaService, user, connectionContext);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   loginName          DOCUMENT ME!
     * @param   metaService        DOCUMENT ME!
     * @param   user               DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public CidsBean getExternalUser(final String loginName,
            final MetaService metaService,
            final User user,
            final ConnectionContext connectionContext) throws Exception {
        final MetaClass metaClass = getMetaClass("billing_kunden_logins", connectionContext);
        if (metaClass == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                    "The metaclass for billing_kunden_logins is null. The current user has probably not the needed rights.");
            }
            return null;
        }

        final String query = String.format(
                "SELECT %d, %s FROM %s WHERE name = '%s'",
                metaClass.getID(),
                metaClass.getPrimaryKey(),
                metaClass.getTableName(),
                loginName);
        CidsBean externalUser = null;
        try {
            final MetaObject[] metaObjects = getMetaObjects(query, metaService, user, connectionContext);
            if ((metaObjects != null) && (metaObjects.length > 0)) {
                externalUser = metaObjects[0].getBean();
            }
        } catch (final RemoteException ex) {
            LOG.error("Error while retrieving the CidsBean of an external user.", ex);
        }
        return externalUser;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BillingUtils getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final BillingUtils INSTANCE = new BillingUtils();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
