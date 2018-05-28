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

import java.net.URL;

import java.util.Date;

import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;

import de.cismet.cids.server.actions.DownloadFileAction;
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
public class AlkisProductServerAction extends DownloadFileAction implements ConnectionContextStore,
    UserAwareServerAction,
    MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    public static final Logger LOG = Logger.getLogger(AlkisProductServerAction.class);
    public static final String TASK_NAME = "alkisProduct";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Body {

        //~ Enum constants -----------------------------------------------------

        KARTE, EINZELNACHWEIS, EINZELNACHWEIS_STICHTAG
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        ALKIS_CODE, PRODUKT, FERTIGUNGSVERMERK, STICHTAG
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
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            String product = null;
            String alkisCode = null;
            String fertigungsVermerk = null;
            Date stichtag = null;

            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(AlkisProductServerAction.Parameter.PRODUKT.toString())) {
                        product = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.ALKIS_CODE.toString())) {
                        alkisCode = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.FERTIGUNGSVERMERK.toString())) {
                        fertigungsVermerk = (String)sap.getValue();
                    } else if (sap.getKey().equals(AlkisProductServerAction.Parameter.STICHTAG.toString())) {
                        stichtag = (Date)sap.getValue();
                    }
                }
            }

            final URL url;

            switch ((Body)body) {
                case KARTE: {
                    url = ServerAlkisProducts.getInstance().productKarteUrl(
                            alkisCode,
                            fertigungsVermerk);
                }
                break;
                case EINZELNACHWEIS: {
                    url = ServerAlkisProducts.getInstance()
                                .productEinzelNachweisUrl(
                                        alkisCode,
                                        product,
                                        getUser(),
                                        fertigungsVermerk);
                }
                break;
                case EINZELNACHWEIS_STICHTAG: {
                    url = ServerAlkisProducts.getInstance()
                                .productEinzelnachweisStichtagsbezogenUrl(
                                        alkisCode,
                                        product,
                                        stichtag,
                                        getUser());
                }
                break;
                default: {
                    url = null;
                }
            }
            if (url != null) {
                final Object ret = super.execute(url);
                if (ret == null) {
                    throw new RuntimeException("File not found.");
                }
                return ret;
            } else {
                return null;
            }
        } catch (final Exception ex) {
            LOG.error(ex, ex);
            return ex;
        }
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
