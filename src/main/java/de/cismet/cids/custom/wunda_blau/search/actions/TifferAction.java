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

import org.apache.log4j.Logger;

import org.jfree.util.Log;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.HashMap;
import java.util.Iterator;
import java.util.PropertyResourceBundle;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

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

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ParameterType {

        //~ Enum constants -----------------------------------------------------

        BILDNUMMER, FORMAT, SCALE, SUBDIR
    }

    //~ Instance fields --------------------------------------------------------

    PropertyResourceBundle res = null;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new TifferAction object.
     */
    public TifferAction() {
        try {
            res = new PropertyResourceBundle(this.getClass().getResourceAsStream("tifferAction.cfg"));
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

        final String bildnummer = (String)parameterMap.get(BILDNUMMER.toString());

        if (bildnummer == null) {
            return null;
        }

        String format = (String)parameterMap.get(FORMAT.toString());
        final String scale = (String)parameterMap.get(SCALE.toString());

        String subdir = (String)parameterMap.get(SUBDIR.toString());
        if (subdir == null) {
            subdir = "";
        }

        txt = txt.replace("$bnr$", bildnummer);
        txt = txt.replace("(c)", "\u00A9");

        ////////////////////////////////////////////////////////////////////////////////////////

        if (format != null) {
            if (format.equalsIgnoreCase("jpg")) {
                format = "JPG";
            } else if (format.equalsIgnoreCase("jpeg")) {
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
                scaleFactor = Double.parseDouble(scale);
            }
        } catch (Exception e) {
            LOG.error("scale Format", e);
        }

        ImageAnnotator a;
        try {
            if (!urlOrFile.equalsIgnoreCase("file")) {
                final URL imgUrl = new URL("http://" + base + subdir + bildnummer + "." + format.toLowerCase());
                a = new ImageAnnotator(imgUrl, txt);
            } else // file
            {
                final String fileLocation = base + subdir + bildnummer + "." + format.toLowerCase();
                a = new ImageAnnotator(fileLocation, txt);
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
            writeImage(bi, format, out);

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
     * A replacement of ImageIO.write() as it compresses JPGs to much.
     *
     * @param   image   DOCUMENT ME!
     * @param   format  DOCUMENT ME!
     * @param   output  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public static void writeImage(final RenderedImage image, final String format, final Object output)
            throws IOException {
        final Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(format);
        final ImageWriter writer = iter.next();
        final ImageWriteParam iwp = writer.getDefaultWriteParam();
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.setCompressionQuality(1f);

        final ImageOutputStream ios = ImageIO.createImageOutputStream(output);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, null), iwp);
        writer.dispose();
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
