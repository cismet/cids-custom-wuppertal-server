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
package de.cismet.cids.custom.wunda_blau.search.server;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

/**
 * DOCUMENT ME!
 *
 * @param    <I>
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public interface StorableSearch<I extends StorableSearch.Configuration> {

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    String getName();

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    String createQuery();

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    ObjectMapper getConfigurationMapper();

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    I getConfiguration();

    /**
     * DOCUMENT ME!
     *
     * @param  configuration  DOCUMENT ME!
     */
    void setConfiguration(final I configuration);

    /**
     * DOCUMENT ME!
     *
     * @param  configuration  DOCUMENT ME!
     */
    void setConfiguration(final Object configuration); // jalopy wants this method. workaround

    /**
     * DOCUMENT ME!
     *
     * @param   configurationJson  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    void setConfiguration(final String configurationJson) throws Exception;

    //~ Inner Interfaces -------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public interface Configuration extends Serializable {
    }
}
