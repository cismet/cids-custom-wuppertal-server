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
    private String permissionNeeded;
    private String templateContent;
    private String server;
    private Map<String, Object> params;

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
     * @return  the templateContent
     */
    public String getTemplateContent() {
        return templateContent;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  templateContent  the templateContent to set
     */
    public void setTemplateContent(final String templateContent) {
        this.templateContent = templateContent;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  the server
     */
    public String getServer() {
        return server;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  server  the server to set
     */
    public void setServer(final String server) {
        this.server = server;
    }

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
     * @param  displayName  DOCUMENT ME!
     */
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  billingKey  DOCUMENT ME!
     */
    public void setBillingKey(final String billingKey) {
        this.billingKey = billingKey;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  template  DOCUMENT ME!
     */
    public void setTemplate(final String template) {
        this.template = template;
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
        if (displayName == null) {
            return super.toString();
        }
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
    public Map<String, Object> getParams() {
        return params;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  params  DOCUMENT ME!
     */
    public void setParams(final Map<String, Object> params) {
        this.params = params;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getPermissionNeeded() {
        return permissionNeeded;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  permissionNeeded  DOCUMENT ME!
     */
    public void setPermissionNeeded(final String permissionNeeded) {
        this.permissionNeeded = permissionNeeded;
    }
}
