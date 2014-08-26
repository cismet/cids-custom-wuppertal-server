/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.CidsServerSearch;

/**
 * DOCUMENT ME!
 *
 * @author   stefan
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = CidsServerSearch.class)
public class Alb_BaulastChecker extends AbstractCidsServerSearch {

    //~ Instance fields --------------------------------------------------------

    private String searchQuery;
    private String blattnummer;
    private String lastnummer;
    private Integer id;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Alb_BaulastChecker object.
     */
    public Alb_BaulastChecker() {
    }

    /**
     * Creates a new Alb_BaulastChecker object.
     *
     * @param  blattnummer  DOCUMENT ME!
     * @param  lastnummer   DOCUMENT ME!
     * @param  id           DOCUMENT ME!
     */
    public Alb_BaulastChecker(final String blattnummer, final String lastnummer, final int id) {
        setBlattnummer(blattnummer);
        setLastnummer(lastnummer);
        setId(id);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getBlattnummer() {
        return blattnummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getLastnummer() {
        return lastnummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Integer getId() {
        return id;
    }

    /**
     * DOCUMENT ME!
     */
    private void refreshQuery() {
        this.searchQuery = "select count(*) from alb_baulast where blattnummer = '" + blattnummer.replaceAll("'", "")
                    + "' and laufende_nummer = '" + lastnummer.replaceAll("'", "") + "' and id <> " + id;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  blattnummer  DOCUMENT ME!
     */
    public final void setBlattnummer(final String blattnummer) {
        this.blattnummer = blattnummer;
        refreshQuery();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  lastnummer  DOCUMENT ME!
     */
    public final void setLastnummer(final String lastnummer) {
        this.lastnummer = lastnummer;
        refreshQuery();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  id  DOCUMENT ME!
     */
    public final void setId(final Integer id) {
        this.id = id;
        refreshQuery();
    }

    @Override
    public Collection performServerSearch() {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
        if (ms != null) {
            try {
                final ArrayList<ArrayList> lists = ms.performCustomSearch(searchQuery);
                return lists;
            } catch (RemoteException ex) {
            }
        }
        //
        return null;
    }

    @Override
    public String toString() {
        return searchQuery;
    }
}
