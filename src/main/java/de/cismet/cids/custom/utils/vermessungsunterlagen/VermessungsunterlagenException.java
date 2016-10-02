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
package de.cismet.cids.custom.utils.vermessungsunterlagen;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenException extends Exception {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenException object.
     *
     * @param  message  DOCUMENT ME!
     */
    public VermessungsunterlagenException(final String message) {
        super(message);
    }

    /**
     * Creates a new VermessungsunterlagenException object.
     *
     * @param  message  DOCUMENT ME!
     * @param  cause    DOCUMENT ME!
     */
    public VermessungsunterlagenException(final String message, final Exception cause) {
        super(message, cause);
    }
}
