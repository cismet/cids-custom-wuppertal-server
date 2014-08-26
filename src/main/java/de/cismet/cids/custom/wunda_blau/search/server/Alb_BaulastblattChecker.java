/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 *  Copyright (C) 2010 stefan
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
public class Alb_BaulastblattChecker extends AbstractCidsServerSearch {

    //~ Instance fields --------------------------------------------------------

    private String searchQuery = null;
    private String blattnummer = null;
    private Integer id = null;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Alb_BaulastblattChecker object.
     */
    public Alb_BaulastblattChecker() {
    }

    /**
     * Creates a new Alb_BaulastblattChecker object.
     *
     * @param  blattnummer  DOCUMENT ME!
     * @param  id           DOCUMENT ME!
     */
    public Alb_BaulastblattChecker(final String blattnummer, final int id) {
        setBlattnummer(blattnummer);
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
    public Integer getId() {
        return id;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  blattnummer  DOCUMENT ME!
     */
    public final void setBlattnummer(final String blattnummer) {
        this.blattnummer = blattnummer;
        refreshSearchQuery();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  id  DOCUMENT ME!
     */
    public final void setId(final int id) {
        this.id = id;
        refreshSearchQuery();
    }

    /**
     * DOCUMENT ME!
     */
    private void refreshSearchQuery() {
        this.searchQuery = "select count(*) from alb_baulastblatt where blattnummer = '"
                    + blattnummer.replaceAll("'", "") + "' and id <> " + id;
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
