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
package de.cismet.cids.custom.utils.formsolutions;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class FormSolutionBestellungSpecialLogger {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(FormSolutionBestellungSpecialLogger.class);

    //~ Instance fields --------------------------------------------------------

    private final FileWriter specialLogWriter;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionBestellungSpecialLogger object.
     */
    private FormSolutionBestellungSpecialLogger() {
        FileWriter specialLogWriter = null;
        final String specialLogAbsPath = FormSolutionsProperties.getInstance().getSpecialLogAbsPath();
        try {
            if ((specialLogAbsPath != null) && !specialLogAbsPath.isEmpty()) {
                final File specialLogFile = new File(specialLogAbsPath);
                if (!specialLogFile.exists() || (specialLogFile.isFile() && specialLogFile.canWrite())) {
                    specialLogWriter = new FileWriter(specialLogFile, true);
                }
            }
        } catch (final IOException ex) {
            LOG.error("special log file writer could not be created", ex);
        }
        this.specialLogWriter = specialLogWriter;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param  message  DOCUMENT ME!
     */
    public synchronized void log(final String message) {
        if (specialLogWriter != null) {
            try {
                specialLogWriter.write(System.currentTimeMillis() + " - " + message + "\n");
                specialLogWriter.flush();
            } catch (final IOException ex) {
                LOG.warn("could not write to logSpecial", ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static FormSolutionBestellungSpecialLogger getInstance() {
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

        private static final FormSolutionBestellungSpecialLogger INSTANCE = new FormSolutionBestellungSpecialLogger();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
