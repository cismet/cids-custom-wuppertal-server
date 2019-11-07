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
public class BillingProductGroupAmount {

    //~ Instance fields --------------------------------------------------------

    private String group;
    private int amount;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ProductGroupAmount object.
     */
    public BillingProductGroupAmount() {
    }

    /**
     * Creates a new ProductGroupAmount object.
     *
     * @param  group   DOCUMENT ME!
     * @param  amount  DOCUMENT ME!
     */
    public BillingProductGroupAmount(final String group, final int amount) {
        this.group = group;
        this.amount = amount;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public int getAmount() {
        return amount;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  amount  DOCUMENT ME!
     */
    public void setAmount(final int amount) {
        this.amount = amount;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getGroup() {
        return group;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  group  DOCUMENT ME!
     */
    public void setGroup(final String group) {
        this.group = group;
    }
}
