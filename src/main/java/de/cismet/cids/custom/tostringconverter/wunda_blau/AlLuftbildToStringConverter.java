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

import java.text.SimpleDateFormat;

import java.util.Date;

import de.cismet.cids.tools.CustomToStringConverter;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
public class AlLuftbildToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    public static final String FIELD__NAME = "dateiname";
    public static final String FIELD__DATUM = "datum";
    public static final String FIELD__FLUG = "flugnummer";

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        // Date datum = (Date)(cidsBean.getProperty(FIELD__DATUM));
        final String datum = String.valueOf(cidsBean.getProperty(FIELD__DATUM));
        final String flug = String.valueOf(cidsBean.getProperty(FIELD__FLUG));
        final String name = String.valueOf(cidsBean.getProperty(FIELD__NAME));
        return String.format(
                "%s -- %s -- %s",
                datum, // new SimpleDateFormat("dd.MM.yyyy").format(datum),
                flug,
                name);
    }
}
