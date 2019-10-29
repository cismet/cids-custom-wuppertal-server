/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.billing;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
public class BillingInfo {

    //~ Instance fields --------------------------------------------------------

    private ArrayList<BillingModus> modi = new ArrayList<>();
    private ArrayList<BillingProductGroup> productGroups = new ArrayList<>();
    private ArrayList<BillingUsage> usages = new ArrayList<>();
    private ArrayList<BillingProduct> products = new ArrayList<>();

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ArrayList<BillingModus> getModi() {
        return modi;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  modi  DOCUMENT ME!
     */
    public void setModi(final ArrayList<BillingModus> modi) {
        this.modi = modi;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ArrayList<BillingProductGroup> getProductGroups() {
        return productGroups;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  productGroups  DOCUMENT ME!
     */
    public void setProductGroups(final ArrayList<BillingProductGroup> productGroups) {
        this.productGroups = productGroups;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ArrayList<BillingUsage> getUsages() {
        return usages;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  usages  DOCUMENT ME!
     */
    public void setUsages(final ArrayList<BillingUsage> usages) {
        this.usages = usages;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ArrayList<BillingProduct> getProducts() {
        return products;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  products  DOCUMENT ME!
     */
    public void setProducts(final ArrayList<BillingProduct> products) {
        this.products = products;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   args  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static void main(final String[] args) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final BillingProduct p = new BillingProduct();
        p.setId("fsnw");
        p.setName("Flurstücksnachweis");
        p.setDescription("none");

        p.addPrice("ea", 10.0);
        p.addDiscount("VU aL", 0.0);
        p.addDiscount("VU hV", 0.0);
        p.addDiscount("VU s", 0.75);
        p.addDiscount("eigG", 0.75);

        final BillingProduct p2 = new BillingProduct();
        p2.setId("fsuenw");
        p2.setName("Flurstücks- und Eigentumsnachweis (NRW)");
        p2.setDescription("none");

        p2.addPrice("ea", 10.0);
        p2.addDiscount("VU aL", 0.0);
        p2.addDiscount("VU hV", 0.0);
        p2.addDiscount("VU s", 0.75);
        p2.addDiscount("eigG", 0.75);

        final BillingInfo bi = new BillingInfo();
        bi.getUsages().add(new BillingUsage("VU aL", "Vermessungs-unterlagen (amtlicher Lageplan TS 3)", "-"));
        bi.getUsages().add(new BillingUsage("VU hV", "Vermessungs-unterlagen (hoheitliche Vermessung TS 4)", "-"));
        bi.getUsages().add(new BillingUsage("VU s", "Vermessungs-unterlagen (sonstige)", "-"));
        bi.getUsages().add(new BillingUsage("eigG", "eigener Gebrauch (einmalig)", "-"));
        bi.getProducts().add(p);
        bi.getProducts().add(p2);

        bi.getProductGroups().add(new BillingProductGroup("ea", "Stück"));

        final String s = mapper.writeValueAsString(bi);

        final BillingInfo tester = mapper.readValue(BillingInfo.class.getResourceAsStream(
                    "/de/cismet/cids/custom/billing/billing.json"),
                BillingInfo.class);
        System.out.println(tester.getProducts().get(0).getId() + " mwst: " + tester.getProducts().get(0).getMwst());
    }
}
