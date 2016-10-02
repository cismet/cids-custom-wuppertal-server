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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

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
     * @return  DOCUMENT ME!
     */
    private AlkisProductDescription determineProduct() {
        // TODO algorithmus für bestes format

        final String clazz = String.valueOf("WUP-Kommunal");
        final String type = String.valueOf("AP-Übersicht");
        final String scale = String.valueOf(2500);
        final String layout = String.valueOf("DINA4 Querformat");
        for (final AlkisProductDescription product : AlkisProducts.getInstance().ALKIS_MAP_PRODUCTS) {
            if (clazz.equals(product.getClazz()) && type.equals(product.getType())
                        && scale.equals(product.getMassstab()) && layout.equals(product.getDinFormat())) {
                return product;
            }
        }
        return null;
    }

    @Override
    public void performTask() throws Exception {
        final GeometryFactory geometryFactory = new GeometryFactory();
        final Collection<Geometry> geometries = new ArrayList<Geometry>(getAlkisPoints().size());
        for (final CidsBean alkisPoint : getAlkisPoints()) {
            final Geometry geom = (Geometry)alkisPoint.getProperty("geom.geo_field");
            geometries.add(geom);
        }
        final Geometry envelope = geometryFactory.createGeometryCollection(geometries.toArray(new Geometry[0]))
                    .getEnvelope();

        final Point center = envelope.getCentroid();

        final String landparcelcode = (String)flurstuecke.iterator().next().getProperty("alkis_id");
        final AlkisProductDescription product = determineProduct();

        final URL url = AlkisProducts.getInstance()
                    .productKarteUrl(
                        landparcelcode,
                        product,
                        Double.valueOf(0).intValue(),
                        Double.valueOf(center.getX()).intValue(),
                        Double.valueOf(center.getY()).intValue(),
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

    /**
     * DOCUMENT ME!
     *
     * @param   alkisPoints  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String getPunktlistenStringForChosenPoints(final Collection<CidsBean> alkisPoints) {
        final StringBuffer punktListeString = new StringBuffer();

        for (final CidsBean alkisPoint : alkisPoints) {
            if (punktListeString.length() > 0) {
                punktListeString.append(",");
            }
            punktListeString.append(PRODUCTS.getPointDataForProduct(alkisPoint));
        }

        return punktListeString.toString();
    }
}
