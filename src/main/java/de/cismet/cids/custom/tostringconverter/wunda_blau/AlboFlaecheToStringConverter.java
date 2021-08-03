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
public class AlboFlaecheToStringConverter extends CustomToStringConverter {

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        final String erhebungsnummer = (String)cidsBean.getProperty("erhebungsnummer");
        return Boolean.TRUE.equals(cidsBean.getProperty("loeschen"))
            ? String.format("<html><body>Fläche: <strike>%s</strike>", erhebungsnummer)
            : String.format("Fläche: %s", erhebungsnummer);
    }
}
