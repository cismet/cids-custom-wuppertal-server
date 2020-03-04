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
public class StrAdrGeplanteAdresseToStringConverter extends CustomToStringConverter {
    public static final String FIELD__NAME = "fk_strasse_id.name";
    public static final String FIELD__ADRESSZUSATZ = "adr_zusatz";
    public static final String FIELD__HNR = "hausnr";                               
    public static final String FIELD__ID = "id"; 

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String myString() {
        String myadress;
        final String myname = String.valueOf(cidsBean.getProperty(FIELD__NAME));
        final String hausnr = String.valueOf(cidsBean.getProperty(FIELD__HNR));
        final String adr_zusatz = String.valueOf(cidsBean.getProperty(FIELD__ADRESSZUSATZ));
        if ( "null".equals(adr_zusatz)) {
            myadress = myname + " " + hausnr;
        } else {
            myadress = myname + " " + hausnr + " " + adr_zusatz.trim();
        }

        return myadress;
    }

    @Override
    public String createString() {final String myid = String.valueOf(cidsBean.getProperty(FIELD__ID));
        if ("-1".equals(myid)) {
            return "neue Adresse anlegen";
        }

        return myString();
    }
}
