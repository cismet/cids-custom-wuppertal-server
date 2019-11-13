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
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import org.openide.util.Exceptions;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Properties;

import static org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class FormSolutionsFtpClient {

    //~ Instance fields --------------------------------------------------------

    private final FormSolutionsProperties properties;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionBestellungFtpClient object.
     *
     * @param  properties  DOCUMENT ME!
     */
    private FormSolutionsFtpClient(final FormSolutionsProperties properties) {
        this.properties = properties;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public FormSolutionsProperties getProperties() {
        return properties;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   in               DOCUMENT ME!
     * @param   destinationPath  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void upload(final InputStream in, final String destinationPath) throws Exception {
        final FTPClient ftpClient = getConnectedFTPClient();
        ftpClient.setFileType(BINARY_FILE_TYPE);
        ftpClient.storeFile(destinationPath, in);
        ftpClient.disconnect();
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void test() throws Exception {
        final FTPClient ftpClient = getConnectedFTPClient();
        System.out.println(ftpClient.printWorkingDirectory());
        for (final FTPFile ftpFile : ftpClient.listFiles()) {
            System.out.println(ftpFile.getName());
        }
        ftpClient.disconnect();
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
        final FTPClient ftpClient = getConnectedFTPClient();
        ftpClient.setFileType(BINARY_FILE_TYPE);
        if (!ftpClient.retrieveFile(destinationPath, out)) {
            throw new Exception("file " + destinationPath + " not found");
        }
        ftpClient.disconnect();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        try {
            final Properties props = new Properties();
            props.load(new FileInputStream(
                    "/home/jruiz/cidsDistribution/server/040_wunda_live/server_resources/formsolutions/fs_conf.properties"));
            final FormSolutionsProperties fsProps = new FormSolutionsProperties(props);
            final FormSolutionsFtpClient ftpClient = new FormSolutionsFtpClient(fsProps);
            ftpClient.test();
        } catch (final Exception ex) {
            Exceptions.printStackTrace(ex);
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
        final String host = getProperties().getFtpHost();
        final String username = getProperties().getFtpLogin();
        final String password = getProperties().getFtpPass();
        final boolean secure = getProperties().isFtpOverTls();

        final FTPClient ftpClient = secure ? new FTPSClient() : new FTPClient();
        ftpClient.connect(host);
        final int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            throw new Exception("Exception in connecting to FTP Server");
        }
        ftpClient.enterLocalPassiveMode();
        if (!ftpClient.login(username, password)) {
            throw new Exception("Login failed");
        }
        ;

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

        private static final FormSolutionsFtpClient INSTANCE = new FormSolutionsFtpClient(FormSolutionsProperties
                        .getInstance());

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
