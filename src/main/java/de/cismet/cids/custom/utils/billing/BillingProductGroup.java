/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.billing;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
public class BillingProductGroup {

    //~ Instance fields --------------------------------------------------------

    private String key;
    private String description;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ProductGroup object.
     */
    public BillingProductGroup() {
    }

    /**
     * Creates a new ProductGroup object.
     *
     * @param  key          DOCUMENT ME!
     * @param  description  DOCUMENT ME!
     */
    public BillingProductGroup(final String key, final String description) {
        this.key = key;
        this.description = description;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getDescription() {
        return description;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  description  DOCUMENT ME!
     */
    public void setDescription(final String description) {
        this.description = description;
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
}
