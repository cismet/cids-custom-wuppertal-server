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
package de.cismet.cids.custom.wunda_blau.trigger;

import org.openide.util.Lookup;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.cismet.cids.custom.wunda_blau.search.server.StorableSearch;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class CsSearchconfHandler {

    //~ Instance fields --------------------------------------------------------

    private final Map<String, StorableSearch> storableSearches = new HashMap<>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CsSearchconfHandler object.
     */
    private CsSearchconfHandler() {
        final Collection<? extends StorableSearch> lookupStorableSearches = Lookup.getDefault()
                    .lookupAll(StorableSearch.class);
        if (lookupStorableSearches != null) {
            for (final StorableSearch storableSearch : lookupStorableSearches) {
                if (storableSearch != null) {
                    final String searchName = storableSearch.getName();
                    if (searchName != null) {
                        storableSearches.put(searchName, storableSearch);
                    }
                }
            }
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   name  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public StorableSearch getStorableSearches(final String name) {
        return storableSearches.get(name);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static CsSearchconfHandler getInstance() {
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

        private static final CsSearchconfHandler INSTANCE = new CsSearchconfHandler();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
