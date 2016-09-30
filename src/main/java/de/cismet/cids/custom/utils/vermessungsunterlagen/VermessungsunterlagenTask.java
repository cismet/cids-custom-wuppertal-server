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

import java.io.File;

import java.util.ArrayList;
import java.util.Collection;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class VermessungsunterlagenTask implements Runnable {

    //~ Static fields/initializers ---------------------------------------------

    protected static final transient Logger LOG = Logger.getLogger(VermessungsunterlagenTask.class);

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

    @Getter private final String jobkey;

    @Getter private Status status = Status.NONE;

    @Getter private final Collection<String> files = new ArrayList<String>();

    @Getter private Exception exception;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenTask object.
     *
     * @param  type    DOCUMENT ME!
     * @param  jobkey  DOCUMENT ME!
     */
    protected VermessungsunterlagenTask(final String type, final String jobkey) {
        this.type = type;
        this.jobkey = jobkey;

        new File(getPath()).mkdirs();
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
            LOG.debug("Task status changed (" + getJobkey() + "/" + getType() + "): " + status.toString());
        }
        this.status = status;
    }

    @Override
    public void run() {
        setStatus(Status.RUNNING);
        try {
            performTask();
            setStatus(Status.FINISHED);
        } catch (final Exception ex) {
            LOG.info("setting status to ERROR because of an exception", ex);
            this.exception = ex;
            setStatus(Status.ERROR);
            VermessungsunterlagenHelper.writeExceptionJson(ex, getPath() + "/fehlerprotokoll_" + getType() + ".json");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    protected abstract void performTask() throws Exception;

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
        return VermessungsunterlagenHelper.getPath(jobkey) + getSubPath();
    }
}
