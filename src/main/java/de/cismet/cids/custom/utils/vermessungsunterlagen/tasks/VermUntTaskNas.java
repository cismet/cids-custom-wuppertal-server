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

import Sirius.server.newuser.User;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import java.io.File;
import java.io.FileOutputStream;

import de.cismet.cids.custom.utils.nas.NasProduct;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenTask;
import de.cismet.cids.custom.wunda_blau.search.actions.NasDataQueryAction;

import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class VermUntTaskNas extends VermessungsunterlagenTask {

    //~ Static fields/initializers ---------------------------------------------

    private static final String XML_EXTENSION = ".xml";
    private static final String ZIP_EXTENSION = ".zip";
    private static final String DXF_EXTENSION = ".dxf";
    private static final int NAS_POLLING_FREQUENCY_MS = 5000;

    //~ Instance fields --------------------------------------------------------

    private final Geometry geometry;
    private final NasProduct product;
    private final User user;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskNas object.
     *
     * @param  type      DOCUMENT ME!
     * @param  jobkey    DOCUMENT ME!
     * @param  user      DOCUMENT ME!
     * @param  geometry  DOCUMENT ME!
     * @param  product   DOCUMENT ME!
     */
    public VermUntTaskNas(final String type,
            final String jobkey,
            final User user,
            final Geometry geometry,
            final NasProduct product) {
        super(type, jobkey);

        this.user = user;
        this.geometry = geometry;
        this.product = product;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private User getUser() {
        return user;
    }

    @Override
    public void performTask() throws Exception {
        final GeometryCollection geometryCollection = generateSearchGeomCollection(geometry);

        final String extension;
        if (product.getFormat().equals(NasProduct.Format.DXF.toString())) {
            extension = DXF_EXTENSION;
        } else {
            extension = isNASOrderSplitted(geometryCollection) ? ZIP_EXTENSION : XML_EXTENSION;
        }

        final File fileToSaveTo = new File(getPath() + "/" + getJobkey() + extension);

        // TODO anfragename ?!
        final String orderId = sendNasRequest(product, geometryCollection, getJobkey());
        if (orderId == null) {
            return;
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
                throw new Exception("error during pulling nas result from server");
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
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(fileToSaveTo);
                out.write(content);
            } catch (final Exception ex) {
                LOG.error("Couldn't write downloaded content to file '" + fileToSaveTo + "'.", ex);
                throw ex;
            } finally {
                VermessungsunterlagenHelper.closeStream(out);
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
     * @throws  Exception  DOCUMENT ME!
     */
    private String sendNasRequest(final NasProduct product,
            final GeometryCollection geometrieCollection,
            final String requestId) throws Exception {
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
            throw new Exception("error during enqueuing nas server request");
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
}
