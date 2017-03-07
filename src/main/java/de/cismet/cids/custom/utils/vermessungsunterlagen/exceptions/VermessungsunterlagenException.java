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
public class VermessungsunterlagenException extends Exception {

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Type {

        //~ Enum constants -----------------------------------------------------

        VALIDATOR("VALIDATOR"), JOB("JOB"), TASK("TASK"), OTHER("OTHER");

        //~ Instance fields ----------------------------------------------------

        private final String type;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Type object.
         *
         * @param  type  DOCUMENT ME!
         */
        private Type(final String type) {
            this.type = type;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString() {
            return type;
        }
    }

    //~ Instance fields --------------------------------------------------------

    @JsonProperty @Getter private final Type type;
    @JsonProperty @Getter private final double timeMillis;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenException object.
     *
     * @param  message  DOCUMENT ME!
     * @param  cause    DOCUMENT ME!
     */
    public VermessungsunterlagenException(final String message, final Exception cause) {
        this(Type.OTHER, message, cause, System.currentTimeMillis());
    }

    /**
     * Creates a new VermessungsunterlagenException object.
     *
     * @param  type        DOCUMENT ME!
     * @param  message     DOCUMENT ME!
     * @param  cause       DOCUMENT ME!
     * @param  timeMillis  DOCUMENT ME!
     */
    @JsonCreator
    public VermessungsunterlagenException(@JsonProperty("type") final Type type,
            @JsonProperty("message") final String message,
            @JsonProperty("cause") final Exception cause,
            @JsonProperty("timeMillis") final double timeMillis) {
        super(message, cause);

        this.type = type;
        this.timeMillis = timeMillis;
    }
}
