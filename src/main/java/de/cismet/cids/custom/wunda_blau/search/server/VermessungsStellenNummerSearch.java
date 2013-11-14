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
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;

import org.apache.log4j.Logger;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class VermessungsStellenNummerSearch extends AbstractCidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(VermessungsStellenNummerSearch.class);

    //~ Instance fields --------------------------------------------------------

    private String userName;
    private final String QUERY =
        "select k.vermessungsstellennummer from \"public\".billing_kunden_logins kl join billing_kunde k on k.id=kl.kunde "
                + "where vermessungsstellennummer is not null and kl.name like '%1$s';";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsStellenNummerSearch object.
     *
     * @param  userName  DOCUMENT ME!
     */
    public VermessungsStellenNummerSearch(final String userName) {
        this.userName = userName;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

        if (ms != null) {
            try {
                final String query = String.format(QUERY, userName);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("query: " + query); // NOI18N
                }
                final ArrayList<ArrayList> lists = ms.performCustomSearch(query);
                final ArrayList<String> result = new ArrayList<String>();
                for (final ArrayList l : lists) {
                    final String vermessungsStellenNummer = (String)l.get(0);
                    result.add(vermessungsStellenNummer);
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
}
