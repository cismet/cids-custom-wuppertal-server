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
public class BparkZoneToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    public static final String FIELD__ZONE = "zone";                 
    public static final String FIELD__NR = "nummer"; 

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        final Integer myid = cidsBean.getPrimaryKeyValue();
        if (myid < 0) {
            return "Neuen Bereich anlegen";
        } else {
            return (cidsBean.getProperty(FIELD__NR) == null 
                        || cidsBean.getProperty(FIELD__NR).toString().trim().length() == 0)
                    ? String.format("%s", cidsBean.getProperty(FIELD__ZONE).toString()) 
                    : String.format("%s - %s",cidsBean.getProperty(FIELD__ZONE).toString(), cidsBean.getProperty(FIELD__NR).toString());
        }
    }
}
