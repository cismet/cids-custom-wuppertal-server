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

import java.io.InputStream;

import de.cismet.commons.security.WebDavClient;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenWebdavHelper {

    //~ Instance fields --------------------------------------------------------

    private final VermessungsunterlagenProperties properties = VermessungsunterlagenProperties.fromServerResources();

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   in             DOCUMENT ME!
     * @param   webDAVZipPath  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void uploadToWebDAV(final InputStream in, final String webDAVZipPath) throws Exception {
        final String url = properties.getWebDavHost() + "/" + webDAVZipPath;

        final WebDavClient webdavclient = new WebDavClient(
                null,
                properties.getWebDavLogin(),
                properties.getWebDavPass(),
                false);
        final int statusCode = webdavclient.put(url, in);
        if (statusCode >= 400) {
            throw new Exception("webdav put failed wit status code " + statusCode);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   webDAVFilePath  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public InputStream downloadFromWebDAV(final String webDAVFilePath) throws Exception {
        final WebDavClient webdavclient = new WebDavClient(
                null,
                properties.getWebDavLogin(),
                properties.getWebDavPass(),
                false);
        return webdavclient.getInputStream(properties.getWebDavHost() + webDAVFilePath);
    }
}
