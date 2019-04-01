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

import lombok.Getter;

import java.util.Properties;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class StamperConf {

    //~ Static fields/initializers ---------------------------------------------

    private static final int DEFAULT_MAX_BUFFER_SIZE = 1024;

    //~ Instance fields --------------------------------------------------------

    private final String stamperService;
    private final String stanmperRequest;
    private final String stamperDocument;
    private final String stamperVerify;
    private final int maxBufferSize;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ServerAlkisConf object.
     *
     * @param   properties  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public StamperConf(final Properties properties) throws Exception {
        stamperService = properties.getProperty("STAMPER_SERVICE");
        stanmperRequest = properties.getProperty("STAMPER_REQUEST");
        stamperDocument = properties.getProperty("STAMPER_DOCUMENT");
        stamperVerify = properties.getProperty("STAMPER_VERIFY");
        maxBufferSize = (properties.getProperty("MAX_BUFFER_SIZE") != null)
            ? Integer.parseInt(properties.getProperty("MAX_BUFFER_SIZE")) : DEFAULT_MAX_BUFFER_SIZE;
    }
}
