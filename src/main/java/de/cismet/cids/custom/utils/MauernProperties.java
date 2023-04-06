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
package de.cismet.cids.custom.utils;

import lombok.Getter;

import de.cismet.cids.utils.serverresources.DefaultServerResourcePropertiesHandler;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class MauernProperties extends DefaultServerResourcePropertiesHandler {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(MauernProperties.class);

    private static final String PROP__RASTERFARI_URL = "rasterfariUrl";
    private static final String PROP__WEBDAV_URL = "webdavUrl";
    private static final String PROP__WEBDAV_USER = "webdavUser";
    private static final String PROP__WEBDAV_PASSWORD = "webdavPassword";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MauernProperties object.
     */
    public MauernProperties() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getRasterfariUrl() {
        return getProperties().getProperty(PROP__RASTERFARI_URL, null);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getWebdavUrl() {
        return getProperties().getProperty(PROP__WEBDAV_URL, null);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getWebdavUser() {
        return getProperties().getProperty(PROP__WEBDAV_USER, null);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getWebdavPassword() {
        return getProperties().getProperty(PROP__WEBDAV_PASSWORD, null);
    }
}
