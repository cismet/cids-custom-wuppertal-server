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

import java.util.List;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenTaskRetryException extends VermessungsunterlagenTaskException {

    //~ Instance fields --------------------------------------------------------

    @JsonProperty @Getter private final List<VermessungsunterlagenException> exceptions;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenTaskRetryException object.
     *
     * @param  task        DOCUMENT ME!
     * @param  message     DOCUMENT ME!
     * @param  exceptions  DOCUMENT ME!
     */
    public VermessungsunterlagenTaskRetryException(final String task,
            final String message,
            final List<VermessungsunterlagenException> exceptions) {
        this(task, message, exceptions, null, System.currentTimeMillis());
    }

    /**
     * Creates a new VermessungsunterlagenMultipleRetryException object.
     *
     * @param  task        DOCUMENT ME!
     * @param  message     DOCUMENT ME!
     * @param  exceptions  DOCUMENT ME!
     * @param  cause       DOCUMENT ME!
     * @param  timeMillis  DOCUMENT ME!
     */
    @JsonCreator
    public VermessungsunterlagenTaskRetryException(@JsonProperty("task") final String task,
            @JsonProperty("message") final String message,
            @JsonProperty("exceptions") final List<VermessungsunterlagenException> exceptions,
            @JsonProperty("cause") final Exception cause,
            @JsonProperty("timeMillis") final double timeMillis) {
        super(TaskExceptionType.RETRY, task, message, cause, timeMillis);
        this.exceptions = exceptions;
    }
}
