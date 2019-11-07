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

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class BillingInfoHandler {

    //~ Instance fields --------------------------------------------------------

    private final HashMap<String, BillingModus> modi = new HashMap<>();
    private final HashMap<String, BillingProduct> products = new HashMap<>();
    private final HashMap<String, BillingUsage> usages = new HashMap<>();
    private final HashMap<String, BillingProductGroup> productGroups = new HashMap<>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BillingInfoHandler object.
     *
     * @param  billingInfo  DOCUMENT ME!
     */
    public BillingInfoHandler(final BillingInfo billingInfo) {
        final ArrayList<BillingModus> lm = billingInfo.getModi();
        for (final BillingModus m : lm) {
            modi.put(m.getKey(), m);
        }
        final ArrayList<BillingProduct> lp = billingInfo.getProducts();
        for (final BillingProduct p : lp) {
            products.put(p.getId(), p);
        }
        final ArrayList<BillingUsage> lu = billingInfo.getUsages();
        for (final BillingUsage u : lu) {
            usages.put(u.getKey(), u);
        }
        final ArrayList<BillingProductGroup> lpg = billingInfo.getProductGroups();
        for (final BillingProductGroup pg : lpg) {
            productGroups.put(pg.getKey(), pg);
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!modus.
     *
     * @param   product  DOCUMENT ME!
     * @param   amounts  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static double calculateRawPrice(final BillingProduct product, final BillingProductGroupAmount... amounts) {
        double price = 0;
        for (final BillingProductGroupAmount pga : amounts) {
            price += ((double)pga.getAmount()) * product.getPrices().get(pga.getGroup());
        }
        return price;
    }
}
