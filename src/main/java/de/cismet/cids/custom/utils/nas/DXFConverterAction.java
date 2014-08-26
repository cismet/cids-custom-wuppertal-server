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
package de.cismet.cids.custom.utils.nas;

import java.io.File;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.ws.rs.core.MediaType;

import de.cismet.cids.server.api.types.ActionTask;

import de.cismet.commons.concurrency.CismetExecutors;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class DXFConverterAction extends CidsActionClient {

    //~ Static fields/initializers ---------------------------------------------

    private static final String ACTION_KEY = "dxf";
    private static final ExecutorService executor = CismetExecutors.newCachedLimitedThreadPool(
            30,
            "dxfConvertPollingThread");
    private static final int POLLING_INTERVAL = 5000;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new DXFConverterAction object.
     *
     * @param  domain      DOCUMENT ME!
     * @param  serviceUrl  DOCUMENT ME!
     */
    public DXFConverterAction(final String domain, final String serviceUrl) {
        super(domain, serviceUrl);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   params    DOCUMENT ME!
     * @param   nasFile   DOCUMENT ME!
     * @param   isZipped  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ActionTask createDxfActionTask(final Map<String, Object> params,
            final File nasFile,
            final boolean isZipped) {
        final ActionTask task = new ActionTask();
        if (isZipped) {
            params.put("$1", "zip");
        }
        task.setParameters(params);
        final MediaType type = isZipped ? new MediaType("application", "zip") : MediaType.APPLICATION_XML_TYPE;
        return super.createTask(ACTION_KEY, task, nasFile, type, true);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   taskKey  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Future<File> getResult(final String taskKey) {
        return executor.submit(new DXFConverterPollingCallable(taskKey));
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private final class DXFConverterPollingCallable implements Callable<File> {

        //~ Instance fields ----------------------------------------------------

        private final String taskKey;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new DXFConverterPollingCallable object.
         *
         * @param  taskKey  DOCUMENT ME!
         */
        public DXFConverterPollingCallable(final String taskKey) {
            this.taskKey = taskKey;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public File call() throws Exception {
            ActionTask.Status status = getTaskStatus(ACTION_KEY, taskKey);
            while (status != ActionTask.Status.FINISHED) {
                try {
                    Thread.sleep(POLLING_INTERVAL);
                    status = getTaskStatus(ACTION_KEY, taskKey);
                } catch (InterruptedException ex) {
                }
            }

            final File tmpFile = getTaskResult(File.class,
                    ACTION_KEY,
                    taskKey,
                    "dxfOutput");
            return tmpFile;
        }
    }
}
