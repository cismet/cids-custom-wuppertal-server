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

import de.cismet.cids.custom.utils.alkis.ServerAlkisConf;

import de.cismet.cids.dynamics.CidsBean;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskRisseGrenzniederschrift extends VermUntTaskRisse {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "RISSE_ErgaenzendeDokumente";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskRisseGrenzniederschrift object.
     *
     * @param  jobKey          DOCUMENT ME!
     * @param  risseBeans      DOCUMENT ME!
     * @param  auftragsnummer  DOCUMENT ME!
     * @param  projektnummer   DOCUMENT ME!
     */
    public VermUntTaskRisseGrenzniederschrift(final String jobKey,
            final Collection<CidsBean> risseBeans,
            final String auftragsnummer,
            final String projektnummer) {
        super(
            TYPE,
            jobKey,
            risseBeans,
            ServerAlkisConf.getInstance().getVermessungHostGrenzniederschriften(),
            auftragsnummer,
            projektnummer);
    }
}
