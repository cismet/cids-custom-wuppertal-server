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

import lombok.Getter;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenJobException extends VermessungsunterlagenException {

    //~ Instance fields --------------------------------------------------------

    @JsonProperty @Getter private final VermessungsunterlagenTaskException cause;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenJobException object.
     *
     * @param  message  DOCUMENT ME!
     */
    public VermessungsunterlagenJobException(final String message) {
        this(message, null);
    }

    /**
     * Creates a new VermessungsunterlagenJobException object.
     *
     * @param  message  DOCUMENT ME!
     * @param  cause    DOCUMENT ME!
     */
    public VermessungsunterlagenJobException(final String message, final VermessungsunterlagenTaskException cause) {
        this(message, cause, System.currentTimeMillis());
    }

    /**
     * Creates a new VermessungsunterlagenJobException object.
     *
     * @param  message     DOCUMENT ME!
     * @param  cause       DOCUMENT ME!
     * @param  timeMillis  DOCUMENT ME!
     */
    @JsonCreator
    public VermessungsunterlagenJobException(@JsonProperty("message") final String message,
            @JsonProperty("cause") final VermessungsunterlagenTaskException cause,
            @JsonProperty("timeMillis") final double timeMillis) {
        super(Type.JOB, message, cause, timeMillis);

        this.cause = cause;
    }
}
