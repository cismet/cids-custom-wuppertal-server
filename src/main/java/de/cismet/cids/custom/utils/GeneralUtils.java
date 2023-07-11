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
package de.cismet.cids.custom.utils;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
public class GeneralUtils {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(GeneralUtils.class);
    private static final String CMDREPLACER_EMAIL_ADDRESS = "{MAIL_ADDRESS}";
    private static final String CMDREPLACER_TOPIC = "{TOPIC}";
    private static final String CMDREPLACER_MESSAGE = "{MESSAGE}";

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   cmd  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private static String executeCmd(final String cmd) throws Exception {
        final ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", cmd);
        final Process process = builder.start();
        final InputStream is = process.getInputStream();
        return IOUtils.toString(new InputStreamReader(is));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   cmdTemplate   DOCUMENT ME!
     * @param   emailAdresse  DOCUMENT ME!
     * @param   betreff       DOCUMENT ME!
     * @param   inhalt        DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static void sendMail(final String cmdTemplate,
            final String emailAdresse,
            final String betreff,
            final String inhalt) throws Exception {
        if ((emailAdresse != null) && (cmdTemplate != null)) {
            final String cmd =
                cmdTemplate.replaceAll(Pattern.quote(CMDREPLACER_EMAIL_ADDRESS), Matcher.quoteReplacement(emailAdresse)) //
                .replaceAll(Pattern.quote(CMDREPLACER_TOPIC), Matcher.quoteReplacement(betreff))                         //
                .replaceAll(Pattern.quote(CMDREPLACER_MESSAGE), Matcher.quoteReplacement(inhalt));                       //

            LOG.info(String.format("executing sendMail CMD: %s", cmd));
            executeCmd(cmd);
        }
    }
}
