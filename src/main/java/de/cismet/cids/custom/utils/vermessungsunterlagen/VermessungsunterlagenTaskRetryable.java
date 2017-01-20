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
public interface VermessungsunterlagenTaskRetryable {

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    long getMaxTotalWaitTimeMs();

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    long getFirstWaitTimeMs();

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    double getWaitTimeMultiplicator();
}
