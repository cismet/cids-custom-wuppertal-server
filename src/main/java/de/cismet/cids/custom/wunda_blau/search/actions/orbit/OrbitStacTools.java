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
package de.cismet.cids.custom.wunda_blau.search.actions.orbit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;

import java.util.HashMap;

/**
 * DOCUMENT ME!
 *
 * @author   hell
 * @version  $Revision$, $Date$
 */
public class OrbitStacTools {

    //~ Instance fields --------------------------------------------------------

    private HashMap<String, StacEntry> stacs = new HashMap<String, StacEntry>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new OrbitStacTools object.
     */
    private OrbitStacTools() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static OrbitStacTools getInstance() {
        return OrbitStacTools.LazyInitialiser.INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   base_login_name  DOCUMENT ME!
     * @param   ipAddress        DOCUMENT ME!
     * @param   stacOptions      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String createStac(final String base_login_name, final String ipAddress, final String stacOptions) {
        final int length = 16;
        final boolean useLetters = true;
        final boolean useNumbers = true;
        final String stac = RandomStringUtils.random(length, useLetters, useNumbers);

        final StacEntry stacEntry = new StacEntry(stac, base_login_name, ipAddress, stacOptions);

        stacs.put(stacEntry.getHash(), stacEntry);

        return stac;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   stac  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public StacEntry getEntry(final String stac) {
        return stacs.get(DigestUtils.md5Hex(stac));
    }

    /**
     * DOCUMENT ME!
     *
     * @param  stac  DOCUMENT ME!
     */
    public void removeStac(final String stac) {
        stacs.remove(DigestUtils.md5Hex(stac));
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final OrbitStacTools INSTANCE;

        static {
            try {
                INSTANCE = new OrbitStacTools();
            } catch (final Exception ex) {
                throw new RuntimeException("Exception while initializing OrbitStacTools", ex);
            }
        }

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
