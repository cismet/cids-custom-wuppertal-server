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

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import java.io.StringReader;

import java.net.URL;

import java.util.Date;

import de.cismet.cids.custom.utils.ServerStamperUtils;
import de.cismet.cids.custom.utils.StamperUtils;
import de.cismet.cids.custom.utils.alkis.AlkisProducts;
import de.cismet.cids.custom.utils.alkis.AlkisRestConf;
import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionHelper;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UploadableInputStream;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.commons.security.AccessHandler;
import de.cismet.commons.security.exceptions.BadHttpStatusCodeException;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class AlkisProductServerAction implements ConnectionContextStore, UserAwareServerAction, MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    public static final Logger LOG = Logger.getLogger(AlkisProductServerAction.class);
    public static final String TASK_NAME = "alkisProduct";
    private static final String MASK_REPLACEMENT = "***";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Body {

        //~ Enum constants -----------------------------------------------------

        KARTE, KARTE_CUSTOM, EINZELNACHWEIS, EINZELNACHWEIS_STICHTAG, LISTENNACHWEIS
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        ALKIS_CODE, PRODUKT, FERTIGUNGSVERMERK, STICHTAG, X, Y, MASSSTAB, MASSSTAB_MIN, MASSSTAB_MAX, WINKEL, ZUSATZ,
        AUFTRAGSNUMMER
    }

    //~ Instance fields --------------------------------------------------------

    private User user;
    private MetaService metaService;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

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
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Object execute(Object body, final ServerActionParameter... params) {
        try {
            String produkt = null;
            String alkisCode = null;
            String fertigungsVermerk = null;
            Date stichtag = null;
            Integer winkel = null;
            Integer x = null;
            Integer y = null;
            String massstab = null;
            String massstabMin = null;
            String massstabMax = null;
            String zusatz = null;
            String auftragsnummer = null;

            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(AlkisProductServerAction.Parameter.PRODUKT.toString())) {
                        produkt = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.ALKIS_CODE.toString())) {
                        alkisCode = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.FERTIGUNGSVERMERK.toString())) {
                        fertigungsVermerk = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.STICHTAG.toString())) {
                        if (sap.getValue() instanceof String) {
                            DateFormat df = new SimpleDateFormat("d.M.yyyy");
                            stichtag = df.parse((String)sap.getValue());
                        } else {
                            stichtag = (Date)sap.getValue();
                        }
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.WINKEL.toString())) {
                        winkel = (Integer)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.X.toString())) {
                        x = (Integer)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.Y.toString())) {
                        y = (Integer)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.MASSSTAB.toString())) {
                        massstab = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.MASSSTAB_MIN.toString())) {
                        massstabMin = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.MASSSTAB_MAX.toString())) {
                        massstabMax = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.ZUSATZ.toString())) {
                        zusatz = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.AUFTRAGSNUMMER.toString())) {
                        auftragsnummer = (String)sap.getValue();
                    }
                }
            }

            final URL url;

            if (body instanceof byte[]) {
                String bodyString = new String((byte[])body);
                bodyString = bodyString.trim();

                try {
                    final Body retVal = Body.valueOf(bodyString);

                    if (retVal != null) {
                        body = retVal;
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        "Body has to be either KARTE, KARTE_CUSTOM, EINZELNACHWEIS, EINZELNACHWEIS_STICHTAG or LISTENNACHWEIS");
                }
            } else if (!(body instanceof Body)) {
                throw new IllegalArgumentException(
                    "Body has to be either KARTE, KARTE_CUSTOM, EINZELNACHWEIS, EINZELNACHWEIS_STICHTAG or LISTENNACHWEIS");
            }

            switch ((Body)body) {
                case KARTE: {
                    url = ServerAlkisProducts.getInstance().productKarteUrl(alkisCode, fertigungsVermerk);
                }
                break;
                case KARTE_CUSTOM: {
                    url = ServerAlkisProducts.getInstance()
                                .productKarteUrl(
                                        alkisCode,
                                        produkt,
                                        winkel,
                                        x,
                                        y,
                                        massstab,
                                        massstabMin,
                                        massstabMax,
                                        zusatz,
                                        auftragsnummer,
                                        true,
                                        fertigungsVermerk);
                }
                break;
                case EINZELNACHWEIS: {
                    url = ServerAlkisProducts.getInstance()
                                .productEinzelNachweisUrl(
                                        alkisCode,
                                        produkt,
                                        getUser(),
                                        fertigungsVermerk);
                }
                break;
                case EINZELNACHWEIS_STICHTAG: {
                    url = ServerAlkisProducts.getInstance()
                                .productEinzelnachweisStichtagsbezogenUrl(
                                        alkisCode,
                                        produkt,
                                        stichtag,
                                        getUser());
                }
                break;
                case LISTENNACHWEIS: {
                    url = ServerAlkisProducts.getInstance().productListenNachweisUrl(alkisCode, produkt);
                }
                break;
                default: {
                    url = null;
                }
            }
            if (url != null) {
                return ServerActionHelper.asyncByteArrayHelper(doDownload(
                            url,
                            Body.LISTENNACHWEIS.equals(body),
                            (Body)body),
                        "ProduktReport.pdf");
            } else {
                throw new Exception("url could not be generated");
            }
        } catch (final BadHttpStatusCodeException ex) {
            LOG.error(ex, ex);
            return new RuntimeException(mask(ex.getResponse()));
        } catch (final Exception ex) {
            LOG.error(ex, ex);
            return new RuntimeException(mask(ex.getMessage()));
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   mask  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String mask(final String mask) {
        return mask.replaceAll(AlkisRestConf.getInstance().getCreds().getUser(), MASK_REPLACEMENT)
                    .replaceAll(AlkisRestConf.getInstance().getCreds().getPassword(), MASK_REPLACEMENT);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   url         DOCUMENT ME!
     * @param   postParams  requestParametersString DOCUMENT ME!
     * @param   body        DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private UploadableInputStream doDownload(final URL url, final boolean postParams, final Body body)
            throws Exception {
        final String queryString = url.getQuery();
        final String urlString = url.toExternalForm();
        final boolean fullUrl = (queryString == null) && postParams;

        final String documentType = "alkisrequest" + ((body != null) ? ("_" + body.toString().toLowerCase()) : "");
        return ServerStamperUtils.getInstance()
                    .stampRequest(
                        documentType,
                        fullUrl ? url : new URL(urlString.substring(0, urlString.lastIndexOf('?'))),
                        fullUrl ? null : queryString,
                        new StamperUtils.StamperFallback() {

                            @Override
                            public UploadableInputStream createProduct() throws Exception {
                                return (postParams
                                        ? new UploadableInputStream(
                                            new SimpleHttpAccessHandler().doRequest(
                                                fullUrl ? url
                                                        : new URL(urlString.substring(0, urlString.lastIndexOf('?'))),
                                                fullUrl ? null : new StringReader(queryString),
                                                AccessHandler.ACCESS_METHODS.POST_REQUEST,
                                                AlkisProducts.POST_HEADER))
                                        : new UploadableInputStream(new SimpleHttpAccessHandler().doRequest(url)));
                            }
                        },
                        getConnectionContext());
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
