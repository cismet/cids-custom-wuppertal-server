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

import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenTask;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenTaskRetryable;

import de.cismet.cids.dynamics.CidsBean;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class VermUntTaskNivP extends VermessungsunterlagenTask implements VermessungsunterlagenTaskRetryable {

    //~ Instance fields --------------------------------------------------------

    private final Collection<CidsBean> nivPoints;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskAP object.
     *
     * @param  type       DOCUMENT ME!
     * @param  jobkey     DOCUMENT ME!
     * @param  nivPoints  DOCUMENT ME!
     */
    public VermUntTaskNivP(final String type, final String jobkey, final Collection<CidsBean> nivPoints) {
        super(type, jobkey);

        this.nivPoints = nivPoints;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected Collection<CidsBean> getNivPoints() {
        return nivPoints;
    }

    @Override
    protected String getSubPath() {
        return "/NivP";
    }

    @Override
    public long getMaxTotalWaitTimeMs() {
        return DEFAULT_MAX_TOTAL_WAIT_TIME_MS;
    }

    @Override
    public long getFirstWaitTimeMs() {
        return DEFAULT_FIRST_WAIT_TIME_MS;
    }

    @Override
    public double getWaitTimeMultiplicator() {
        return DEFAULT_WAIT_TIME_MULTIPLICATOR;
    }
}
