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

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import java.io.File;
import java.io.FileOutputStream;

import de.cismet.cids.custom.utils.nas.NasProduct;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHandler;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenTask;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenTaskRetryable;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenTaskException;
import de.cismet.cids.custom.wunda_blau.search.actions.NasDataQueryAction;

import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class VermUntTaskNas extends VermessungsunterlagenTask implements VermessungsunterlagenTaskRetryable {

    //~ Static fields/initializers ---------------------------------------------

    private static final String XML_EXTENSION = ".xml";
    private static final String ZIP_EXTENSION = ".zip";
    private static final String DXF_EXTENSION = ".dxf";
    private static final int NAS_POLLING_FREQUENCY_MS = 5000;

    //~ Instance fields --------------------------------------------------------

    private final Geometry geometry;
    private final NasProduct product;
    private final String requestId;

    private String orderId;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskNas object.
     *
     * @param  type       DOCUMENT ME!
     * @param  jobkey     DOCUMENT ME!
     * @param  requestId  DOCUMENT ME!
     * @param  geometry   DOCUMENT ME!
     * @param  product    DOCUMENT ME!
     */
    public VermUntTaskNas(final String type,
            final String jobkey,
            final String requestId,
            final Geometry geometry,
            final NasProduct product) {
        super(type, jobkey);

        this.requestId = requestId;
        this.geometry = geometry;
        this.product = product;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void performTask() throws VermessungsunterlagenTaskException {
        final GeometryCollection geometryCollection = generateSearchGeomCollection(geometry);

        final String extension;
        if (product.getFormat().equals(NasProduct.Format.DXF.toString())) {
            extension = DXF_EXTENSION;
        } else {
            extension = isNASOrderSplitted(geometryCollection) ? ZIP_EXTENSION : XML_EXTENSION;
        }

        final String filename;
        if (VermessungsunterlagenHandler.NAS_PRODUCT_PUNKTE.getKey().equals(product.getKey())) {
            filename = getJobKey() + "_Koord";
        } else {
            filename = getJobKey();
        }

        final File fileToSaveTo = new File(getPath() + "/" + filename.replace("/", "--") + extension);

        /*
         * Phase 1: sending the request to the server (only if it hasnt already be sent correctly)
         */
        if (orderId == null) {
            orderId = sendNasRequest(product, geometryCollection, requestId);
            if (orderId == null) {
                final String message =
                    "Der NAS-Server hat keine OrderID zurückgeliefert. Der Download kann nicht gestartet werden.";
                LOG.error(message, new Exception());
                throw new VermessungsunterlagenTaskException(getType(), message);
            }
        }

        /*
         * Phase 2: retrive the result from the cids server
         */
        if (LOG.isDebugEnabled()) {
            LOG.debug("NAS Download: Request correctly sended start polling the result from server (max 1 hour)");
        }

        final NasDataQueryAction action = new NasDataQueryAction();
        action.setUser(getUser());
        final ServerActionParameter paramMethod = new ServerActionParameter(NasDataQueryAction.PARAMETER_TYPE.METHOD
                        .toString(),
                NasDataQueryAction.METHOD_TYPE.GET);

        final ServerActionParameter paramOrderId = new ServerActionParameter(
                NasDataQueryAction.PARAMETER_TYPE.ORDER_ID.toString(),
                orderId);
        byte[] content = null;
        while ((content == null) || (content.length == 0)) {
            if (Thread.interrupted()) {
                LOG.info("result fetching thread was interrupted");
                break;
            }
            content = (byte[])action.execute(null, paramOrderId, paramMethod);
            if (content == null) {
                final String message = "Beim Abfragen der NAS-Ergebnisse kam es zu einem unerwarteten Fehler.";
                LOG.error(message, new Exception());
                throw new VermessungsunterlagenTaskException(getType(), message);
            } else if (content.length == 0) {
                try {
                    Thread.sleep(NAS_POLLING_FREQUENCY_MS);
                } catch (InterruptedException ex) {
                    LOG.info("result fetching thread was interrupted", ex);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("NAS Download: Polling is finished.");
        }

        /*
         * Phase 3: save the result file
         */
        if (content != null) {
            try(final FileOutputStream out = new FileOutputStream(fileToSaveTo)) {
                out.write(content);
            } catch (final Exception ex) {
                final String message = "Beim Schreiben der NAS-Order in die Datei '" + fileToSaveTo
                            + "' kam es zu einem unerwarteten Fehler.";
                LOG.error(message, ex);
                throw new VermessungsunterlagenTaskException(getType(), message, ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   product              DOCUMENT ME!
     * @param   geometrieCollection  geometrie DOCUMENT ME!
     * @param   requestId            DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  VermessungsunterlagenTaskException  DOCUMENT ME!
     */
    private String sendNasRequest(final NasProduct product,
            final GeometryCollection geometrieCollection,
            final String requestId) throws VermessungsunterlagenTaskException {
        final NasDataQueryAction action = new NasDataQueryAction();
        action.setUser(getUser());
        final String result = (String)action.execute(
                null,
                new ServerActionParameter(NasDataQueryAction.PARAMETER_TYPE.TEMPLATE.toString(), product),
                new ServerActionParameter(
                    NasDataQueryAction.PARAMETER_TYPE.GEOMETRY_COLLECTION.toString(),
                    geometrieCollection),
                new ServerActionParameter(
                    NasDataQueryAction.PARAMETER_TYPE.METHOD.toString(),
                    NasDataQueryAction.METHOD_TYPE.ADD),
                new ServerActionParameter(NasDataQueryAction.PARAMETER_TYPE.REQUEST_ID.toString(), requestId));
        if (result == null) {
            final String message = "Beim Absetzen des Requests an den NAS-Server kam es zu einem unerwarteten Fehler.";
            LOG.error(message, new Exception());
            throw new VermessungsunterlagenTaskException(getType(), message);
        }
        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geometry  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static GeometryCollection generateSearchGeomCollection(final Geometry geometry) {
        final GeometryFactory gf = new GeometryFactory(geometry.getPrecisionModel(), geometry.getSRID());
        Geometry[] geoms = null;
        if (geometry instanceof MultiPolygon) {
            final MultiPolygon mp = ((MultiPolygon)geometry);
            geoms = new Geometry[mp.getNumGeometries()];
            for (int i = 0; i < mp.getNumGeometries(); i++) {
                final Geometry g = mp.getGeometryN(i);
                geoms[i] = g;
            }
        } else if (geometry instanceof Polygon) {
            geoms = new Geometry[1];
            geoms[0] = geometry;
        }

        if (geoms == null) {
            return null;
        }
        return new GeometryCollection(geoms, gf);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geoms  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isNASOrderSplitted(final GeometryCollection geoms) {
        final Envelope env = geoms.getEnvelopeInternal();
        final double xSize = env.getMaxX() - env.getMinX();
        final double ySize = env.getMaxY() - env.getMinY();

        if ((xSize > 500) && (ySize > 500)) {
            return true;
        }
        return false;
    }

    @Override
    protected String getSubPath() {
        return "/NAS";
    }

    @Override
    public long getMaxTotalWaitTimeMs() {
        return DEFAULT_MAX_TOTAL_WAIT_TIME_MS;
    }

    @Override
    public long getFirstWaitTimeMs() {
        return DEFAULT_FIRST_WAIT_TIME_MS;
    }

    @Override
    public double getWaitTimeMultiplicator() {
        return DEFAULT_WAIT_TIME_MULTIPLICATOR;
    }
}
