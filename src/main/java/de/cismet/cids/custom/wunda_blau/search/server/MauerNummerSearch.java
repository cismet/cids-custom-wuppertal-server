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

import org.apache.log4j.Logger;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.CidsServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = CidsServerSearch.class)
public class MauerNummerSearch extends AbstractCidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(MauerNummerSearch.class);

    //~ Instance fields --------------------------------------------------------

    private String mauerNummer;
    private final String QUERY = "SELECT id, mauer_nummer FROM mauer WHERE mauer_nummer = '%1$s'";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MauerNummerSearch object.
     */
    public MauerNummerSearch() {
    }

    /**
     * Creates a new MauerNummerSearch object.
     *
     * @param  mauerNummer  DOCUMENT ME!
     */
    public MauerNummerSearch(final String mauerNummer) {
        setMauerNummer(mauerNummer);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getMauerNummer() {
        return mauerNummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  mauerNummer  DOCUMENT ME!
     */
    public final void setMauerNummer(final String mauerNummer) {
        this.mauerNummer = mauerNummer;
    }

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

        if (ms != null) {
            try {
                final String query = String.format(QUERY, mauerNummer);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("query: " + query); // NOI18N
                }
                final ArrayList<ArrayList> lists = ms.performCustomSearch(query);
                return lists;
            } catch (RemoteException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        } else {
            LOG.error("active local server not found"); // NOI18N
        }

        return null;
    }
}
