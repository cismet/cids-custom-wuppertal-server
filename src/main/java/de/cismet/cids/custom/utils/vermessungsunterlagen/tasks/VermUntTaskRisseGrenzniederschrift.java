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
package de.cismet.cids.custom.utils.vermessungsunterlagen.tasks;

import java.util.Collection;

import de.cismet.cids.custom.utils.alkis.AlkisConstants;

import de.cismet.cids.dynamics.CidsBean;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskRisseGrenzniederschrift extends VermUntTaskRisse {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "Risse_ergdok";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskRisseGrenzniederschrift object.
     *
     * @param  jobKey      DOCUMENT ME!
     * @param  risseBeans  DOCUMENT ME!
     */
    public VermUntTaskRisseGrenzniederschrift(final String jobKey, final Collection<CidsBean> risseBeans) {
        super(
            TYPE,
            jobKey,
            risseBeans,
            AlkisConstants.COMMONS.VERMESSUNG_HOST_GRENZNIEDERSCHRIFTEN);
    }
}
