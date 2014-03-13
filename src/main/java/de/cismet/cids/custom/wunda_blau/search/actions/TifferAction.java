/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.util.image.ImageAnnotator;

import org.apache.log4j.Logger;

import org.jfree.util.Log;

import org.openide.util.Exceptions;

import java.awt.image.BufferedImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.HashMap;
import java.util.PropertyResourceBundle;

import javax.imageio.ImageIO;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

import static de.cismet.cids.custom.wunda_blau.search.actions.TifferAction.ParameterType.*;

/**
 * A server action which adds a simple footer with text to an image. This image can be downloaded from a URL.
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 * @see      ImageAnnotator
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class TifferAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(TifferAction.class);
    public static final String ACTION_NAME = "tifferAction";
    private static final String WATERMARK_NAME = "wupperwurm.gif";
    private static BufferedImage watermark;

    static {
        try {
            watermark = ImageIO.read(TifferAction.class.getResourceAsStream(WATERMARK_NAME));
        } catch (IOException ex) {
            watermark = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            LOG.error("Watermark could not be properly created. Using an empty image instead.", ex);
        }
    }

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ParameterType {

        //~ Enum constants -----------------------------------------------------

        BILDNUMMER, ORT, AUFNAHME_DATUM, FORMAT, SCALE, SUBDIR
    }

    //~ Instance fields --------------------------------------------------------

    PropertyResourceBundle res = null;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new TifferAction object.
     */
    public TifferAction() {
        try {
            res = new PropertyResourceBundle(this.getClass().getResourceAsStream("luftbild_servlet.cfg"));
        } catch (Exception e) {
            LOG.error("Resource not found");
        }
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final HashMap parameterMap = createHashMap(params);

        String txt = res.getString("annotation");
        final String base = res.getString("base");
        final String separator = res.getString("separator");
        final String urlOrFile = res.getString("resource_type");
        if (LOG.isDebugEnabled()) {
            LOG.debug(txt + "\n" + base + "\n" + separator + "\n" + urlOrFile);
        }

        String bildnummer = (String)parameterMap.get(BILDNUMMER.toString());

        if (bildnummer == null) {
            return null;
        }

        final String ort = (String)parameterMap.get(ORT.toString());
        final String aufdat = (String)parameterMap.get(AUFNAHME_DATUM.toString());
        String format = (String)parameterMap.get(FORMAT.toString());
        final String scale = (String)parameterMap.get(SCALE.toString());

        String subdir = (String)parameterMap.get(SUBDIR.toString());
        if (subdir == null) {
            subdir = "";
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        if ((!bildnummer.endsWith(".TIF") && !bildnummer.endsWith(".tif") && !bildnummer.endsWith(".TIFF")
                        && !bildnummer.endsWith("tiff"))) {
            bildnummer += ".tif";
        }

        if (ort != null) {
            txt = "Ort {" + ort + "} / " + txt;
        }
        if (aufdat != null) {
            txt = "Aufnahmedatum {" + aufdat + "} / " + txt;
        }
        txt = "Bildnummer {" + bildnummer + "} / " + txt;

        ////////////////////////////////////////////////////////////////////////////////////////

        if (format != null) {
            if (format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg")) {
                format = "JPEG";
            } else if (format.equalsIgnoreCase("bmp")) {
                format = "BMP";
            } else if (format.equalsIgnoreCase("png")) {
                format = "PNG";
            } else {
                format = "TIFF";
            }
        } else {
            format = "TIFF";
        }

        //////////////////////////////////////////////////////////////////////////////////
        double scaleFactor = 1.0;

        try {
            if ((scale != null) && !scale.equals("0.0") && !scale.equals("0") && !scale.equals(".0")) {
                scaleFactor = new Double(scale).doubleValue();
            }
        } catch (Exception e) {
            LOG.error("scale Format", e);
        }

        ImageAnnotator a;
        try {
            if (!urlOrFile.equalsIgnoreCase("file")) {
                final URL imgUrl = new URL("http://" + base + subdir + bildnummer);
                a = new ImageAnnotator(imgUrl, watermark, txt);
            } else // file
            {
                final String fileLocation = base + subdir + bildnummer;
                a = new ImageAnnotator(fileLocation, watermark, txt);
            }
        } catch (MalformedURLException ex) {
            LOG.error("MalformedURLException while annotating the image.", ex);
            return null;
        } catch (IOException ex) {
            LOG.error("IOException while annotating the image.", ex);
            return null;
        } catch (Exception ex) {
            LOG.error("Some other exception while annotating the image.", ex);
            return null;
        }

        a.setPrintFilename(false);

        BufferedImage bi = a.getAnnotatedImage();

        java.awt.Image image = null;
        if (scaleFactor != 1.0) {
            image = bi.getScaledInstance((int)(bi.getWidth() * scaleFactor),
                    (int)(bi.getHeight() * scaleFactor),
                    java.awt.Image.SCALE_FAST);
        }

        if (image != null) {
            bi = new BufferedImage(image.getWidth(null), image.getHeight(null), bi.getType());
            bi.getGraphics().drawImage(image, 0, 0, null);
        }

        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            ImageIO.write(bi, format, out);

            return out.toByteArray();
        } catch (IOException ex) {
            LOG.error("Error while creating the ByteArrayOutputStream", ex);
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    Log.error(ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   params  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private HashMap createHashMap(final ServerActionParameter... params) {
        final HashMap map = new HashMap();
        for (final ServerActionParameter param : params) {
            map.put(param.getKey(), param.getValue());
        }
        return map;
    }

    @Override
    public String getTaskName() {
        return ACTION_NAME;
    }
}
