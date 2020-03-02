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
 * @author   lat-lon
 * @version  $Revision$, $Date$
 */
public class InspireVersionInfraKitaToStringConverter extends CustomToStringConverter {
    public static final String FIELD__REF = "infra_kita_reference";
    public static final String FIELD__NR = "versionnr";                               
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
            return "neue Kita-Version anlegen";
        }
        final String mykita = String.valueOf(cidsBean.getProperty(FIELD__REF));
        final String myversion = String.valueOf(cidsBean.getProperty(FIELD__NR));

        return mykita + ", " + myversion;
    }
}
