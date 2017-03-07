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
package de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenValidatorException extends VermessungsunterlagenException {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenValidatorException object.
     *
     * @param  message  DOCUMENT ME!
     */
    public VermessungsunterlagenValidatorException(final String message) {
        this(message, null);
    }

    /**
     * Creates a new VermessungsunterlagenValidatorException object.
     *
     * @param  message  DOCUMENT ME!
     * @param  cause    DOCUMENT ME!
     */
    public VermessungsunterlagenValidatorException(final String message, final Exception cause) {
        this(message, cause, System.currentTimeMillis());
    }

    /**
     * Creates a new VermessungsunterlagenValidatorException object.
     *
     * @param  message     DOCUMENT ME!
     * @param  cause       DOCUMENT ME!
     * @param  timeMillis  DOCUMENT ME!
     */
    @JsonCreator
    public VermessungsunterlagenValidatorException(@JsonProperty("message") final String message,
            @JsonProperty("cause") final Exception cause,
            @JsonProperty("timeMillis") final double timeMillis) {
        super(Type.VALIDATOR, message, cause, timeMillis);
    }
}
