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

import de.cismet.cids.dynamics.CidsBean;

import static de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper.doGetRequest;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskNivPUebersicht extends VermUntTaskNivP {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "NivP_Uebersicht";

    //~ Instance fields --------------------------------------------------------

    private final String auftragsnummer;

    private final Collection<CidsBean> flurstuecke;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskAPList object.
     *
     * @param  jobkey          DOCUMENT ME!
     * @param  nivPoints       DOCUMENT ME!
     * @param  flurstuecke     DOCUMENT ME!
     * @param  auftragsnummer  DOCUMENT ME!
     */
    public VermUntTaskNivPUebersicht(final String jobkey,
            final Collection<CidsBean> nivPoints,
            final Collection<CidsBean> flurstuecke,
            final String auftragsnummer) {
        super(TYPE, jobkey, nivPoints);

        this.flurstuecke = flurstuecke;
        this.auftragsnummer = auftragsnummer;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void performTask() throws Exception {
        final GeometryFactory geometryFactory = new GeometryFactory();
        final Collection<Geometry> geometries = new ArrayList<Geometry>(getNivPoints().size());
        for (final CidsBean nivPoint : getNivPoints()) {
            final Geometry geom = (Geometry)nivPoint.getProperty("geometrie.geo_field");
            geometries.add(geom);
        }
        final Envelope envelope = geometryFactory.createGeometryCollection(geometries.toArray(new Geometry[0]))
                    .getEnvelopeInternal();
        final Coordinate center = envelope.centre();

        final String landparcelcode = (String)flurstuecke.iterator().next().getProperty("alkis_id");
        final AlkisProductDescription product = VermessungsunterlagenHelper.determineAlkisProduct(String.valueOf(
                    "WUP-Kommunal"),
                String.valueOf("NivP-Übersicht"),
                envelope);

        final URL url = ServerAlkisProducts.getInstance()
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
