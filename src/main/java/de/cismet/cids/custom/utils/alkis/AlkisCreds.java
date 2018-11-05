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
package de.cismet.cids.custom.utils.alkis;

import lombok.Getter;

import java.util.Properties;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class AlkisCreds {

    //~ Instance fields --------------------------------------------------------

    private final String user;
    private final String password;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlkisConf object.
     *
     * @param  properties  DOCUMENT ME!
     */
    public AlkisCreds(final Properties properties) {
        user = properties.getProperty("USER");
        password = properties.getProperty("PASSWORD");
    }
}
