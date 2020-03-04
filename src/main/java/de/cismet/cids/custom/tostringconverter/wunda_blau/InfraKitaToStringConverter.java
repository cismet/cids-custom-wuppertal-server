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
package de.cismet.cids.custom.tostringconverter.wunda_blau;

import de.cismet.cids.tools.CustomToStringConverter;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
public class InfraKitaToStringConverter extends CustomToStringConverter {
    public static final String FIELD__NAME = "name";
    public static final String FIELD__ADRESSE = "adresse";                               
    public static final String FIELD__ID = "id"; 
    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */

    @Override
    public String createString() {
        final String myid = String.valueOf(cidsBean.getProperty(FIELD__ID));
        if ("-1".equals(myid)) {
            return "neue Kita anlegen";
        }
        String mystrasse;
        final String myname;
        mystrasse = String.valueOf(cidsBean.getProperty(FIELD__ADRESSE));
        mystrasse = mystrasse.trim();
        myname = String.valueOf(cidsBean.getProperty(FIELD__NAME));

        return myname + ", " + mystrasse;
    }
}
