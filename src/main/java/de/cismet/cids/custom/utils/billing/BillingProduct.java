/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.billing;

import java.io.Serializable;

import java.util.HashMap;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */

public class BillingProduct implements Serializable {

    //~ Instance fields --------------------------------------------------------

    private String id;
    private String name;
    private String description;
    private Double mwst;
    private final HashMap<String, Double> prices = new HashMap<>();
    private final HashMap<String, Double> discounts = new HashMap<>();

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
    public HashMap<String, Double> getDiscounts() {
        return discounts;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getId() {
        return id;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  id  DOCUMENT ME!
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getName() {
        return name;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  name  DOCUMENT ME!
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Double getMwst() {
        return mwst;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  mwst  DOCUMENT ME!
     */
    public void setMwst(final Double mwst) {
        this.mwst = mwst;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  pricegroup  DOCUMENT ME!
     * @param  price       DOCUMENT ME!
     */
    public void addPrice(final String pricegroup, final double price) {
        prices.put(pricegroup, price);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  usagekey  DOCUMENT ME!
     * @param  factor    DOCUMENT ME!
     */
    public void addDiscount(final String usagekey, final double factor) {
        discounts.put(usagekey, factor);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public HashMap<String, Double> getPrices() {
        return prices;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   obj  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BillingProduct other = (BillingProduct)obj;
        if ((this.id == null) ? (other.id != null) : (!this.id.equals(other.id))) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : (!this.name.equals(other.name))) {
            return false;
        }
        if ((this.description == null) ? (other.description != null) : (!this.description.equals(other.description))) {
            return false;
        }
        if ((this.discounts != other.discounts)
                    && ((this.discounts == null) || !this.discounts.equals(other.discounts))) {
            return false;
        }
        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = (59 * hash) + ((this.id != null) ? this.id.hashCode() : 0);
        hash = (59 * hash) + ((this.name != null) ? this.name.hashCode() : 0);
        hash = (59 * hash) + ((this.description != null) ? this.description.hashCode() : 0);
        hash = (59 * hash) + ((this.discounts != null) ? this.discounts.hashCode() : 0);
        return hash;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   args  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */

}
