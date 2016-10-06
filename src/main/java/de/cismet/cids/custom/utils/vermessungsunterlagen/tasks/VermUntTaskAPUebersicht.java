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
package de.cismet.cids.custom.utils.vermessungsunterlagen.tasks;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.custom.utils.alkis.AlkisProductDescription;
import de.cismet.cids.custom.utils.alkis.AlkisProducts;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;

import de.cismet.cids.dynamics.CidsBean;

import static de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper.doGetRequest;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskAPUebersicht extends VermUntTaskAP {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "AP_Uebersicht";
    private static final AlkisProducts PRODUCTS = AlkisProducts.getInstance();

    //~ Instance fields --------------------------------------------------------

    private final String auftragsnummer;

    private final Collection<CidsBean> flurstuecke;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskAPList object.
     *
     * @param  jobkey          DOCUMENT ME!
     * @param  alkisPoints     DOCUMENT ME!
     * @param  flurstuecke     DOCUMENT ME!
     * @param  auftragsnummer  DOCUMENT ME!
     */
    public VermUntTaskAPUebersicht(final String jobkey,
            final Collection<CidsBean> alkisPoints,
            final Collection<CidsBean> flurstuecke,
            final String auftragsnummer) {
        super(TYPE, jobkey, alkisPoints);

        this.flurstuecke = flurstuecke;
        this.auftragsnummer = auftragsnummer;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   box     DOCUMENT ME!
     * @param   width   DOCUMENT ME!
     * @param   height  DOCUMENT ME!
     * @param   scale   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean doesBoundingBoxFitIntoLayout(final Envelope box,
            final int width,
            final int height,
            final double scale) {
        final double realWorldLayoutWidth = ((double)width) / 1000.0d * scale;
        final double realWorldLayoutHeigth = ((double)height) / 1000.0d * scale;
        return (realWorldLayoutWidth >= box.getWidth()) && (realWorldLayoutHeigth >= box.getHeight());
    }
    /**
     * DOCUMENT ME!
     *
     * @param   boundingBox  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private AlkisProductDescription determineProduct(final Envelope boundingBox) {
        final String clazz = String.valueOf("WUP-Kommunal");
        final String type = String.valueOf("AP-Übersicht");
        AlkisProductDescription minimalWidthFittingProduct = null;
        AlkisProductDescription minimalHeightFittingProduct = null;
        AlkisProductDescription defaultProduct = null;
        for (final AlkisProductDescription product : AlkisProducts.getInstance().ALKIS_MAP_PRODUCTS) {
            if (clazz.equals(product.getClazz()) && type.equals(product.getType())) {
                if (product.isDefaultProduct()) {
                    defaultProduct = product;
                }
                final boolean fitting = doesBoundingBoxFitIntoLayout(
                        boundingBox,
                        product.getWidth(),
                        product.getHeight(),
                        Integer.parseInt(String.valueOf(product.getMassstab())));
                if (fitting) {
                    if (minimalWidthFittingProduct == null) {
                        // at least the first is the minimal
                        minimalWidthFittingProduct = product;
                    } else if (product.getWidth() <= minimalWidthFittingProduct.getWidth()) {
                        // is smaller or equals
                        if (product.getWidth() < minimalWidthFittingProduct.getWidth()) {
                            // is smaller
                            minimalWidthFittingProduct = product;
                        } else if (Integer.parseInt(String.valueOf(product.getMassstab()))
                                    < Integer.parseInt(String.valueOf(minimalHeightFittingProduct.getMassstab()))) {
                            // not smaller (equals) in size but in scale
                            minimalWidthFittingProduct = product;
                        }
                    }
                    // same as for width but now with height
                    if (minimalHeightFittingProduct == null) {
                        minimalHeightFittingProduct = product;
                    } else if (product.getHeight() <= minimalHeightFittingProduct.getHeight()) {
                        if (product.getHeight() < minimalHeightFittingProduct.getHeight()) {
                            minimalHeightFittingProduct = product;
                        } else if (Integer.parseInt(String.valueOf(product.getMassstab()))
                                    < Integer.parseInt(String.valueOf(minimalHeightFittingProduct.getMassstab()))) {
                            minimalHeightFittingProduct = product;
                        }
                    }
                }
            }
        }

        if ((minimalWidthFittingProduct != null) && (minimalHeightFittingProduct != null)) {
            final boolean isMinimalWidthHoch = minimalWidthFittingProduct.getWidth()
                        < minimalWidthFittingProduct.getHeight();
            final boolean isMinimalHeightHoch = minimalHeightFittingProduct.getWidth()
                        < minimalHeightFittingProduct.getHeight();

            // hochkannt priorisieren
            if (isMinimalWidthHoch && isMinimalHeightHoch) {
                return minimalWidthFittingProduct;
            } else if (isMinimalWidthHoch) {
                return minimalWidthFittingProduct;
            } else {
                return minimalHeightFittingProduct;
            }
        } else if (minimalWidthFittingProduct != null) {
            return minimalWidthFittingProduct;
        } else if (minimalHeightFittingProduct != null) {
            return minimalHeightFittingProduct;
        } else {
            return defaultProduct;
        }
    }

    @Override
    public void performTask() throws Exception {
        final GeometryFactory geometryFactory = new GeometryFactory();
        final Collection<Geometry> geometries = new ArrayList<Geometry>(getAlkisPoints().size());
        for (final CidsBean alkisPoint : getAlkisPoints()) {
            final Geometry geom = (Geometry)alkisPoint.getProperty("geom.geo_field");
            geometries.add(geom);
        }
        final Envelope envelope = geometryFactory.createGeometryCollection(geometries.toArray(new Geometry[0]))
                    .getEnvelopeInternal();

        final Coordinate center = envelope.centre();

        final String landparcelcode = (String)flurstuecke.iterator().next().getProperty("alkis_id");
        final AlkisProductDescription product = determineProduct(envelope);

        final URL url = AlkisProducts.getInstance()
                    .productKarteUrl(
                        landparcelcode,
                        product,
                        Double.valueOf(0).intValue(),
                        Double.valueOf(center.x).intValue(),
                        Double.valueOf(center.y).intValue(),
                        "",
                        auftragsnummer,
                        false,
                        null);

        final String filename = product.getCode() + "." + landparcelcode.replace("/", "--")
                    + ((flurstuecke.size() > 1) ? ".ua" : "") + ".pdf";

        InputStream in = null;
        OutputStream out = null;
        try {
            in = doGetRequest(url);
            out = new FileOutputStream(getPath() + "/" + filename);
            VermessungsunterlagenHelper.downloadStream(in, out);
        } finally {
            VermessungsunterlagenHelper.closeStream(in);
            VermessungsunterlagenHelper.closeStream(out);
        }
    }
}
