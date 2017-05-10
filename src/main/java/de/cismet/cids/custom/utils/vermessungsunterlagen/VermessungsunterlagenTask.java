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

import lombok.Getter;

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenException;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenTaskException;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenTaskRetryException;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class VermessungsunterlagenTask implements Callable<VermessungsunterlagenTask> {

    //~ Static fields/initializers ---------------------------------------------

    protected static final transient Logger LOG = Logger.getLogger(VermessungsunterlagenTask.class);

    protected static final long DEFAULT_FIRST_WAIT_TIME_MS = 1000;
    protected static final long DEFAULT_MAX_TOTAL_WAIT_TIME_MS = 1023000; // 1000 * (2^10 - 1) => 1 try + 9 retries
    protected static final double DEFAULT_WAIT_TIME_MULTIPLICATOR = 2;

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Status {

        //~ Enum constants -----------------------------------------------------

        NONE, RUNNING, FINISHED, ERROR
    }

    //~ Instance fields --------------------------------------------------------

    @Getter private final String type;

    @Getter private final String jobKey;

    @Getter private Status status = Status.NONE;

    @Getter private final Collection<String> files = new ArrayList<String>();

    @Getter private VermessungsunterlagenTaskException exception;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenTask object.
     *
     * @param  type    DOCUMENT ME!
     * @param  jobkey  DOCUMENT ME!
     */
    protected VermessungsunterlagenTask(final String type, final String jobkey) {
        this.type = type;
        this.jobKey = jobkey;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param  file  DOCUMENT ME!
     */
    protected void addFile(final String file) {
        files.add(file);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  status  DOCUMENT ME!
     */
    protected void setStatus(final Status status) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Task status changed (" + getJobKey() + "/" + getType() + "): " + status.toString());
        }
        this.status = status;
    }

    @Override
    public VermessungsunterlagenTask call() throws VermessungsunterlagenTaskException {
        setStatus(Status.RUNNING);
        try {
            new File(getPath()).mkdirs();
            if (this instanceof VermessungsunterlagenTaskRetryable) {
                final VermessungsunterlagenTaskRetryable retryable = (VermessungsunterlagenTaskRetryable)this;

                boolean success = false;
                long totalWaitTime = 0;
                long waitTime = retryable.getFirstWaitTimeMs();

                // try until success or waited long enough
                final List<VermessungsunterlagenException> exceptions = new ArrayList();
                while (!success && (totalWaitTime < retryable.getMaxTotalWaitTimeMs())) {
                    try {
                        performTask();

                        // success only if no exception occurs
                        success = true;
                    } catch (final VermessungsunterlagenException ex) {
                        // overwrite exception and wait before next try
                        LOG.warn("performTask failed. will wait " + waitTime + "ms and try again", ex);
                        exceptions.add(ex);
                        try {
                            Thread.sleep(waitTime);
                        } catch (final InterruptedException ex1) {
                            throw new VermessungsunterlagenTaskException(getType(), ex1.getMessage(), ex1);
                        }
                    } finally {
                        // calculate the new total wait time
                        totalWaitTime += waitTime;

                        // increase the next wait time by the old wait time * multiplicator.
                        // the wait time goes exponential
                        waitTime *= retryable.getWaitTimeMultiplicator();
                    }
                }
                if (!success && (!exceptions.isEmpty())) {
                    throw new VermessungsunterlagenTaskRetryException(
                        getType(),
                        "Abbruch nach "
                                + exceptions.size()
                                + " Versuchen.",
                        exceptions);
                }
            } else {
                performTask();
            }
            setStatus(Status.FINISHED);
        } catch (final VermessungsunterlagenTaskException ex) {
            LOG.info("setting status to ERROR because of an exception", ex);
            this.exception = ex;
            setStatus(Status.ERROR);
            VermessungsunterlagenHelper.writeExceptionJson(ex, getPath() + "/fehlerprotokoll_" + getType() + ".json");
        }
        return this;
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  VermessungsunterlagenTaskException  de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenTaskException
     */
    protected abstract void performTask() throws VermessungsunterlagenTaskException;

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected abstract String getSubPath();

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getPath() {
        return VermessungsunterlagenHelper.getInstance().getPath(getJobKey().replace("/", "--")) + getSubPath();
    }
}
