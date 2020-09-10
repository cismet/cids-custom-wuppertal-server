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
package de.cismet.cids.custom.utils.formsolutions;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungProperties;
import de.cismet.cids.custom.wunda_blau.search.server.FormSolutionsBestellungSearch;

import de.cismet.cids.server.messages.CidsServerMessageManagerImpl;
import de.cismet.cids.server.messages.CidsServerMessageManagerListener;
import de.cismet.cids.server.messages.CidsServerMessageManagerListenerEvent;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class FormSolutionsBestellungBerechtigungspruefungHandler implements ConnectionContextStore, MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            FormSolutionsBestellungBerechtigungspruefungHandler.class);

    //~ Instance fields --------------------------------------------------------

    private MetaService metaService;
    private ConnectionContext connectionContext;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionsBestellungBerechtigungspruefungHandler object.
     */
    private FormSolutionsBestellungBerechtigungspruefungHandler() {
        CidsServerMessageManagerImpl.getInstance()
                .addCidsServerMessageManagerListener(new BerechtigungspruefungServerMessageListener());
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static FormSolutionsBestellungBerechtigungspruefungHandler getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    @Override
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final FormSolutionsBestellungBerechtigungspruefungHandler INSTANCE =
            new FormSolutionsBestellungBerechtigungspruefungHandler();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private class BerechtigungspruefungServerMessageListener implements CidsServerMessageManagerListener {

        //~ Instance fields ----------------------------------------------------

        private final User user = FormSolutionsBestellungHandler.getFsUser();

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public User getUser() {
            return user;
        }

        @Override
        public void messagePublished(final CidsServerMessageManagerListenerEvent csmmle) {
            if (BerechtigungspruefungProperties.getInstance().getCsmFreigabe().equals(
                            csmmle.getMessage().getCategory())) {
                final FormSolutionsBestellungSearch search = new FormSolutionsBestellungSearch();
                final Map localServers = new HashMap<>();
                localServers.put("WUNDA_BLAU", getMetaService());
                search.setActiveLocalServers(localServers);
                search.setUser(getUser());
                if (search instanceof ConnectionContextStore) {
                    ((ConnectionContextStore)search).initWithConnectionContext(getConnectionContext());
                }

                final List<String> schluessels = (List)csmmle.getMessage().getContent();
                for (final String schluessel : schluessels) {
                    search.setBerechtigungspruefungSchluessel(schluessel);
                    final Collection<MetaObjectNode> mons = search.performServerSearch();

                    new FormSolutionsBestellungHandler(
                        getMetaService(),
                        getConnectionContext()).executeSingleStep(FormSolutionsBestellungHandler.STATUS_PRODUKT, mons);
                }
            }
        }
    }
}
