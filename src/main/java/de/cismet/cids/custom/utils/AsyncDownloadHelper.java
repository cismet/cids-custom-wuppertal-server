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

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import de.cismet.cids.server.actions.PreparedAsyncByteAction;

import de.cismet.commons.security.WebDavClient;

import de.cismet.netutil.ProxyHandler;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
public class AsyncDownloadHelper {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(AsyncDownloadHelper.class);

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   taskResult  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static Object actionResultToByteArrayIfPossible(final Object taskResult) {
        if ((taskResult instanceof PreparedAsyncByteAction)) {
            final PreparedAsyncByteAction preparedTaskResult = (PreparedAsyncByteAction)taskResult;

            final String server = preparedTaskResult.getUrl();
            final WebDavClient webDavClient = new WebDavClient(ProxyHandler.getInstance().getProxy(),
                    "",
                    "");

            try {
                final InputStream iStream = webDavClient.getInputStream(server);
                final long length = preparedTaskResult.getLength();
                final byte[] tmp = new byte[1024];

                if (length > 0) {
                    final byte[] result = new byte[(int)length];
                    int resCounter = 0;
                    int counter;

                    // iStream.read(result) does sometimes not read the whole stream

                    while ((counter = iStream.read(tmp)) != -1) {
                        System.arraycopy(tmp, 0, result, resCounter, counter);
                        resCounter += counter;
                    }

                    return result;
                } else {
                    final ByteArrayOutputStream result = new ByteArrayOutputStream();
                    int counter;

                    while ((counter = iStream.read(tmp)) != -1) {
                        result.write(tmp, 0, counter);
                    }

                    return result.toByteArray();
                }
            } catch (Exception e) {
                LOG.error("Error while download action result", e);

                return null;
            }
        } else {
            return taskResult;
        }
    }
}
