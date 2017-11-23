/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.tostringconverter.wunda_blau;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import de.cismet.cids.tools.CustomToStringConverter;

/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
public class BillingBillingToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    //~ Methods ----------------------------------------------------------------
    
    public static String createString(final String geschaeftsbuchnummer, final String kundenname, final String username, final Date angelegt) {
        final StringBuilder sb = new StringBuilder();
        if (kundenname == null || kundenname.isEmpty()) {
            sb.append((username == null) ? "kein Benutzername" : username);
        } else {
            sb.append(kundenname);
        }
        sb.append(" - ");
        sb.append((geschaeftsbuchnummer == null) ? "keine Geschäftsbuchnummer angegeben" : geschaeftsbuchnummer);
        sb.append(" - ");
        sb.append(DATE_FORMAT.format(angelegt));

        return sb.toString();
    }

    @Override
    public String createString() {
        final String geschaeftsbuchnummer = (String)cidsBean.getProperty("geschaeftsbuchnummer");
        final String kundenname = (String)cidsBean.getProperty("angelegt_durch.kunde.name");
        final String username = (String)cidsBean.getProperty("username");
        final Date angelegt = (Date)cidsBean.getProperty("ts");

        return createString(geschaeftsbuchnummer, kundenname, username, angelegt);
    }
}
