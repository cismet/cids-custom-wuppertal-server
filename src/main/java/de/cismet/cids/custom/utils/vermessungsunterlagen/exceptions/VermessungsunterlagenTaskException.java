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
public class VermessungsunterlagenTaskException extends VermessungsunterlagenException {

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum TaskExceptionType {

        //~ Enum constants -----------------------------------------------------

        SINGLE("SINGLE"), RETRY("RETRY");

        //~ Instance fields ----------------------------------------------------

        private final String taskExceptionType;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Type object.
         *
         * @param  type  DOCUMENT ME!
         */
        private TaskExceptionType(final String type) {
            this.taskExceptionType = type;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString() {
            return taskExceptionType;
        }
    }

    //~ Instance fields --------------------------------------------------------

    @JsonProperty @Getter private final String task;
    @JsonProperty @Getter private final TaskExceptionType taskExceptionType;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenTaskException object.
     *
     * @param  task     DOCUMENT ME!
     * @param  message  DOCUMENT ME!
     */
    public VermessungsunterlagenTaskException(final String task, final String message) {
        this(task, message, null);
    }

    /**
     * Creates a new VermessungsunterlagenTaskException object.
     *
     * @param  task     DOCUMENT ME!
     * @param  message  DOCUMENT ME!
     * @param  cause    DOCUMENT ME!
     */
    public VermessungsunterlagenTaskException(final String task, final String message, final Exception cause) {
        this(TaskExceptionType.SINGLE, task, message, cause, System.currentTimeMillis());
    }

    /**
     * Creates a new VermessungsunterlagenTaskException object.
     *
     * @param  taskExceptionType  DOCUMENT ME!
     * @param  task               DOCUMENT ME!
     * @param  message            DOCUMENT ME!
     * @param  cause              DOCUMENT ME!
     * @param  timeMillis         DOCUMENT ME!
     */
    @JsonCreator
    public VermessungsunterlagenTaskException(
            @JsonProperty("taskExceptionType") final TaskExceptionType taskExceptionType,
            @JsonProperty("task") final String task,
            @JsonProperty("message") final String message,
            @JsonProperty("cause") final Exception cause,
            @JsonProperty("timeMillis") final double timeMillis) {
        super(Type.TASK, message, cause, timeMillis);
        this.task = task;
        this.taskExceptionType = taskExceptionType;
    }
}
