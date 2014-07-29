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
package de.cismet.cids.custom.utils.nas;

import java.io.Serializable;

import java.util.Map;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class NasProduct implements Serializable {

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Format {

        //~ Enum constants -----------------------------------------------------

        DXF, NAS
    }

    //~ Instance fields --------------------------------------------------------

    public String key;
    public String format;
    private String displayName;
    private String billingKey;
    private String template;
    private Map<String, String> params;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NasProduct object.
     */
    public NasProduct() {
    }

    /**
     * Creates a new NasProduct object.
     *
     * @param  key  DOCUMENT ME!
     */
    public NasProduct(final String key) {
        this.key = key;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getKey() {
        return key;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  key  DOCUMENT ME!
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getBillingKey() {
        return billingKey;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getTemplate() {
        return template;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getFormat() {
        return format;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  format  DOCUMENT ME!
     */
    public void setFormat(final String format) {
        this.format = format;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Map<String, String> getParams() {
        return params;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  params  DOCUMENT ME!
     */
    public void setParams(final Map<String, String> params) {
        this.params = params;
    }
}
