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

import java.io.Serializable;

/**
 * DOCUMENT ME!
 *
 * @param    <I>
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public interface StorableSearch<I extends StorableSearch.Info> {

    //~ Methods ----------------------------------------------------------------

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
    I getSearchInfo();

    /**
     * DOCUMENT ME!
     *
     * @param  searchInfo  DOCUMENT ME!
     */
    void setSearchInfo(final I searchInfo);

    /**
     * DOCUMENT ME!
     *
     * @param  searchInfo  DOCUMENT ME!
     */
    void setSearchInfo(final Object searchInfo); // jalopy wants this method. workaround

    //~ Inner Classes ----------------------------------------------------------

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
    public abstract static class Info implements Serializable {
    }
}
