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
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.newuser.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungBillingDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungHandler;
import de.cismet.cids.custom.utils.billing.BillingModus;
import de.cismet.cids.custom.utils.billing.BillingPrice;
import de.cismet.cids.custom.utils.billing.BillingProduct;
import de.cismet.cids.custom.utils.billing.BillingUsage;
import de.cismet.cids.custom.utils.billing.BillingUtils;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class BerechtigungspruefungAnfrageServerAction implements UserAwareServerAction,
    MetaServiceStore,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungAnfrageServerAction.class);

    public static final String TASK_NAME = "berechtigungspruefungAnfrage";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ParameterType {

        //~ Enum constants -----------------------------------------------------

        ABGEHOLT, DATEINAME, BEGRUENDUNG, BERECHTIGUNGSGRUND, DOWNLOADINFO_JSON, BILLING_BERECHNUNG, BILLING_USAGE,
        BILLING_PRODUCT, BILLING_MODUS, BILLING_PRICE
    }

    //~ Instance fields --------------------------------------------------------

    private User user = null;
    private MetaService metaService = null;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            String dateiname = null;
            String begruendung = null;
            String downloadinfoJson = null;
            String berechtigungsgrund = null;
            String abgeholt = null;
            String billingBerechnung = null;
            BillingPrice billingPrice = null;
            BillingModus billingModus = null;
            BillingProduct billingProduct = null;
            BillingUsage billingUsage = null;
            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(ParameterType.DATEINAME.toString())) {
                        dateiname = (String)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.BEGRUENDUNG.toString())) {
                        begruendung = (String)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.BERECHTIGUNGSGRUND.toString())) {
                        berechtigungsgrund = (String)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.DOWNLOADINFO_JSON.toString())) {
                        downloadinfoJson = (String)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.ABGEHOLT.toString())) {
                        abgeholt = (String)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.BILLING_BERECHNUNG.toString())) {
                        billingBerechnung = (String)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.BILLING_PRICE.toString())) {
                        billingPrice = (BillingPrice)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.BILLING_USAGE.toString())) {
                        billingUsage = (BillingUsage)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.BILLING_PRODUCT.toString())) {
                        billingProduct = (BillingProduct)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.BILLING_MODUS.toString())) {
                        billingModus = (BillingModus)sap.getValue();
                    }
                }
            }

            if (abgeholt != null) {
                BerechtigungspruefungHandler.getInstance().setMetaService(getMetaService());
                BerechtigungspruefungHandler.getInstance().closeAnfrage(abgeholt);
                return null;
            } else {
                final byte[] data = (byte[])body;
                if ((body != null) && !(body instanceof byte[])) {
                    throw new IllegalArgumentException("body has to be a byte array");
                }

                if (downloadinfoJson == null) {
                    throw new IllegalArgumentException("aufruf enthält keine downloadinfo");
                }

                final BerechtigungspruefungDownloadInfo downloadInfo = BerechtigungspruefungHandler.extractDownloadInfo(
                        downloadinfoJson);

                BerechtigungspruefungHandler.getInstance().setMetaService(getMetaService());

                final String schluessel = BerechtigungspruefungHandler.getInstance().createNewSchluessel(downloadInfo);
                final String userKey = (String)user.getKey();
                final CidsBean anfrageBean = BerechtigungspruefungHandler.getInstance()
                            .addNewAnfrage(
                                userKey,
                                schluessel,
                                downloadInfo,
                                berechtigungsgrund,
                                begruendung,
                                dateiname,
                                data);

                if (downloadInfo instanceof BerechtigungspruefungBillingDownloadInfo) {
                    final BerechtigungspruefungBillingDownloadInfo billingDownloadInfo =
                        (BerechtigungspruefungBillingDownloadInfo)downloadInfo;

                    billingDownloadInfo.setAuftragsnummer(schluessel);

                    final CidsBean cb = BillingUtils.getInstance()
                                .createBilling(
                                    billingDownloadInfo.getProduktbezeichnung(),
                                    schluessel,
                                    "no.yet",
                                    billingBerechnung,
                                    billingModus,
                                    billingUsage,
                                    billingProduct,
                                    billingPrice,
                                    getUser(),
                                    getMetaService(),
                                    getConnectionContext());

                    final CidsBean persistedBillingBean = DomainServerImpl.getServerInstance()
                                .insertMetaObject(BerechtigungspruefungHandler.getInstance().getUser(),
                                        cb.getMetaObject(),
                                        getConnectionContext())
                                .getBean();

                    billingDownloadInfo.setBillingId(persistedBillingBean.getPrimaryKeyValue());
                    anfrageBean.setProperty(
                        "downloadinfo_json",
                        new ObjectMapper().writeValueAsString(billingDownloadInfo));
                    DomainServerImpl.getServerInstance()
                            .updateMetaObject(BerechtigungspruefungHandler.getInstance().getUser(),
                                anfrageBean.getMetaObject(),
                                getConnectionContext());
                }

                return schluessel;
            }
        } catch (final Exception ex) {
            LOG.error("error while executing anfrage task", ex);
            return ex;
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }

    @Override
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
