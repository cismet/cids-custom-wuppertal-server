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

import de.cismet.cids.custom.utils.pointnumberreservation.VermessungsStellenSearchResult;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class KundeByVermessungsStellenNummerSearch extends AbstractCidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(KundeByVermessungsStellenNummerSearch.class);

    //~ Instance fields --------------------------------------------------------

    private final String vermessungsstellennummer;
    private final String QUERY =
        "SELECT vermessungsstellennummer, name FROM billing_kunde WHERE vermessungsstellennummer LIKE '%1$s';";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsStellenNummerSearch object.
     *
     * @param  vermessungsstellennummer  DOCUMENT ME!
     */
    public KundeByVermessungsStellenNummerSearch(final String vermessungsstellennummer) {
        this.vermessungsstellennummer = vermessungsstellennummer;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

        if (ms != null) {
            try {
                final String query = String.format(QUERY, vermessungsstellennummer);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("query: " + query); // NOI18N
                }
                final ArrayList<ArrayList> lists = ms.performCustomSearch(query);
                final ArrayList<VermessungsStellenSearchResult> result =
                    new ArrayList<VermessungsStellenSearchResult>();
                for (final ArrayList l : lists) {
                    final String vermessungsStellenNummer = (String)l.get(0);
                    final String name = (String)l.get(1);
                    result.add(new VermessungsStellenSearchResult(vermessungsStellenNummer, name));
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
