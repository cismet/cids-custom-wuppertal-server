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

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.InputStream;

import static org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenFtpHelper {

    //~ Instance fields --------------------------------------------------------

    private final VermessungsunterlagenProperties properties = VermessungsunterlagenProperties.fromServerResources();

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private FTPClient getConnectedFTPClient() throws Exception {
        final FTPClient ftpClient = new FTPClient();
        ftpClient.connect(properties.getFtpHost());

        final int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            throw new Exception("Exception in connecting to FTP Server");
        }
        ftpClient.login(properties.getFtpLogin(), properties.getFtpPass());
        return ftpClient;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   in          DOCUMENT ME!
     * @param   ftpZipPath  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void uploadToFTP(final InputStream in, final String ftpZipPath) throws Exception {
        final FTPClient connectedFtpClient = getConnectedFTPClient();
        connectedFtpClient.enterLocalPassiveMode();
        connectedFtpClient.setFileType(BINARY_FILE_TYPE);
        connectedFtpClient.storeFile(ftpZipPath, in);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   ftpFilePath  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public InputStream downloadFromFTP(final String ftpFilePath) throws Exception {
        final FTPClient connectedFtpClient = getConnectedFTPClient();
        connectedFtpClient.enterLocalPassiveMode();
        connectedFtpClient.setFileType(BINARY_FILE_TYPE);
        return connectedFtpClient.retrieveFileStream(ftpFilePath);
    }
}
