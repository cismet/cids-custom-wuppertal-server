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
package de.cismet.cids.custom.utils.vermessungsunterlagen;

import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenException;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenJobInfoWrapper {

    //~ Instance fields --------------------------------------------------------

    private VermessungsunterlagenJob job;
    private VermessungsunterlagenJob.Status jobStatus;
    private String jobResult;
    private String jobError;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenJobInfoWrapper object.
     *
     * @param  job  DOCUMENT ME!
     */
    public VermessungsunterlagenJobInfoWrapper(final VermessungsunterlagenJob job) {
        this.job = job;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     */
    private void refreshJobError() {
        if (job != null) {
            final VermessungsunterlagenException exception = job.getException();
            jobError = (exception != null) ? exception.getMessage() : null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getJobError() {
        refreshJobError();
        return jobError;
    }

    /**
     * DOCUMENT ME!
     */
    private void refreshJobResult() {
        if (job != null) {
            jobResult = job.getFtpZipPath();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getJobResult() {
        refreshJobResult();
        return jobResult;
    }

    /**
     * DOCUMENT ME!
     */
    private void refreshJobStatus() {
        if (job != null) {
            jobStatus = job.getStatus();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public VermessungsunterlagenJob.Status getJobStatus() {
        refreshJobStatus();
        return jobStatus;
    }

    /**
     * DOCUMENT ME!
     */
    public void cleanup() {
        refreshJobStatus();
        refreshJobResult();
        refreshJobError();
        this.job = null;
    }
}
