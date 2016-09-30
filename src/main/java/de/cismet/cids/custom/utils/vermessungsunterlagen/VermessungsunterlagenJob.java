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
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.cismet.commons.concurrency.CismetExecutors;

import static de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper.writeExceptionJson;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenJob implements Runnable {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(VermessungsunterlagenJob.class);

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

    @Getter private final String jobkey;

    @Getter private final VermessungsunterlagenAnfrageBean anfrageBean;

    @Getter private Status status = Status.NONE;

    @Getter private Exception exception;

    @Getter private final Collection<VermessungsunterlagenTask> tasks = new ArrayList<VermessungsunterlagenTask>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenJobBean object.
     *
     * @param  jobkey       DOCUMENT ME!
     * @param  anfrageBean  DOCUMENT ME!
     */
    public VermessungsunterlagenJob(final String jobkey, final VermessungsunterlagenAnfrageBean anfrageBean) {
        this.jobkey = jobkey;
        this.anfrageBean = anfrageBean;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param  status  DOCUMENT ME!
     */
    public void setStatus(final Status status) {
        LOG.info("Job changed (" + getJobkey() + "): " + status.toString());
        this.status = status;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  task  DOCUMENT ME!
     */
    public void addTask(final VermessungsunterlagenTask task) {
        this.tasks.add(task);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  exception  DOCUMENT ME!
     */
    public void setException(final Exception exception) {
        this.exception = exception;
    }

    /**
     * DOCUMENT ME!
     */
    @Override
    public void run() {
        final ThreadPoolExecutor taskExecutor = (ThreadPoolExecutor)CismetExecutors.newFixedThreadPool(tasks.size());

        setStatus(Status.RUNNING);
        final Collection<Future> futures = new ArrayList<Future>(tasks.size());
        for (final VermessungsunterlagenTask task : tasks) {
            futures.add(taskExecutor.submit(task));
        }

        try {
            // wait for tasks to finish
            for (final Future future : futures) {
                future.get();
            }

            createAndUploadZip();

            setStatus(Status.FINISHED);
        } catch (final Exception ex) {
            LOG.error("error while generating documents.", ex);
            writeExceptionJson(ex, getPath() + "/fehlerprotokoll_.json");
            setException(ex);
            setStatus(Status.ERROR);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void createAndUploadZip() throws Exception {
        zipDirectory(getPath(), getPath() + ".zip");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   dirName  DOCUMENT ME!
     * @param   zipName  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void zipDirectory(final String dirName, final String zipName) throws Exception {
        ZipOutputStream zipOut = null;
        try {
            zipOut = new ZipOutputStream(new FileOutputStream(zipName));
            addDirectoryToZip("", dirName, zipOut);
            zipOut.flush();
        } finally {
            VermessungsunterlagenHelper.closeStream(zipOut);
        }
    }
    /**
     * recursively add files to the zip files.
     *
     * @param   path      DOCUMENT ME!
     * @param   fileName  DOCUMENT ME!
     * @param   zipOut    DOCUMENT ME!
     * @param   flag      DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void addFileToZip(final String path,
            final String fileName,
            final ZipOutputStream zipOut,
            final boolean flag) throws Exception {
        final File file = new File(fileName);

        if (flag == true) {
            zipOut.putNextEntry(new ZipEntry(path + "/" + file.getName() + "/"));
        } else {
            if (file.isDirectory()) {
                addDirectoryToZip(path, fileName, zipOut);
            } else {
                final byte[] buf = new byte[1024];
                int len;
                final FileInputStream in = new FileInputStream(fileName);
                zipOut.putNextEntry(new ZipEntry(path + "/" + file.getName()));
                while ((len = in.read(buf)) > 0) {
                    zipOut.write(buf, 0, len);
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   path     DOCUMENT ME!
     * @param   dirName  DOCUMENT ME!
     * @param   zipOut   DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void addDirectoryToZip(final String path, final String dirName, final ZipOutputStream zipOut)
            throws Exception {
        final File dir = new File(dirName);

        if (dir.isDirectory()) {
            if (dir.list().length == 0) {
                addFileToZip(path, dirName, zipOut, true);
            } else {
                for (final String filename : dir.list()) {
                    if (path.isEmpty()) {
                        addFileToZip(dir.getName(), dirName + "/" + filename, zipOut, false);
                    } else {
                        addFileToZip(path + "/" + dir.getName(), dirName + "/" + filename, zipOut, false);
                    }
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getPath() {
        return VermessungsunterlagenHelper.getPath(getJobkey());
    }
}
