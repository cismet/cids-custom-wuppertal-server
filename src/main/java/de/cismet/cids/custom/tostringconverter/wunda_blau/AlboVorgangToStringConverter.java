/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * AdressStringConverter.java
 *
 * Created on 11. Mai 2004, 13:31
 *test
 */
package de.cismet.cids.custom.tostringconverter.wunda_blau;
import de.cismet.cids.tools.CustomToStringConverter;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class AlboVorgangToStringConverter extends CustomToStringConverter {

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        final String schluessel = (String)cidsBean.getProperty("schluessel");
        return String.format("Vorgang: %s", schluessel);
    }
}
