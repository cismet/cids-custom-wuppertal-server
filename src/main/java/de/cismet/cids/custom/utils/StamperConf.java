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

    //~ Instance fields --------------------------------------------------------

    private final String tmpDir;
    private final String stamperService;
    private final String stamperRequest;
    private final String stamperDocument;
    private final String stamperVerify;
    private final String password;
    private final String[] enabledFor;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ServerAlkisConf object.
     *
     * @param   properties  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public StamperConf(final Properties properties) throws Exception {
        tmpDir = properties.getProperty("TMP_DIR");
        stamperService = properties.getProperty("STAMPER_SERVICE");
        stamperRequest = properties.getProperty("STAMPER_REQUEST");
        stamperDocument = properties.getProperty("STAMPER_DOCUMENT");
        stamperVerify = properties.getProperty("STAMPER_VERIFY");
        password = properties.getProperty("STAMPER_PASSWORD");
        enabledFor = (properties.getProperty("ENABLED_FOR") != null) ? properties.getProperty("ENABLED_FOR").split(",")
                                                                     : new String[0];
    }
}
