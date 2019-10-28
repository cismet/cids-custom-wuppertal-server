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
package de.cismet.cids.custom.utils.billing;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
@AllArgsConstructor
public class BillingPrice {

    //~ Instance fields --------------------------------------------------------

    private final double raw;
    private final String usage;
    private final BillingProduct product;

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private double getDiscountFrom1() {
        return product.getDiscounts().get(usage);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public double getDiscountAbsolute() {
        return (1.0 - getDiscountFrom1()) * raw;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public double getDiscountPercentage() {
        return Math.round((1.0 - getDiscountFrom1()) * 100);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public double getNetto() {
        return raw * getDiscountFrom1();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public double getMwst() {
        return getNetto() * (product.getMwst() / 100);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public double getBrutto() {
        return Math.round((getNetto() + getMwst()) * 100) / 100.;
    }
}
