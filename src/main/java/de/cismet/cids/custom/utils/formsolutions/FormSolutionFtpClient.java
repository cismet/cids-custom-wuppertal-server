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
public class FormSolutionFtpClient {

    //~ Static fields/initializers ---------------------------------------------

    private static FormSolutionFtpClient INSTANCE;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionBestellungFtpClient object.
     */
    private FormSolutionFtpClient() {
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
        ftpClient.connect(FormSolutionsConstants.FTP_HOST);

        final int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            throw new Exception("Exception in connecting to FTP Server");
        }
        ftpClient.login(FormSolutionsConstants.FTP_LOGIN, FormSolutionsConstants.FTP_PASS);
        return ftpClient;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static FormSolutionFtpClient getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FormSolutionFtpClient();
        }
        return INSTANCE;
    }
}
