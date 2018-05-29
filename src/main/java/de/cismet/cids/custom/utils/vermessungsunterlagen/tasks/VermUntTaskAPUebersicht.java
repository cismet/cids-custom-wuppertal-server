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
import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenTaskException;

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

    @Override
    public void performTask() throws VermessungsunterlagenTaskException {
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
        final AlkisProductDescription product = VermessungsunterlagenHelper.determineAlkisProduct(String.valueOf(
                    "WUP-Kommunal"),
                String.valueOf("AP-Übersicht"),
                envelope);

        InputStream in = null;
        OutputStream out = null;
        try {
            final URL url = ServerAlkisProducts.productKarteUrl(
                            landparcelcode,
                            product.getCode(),
                            Double.valueOf(0).intValue(),
                            Double.valueOf(center.x).intValue(),
                            Double.valueOf(center.y).intValue(),
                            product.getMassstabMin(),
                            product.getMassstabMax(),
                            "",
                            auftragsnummer,
                            false,
                            null);

            final String filename = product.getCode() + "." + landparcelcode.replace("/", "--")
                        + ((flurstuecke.size() > 1) ? ".ua" : "") + ".pdf";

            in = doGetRequest(url);
            out = new FileOutputStream(getPath() + "/" + filename);
            VermessungsunterlagenHelper.downloadStream(in, out);
        } catch (final Exception ex) {
            final String message = "Beim Herunterladen der AP-Karten-Übersicht kam es zu einem unerwarteten Fehler.";
            throw new VermessungsunterlagenTaskException(getType(), message, ex);
        } finally {
            VermessungsunterlagenHelper.closeStream(in);
            VermessungsunterlagenHelper.closeStream(out);
        }
    }
}
