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

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.InputStream;
import java.io.OutputStream;

import static org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class FormSolutionsFtpClient {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionBestellungFtpClient object.
     */
    private FormSolutionsFtpClient() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   in               DOCUMENT ME!
     * @param   destinationPath  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void upload(final InputStream in, final String destinationPath) throws Exception {
        final FTPClient connectedFtpClient = getConnectedFTPClient();
        connectedFtpClient.enterLocalPassiveMode();
        connectedFtpClient.setFileType(BINARY_FILE_TYPE);
        connectedFtpClient.storeFile(destinationPath, in);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   destinationPath  DOCUMENT ME!
     * @param   out              DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void download(final String destinationPath, final OutputStream out) throws Exception {
        final FTPClient connectedFtpClient = getConnectedFTPClient();
        connectedFtpClient.enterLocalPassiveMode();
        connectedFtpClient.setFileType(BINARY_FILE_TYPE);
        if (!connectedFtpClient.retrieveFile(destinationPath, out)) {
            throw new Exception("file " + destinationPath + " not found");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private FTPClient getConnectedFTPClient() throws Exception {
        final FTPClient ftpClient = new FTPClient();
        ftpClient.connect(FormSolutionsProperties.getInstance().getFtpHost());

        final int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            throw new Exception("Exception in connecting to FTP Server");
        }
        ftpClient.login(FormSolutionsProperties.getInstance().getFtpLogin(),
            FormSolutionsProperties.getInstance().getFtpPass());
        return ftpClient;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static FormSolutionsFtpClient getInstance() {
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

        private static final FormSolutionsFtpClient INSTANCE = new FormSolutionsFtpClient();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
