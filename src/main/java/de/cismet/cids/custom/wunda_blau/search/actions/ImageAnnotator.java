/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * ImageAnotator.java
 *
 * Created on 13. Januar 2005, 10:07
 */
package de.cismet.cids.custom.wunda_blau.search.actions;

import org.mortbay.log.Log;

import java.awt.*;

//import java.net.MalformedURLException;
import java.awt.image.BufferedImage;

import java.io.*;

import java.net.URL;

import java.util.*;

import javax.imageio.*;
import javax.imageio.stream.*;

//import com.sun.media.jai.codec.*;
/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
public class ImageAnnotator {

    //~ Static fields/initializers ---------------------------------------------

    static int LINES = 1;
    private static Font calibriFont;

    static {
        try {
            final InputStream is = ImageAnnotator.class.getResourceAsStream("Calibri_Bold.ttf");
            calibriFont = Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (FontFormatException ex) {
            Log.warn("Calibri could not be loaded", ex);
            calibriFont = new Font(Font.SANS_SERIF, Font.BOLD, 100);
        } catch (IOException ex) {
            Log.warn("Calibri could not be loaded", ex);
            calibriFont = new Font(Font.SANS_SERIF, Font.BOLD, 100);
        }
    }

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            ImageAnnotator.class);

    //~ Instance fields --------------------------------------------------------

    BufferedImage image = null;
    BufferedImage watermark = null;
    int width = 0;
    int height = 0;
    int newHeight = 0;
    int newWidth = 0;
    int maximalTextHeight = 0;
    int maximalTextWidth = 0;
    String text = "";
    float ratio = 1.03f;
    java.awt.Color textColor = Color.BLACK;
    java.awt.Color backGroundColor = new java.awt.Color(235, 235, 235);
    String filename = "";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new instance of ImageAnotator.
     *
     * @param   filename  DOCUMENT ME!
     * @param   text      watermarkFilename DOCUMENT ME!
     *
     * @throws  java.io.IOException  DOCUMENT ME!
     */
    public ImageAnnotator(final String filename, final String text) throws java.io.IOException {
        this.filename = filename;

        image = loadImage(filename);

        this.width = image.getWidth();
        this.height = image.getHeight();

        this.text = text;
    }

    /**
     * Creates a new ImageAnnotator object.
     *
     * @param   filename  DOCUMENT ME!
     * @param   text      DOCUMENT ME!
     *
     * @throws  java.io.IOException  DOCUMENT ME!
     */
    public ImageAnnotator(final URL filename, final String text) throws java.io.IOException {
        image = loadImage(filename);

        this.width = image.getWidth();
        this.height = image.getHeight();

        this.text = text;
    }

    /**
     * Creates a new ImageAnnotator object.
     *
     * @param   filename   DOCUMENT ME!
     * @param   watermark  DOCUMENT ME!
     *
     * @throws  java.io.IOException  DOCUMENT ME!
     */
    public ImageAnnotator(final String filename, final BufferedImage watermark) throws java.io.IOException {
        this.filename = filename;

        image = loadImage(filename);

        this.width = image.getWidth();
        this.height = image.getHeight();

        this.watermark = watermark;
    }

    /**
     * Creates a new ImageAnnotator object.
     *
     * @param   filename           DOCUMENT ME!
     * @param   watermarkFilename  DOCUMENT ME!
     * @param   text               DOCUMENT ME!
     *
     * @throws  java.io.IOException  DOCUMENT ME!
     */
    public ImageAnnotator(final String filename, final String watermarkFilename, final String text)
            throws java.io.IOException {
        this.filename = filename;

        image = loadImage(filename);

        this.width = image.getWidth();
        this.height = image.getHeight();

        watermark = loadImage(watermarkFilename);

        this.text = text;
    }

    /**
     * Creates a new ImageAnnotator object.
     *
     * @param   filename           DOCUMENT ME!
     * @param   watermarkFilename  DOCUMENT ME!
     * @param   text               DOCUMENT ME!
     *
     * @throws  java.io.IOException  DOCUMENT ME!
     */
    public ImageAnnotator(final String filename, final BufferedImage watermarkFilename, final String text)
            throws java.io.IOException {
        this(filename, watermarkFilename);

        this.text = text;
    }

    /**
     * Creates a new ImageAnnotator object.
     *
     * @param   file       DOCUMENT ME!
     * @param   watermark  DOCUMENT ME!
     * @param   text       DOCUMENT ME!
     *
     * @throws  java.io.IOException  DOCUMENT ME!
     */
    public ImageAnnotator(final URL file, final URL watermark, final String text) throws java.io.IOException {
        image = loadImage(file);

        this.width = image.getWidth();
        this.height = image.getHeight();

        this.watermark = loadImage(watermark);

        this.text = text;
    }

    /**
     * Creates a new ImageAnnotator object.
     *
     * @param   file       DOCUMENT ME!
     * @param   watermark  DOCUMENT ME!
     * @param   text       DOCUMENT ME!
     *
     * @throws  java.io.IOException  DOCUMENT ME!
     */
    public ImageAnnotator(final URL file, final BufferedImage watermark, final String text) throws java.io.IOException {
        image = loadImage(file);

        this.width = image.getWidth();
        this.height = image.getHeight();

        this.watermark = watermark;

        this.text = text;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   filename  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  java.io.IOException  DOCUMENT ME!
     */
    protected final BufferedImage loadImage(final String filename) throws java.io.IOException {
        String type = extractImageType(filename);

        if (type.length() == 0) {
            type = "TIF";
        }

        // load
        final File f = new File(filename);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Info : file :" + filename + " file " + f);
        }

        final Iterator readers = ImageIO.getImageReadersByFormatName(type);
        final ImageReader reader = (ImageReader)readers.next();

        final ImageInputStream iis = ImageIO.createImageInputStream(f);

        reader.setInput(iis, false);

        final int imageIndex = 0;

        // width = reader.getWidth(imageIndex);
        // height = reader.getHeight(imageIndex);

        return reader.read(imageIndex);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   imgUrl  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  java.io.IOException  DOCUMENT ME!
     */
    protected final BufferedImage loadImage(final URL imgUrl) throws java.io.IOException {
        String type = extractImageType(imgUrl.getFile());

        if (type.length() == 0) {
            type = "TIF";
        }

        // retrieve appropriate imagereader
        final Iterator readers = ImageIO.getImageReadersByFormatName(type);

        final ImageReader reader = (ImageReader)readers.next();
        if (LOG.isDebugEnabled()) {
            LOG.debug(imgUrl);
        }

        final ImageInputStream iis = ImageIO.createImageInputStream(imgUrl.openStream());

        reader.setInput(iis, false);

        final int imageIndex = 0;

        // width = reader.getWidth(imageIndex);
        // height = reader.getHeight(imageIndex);

        return reader.read(imageIndex);
    }

    /**
     * Getter for property text.
     *
     * @return  Value of property text.
     */
    public java.lang.String getText() {
        return text;
    }

    /**
     * Setter for property text.
     *
     * @param  text  New value of property text.
     */
    public void setText(final java.lang.String text) {
        this.text = text;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  text  DOCUMENT ME!
     */
    public void addText(final java.lang.String text) {
        this.text += text;
    }

    /**
     * Getter for property ratio.
     *
     * @return  Value of property ratio.
     */
    public float getRatio() {
        return ratio;
    }

    /**
     * Setter for property ratio.
     *
     * @param  ratio  New value of property ratio.
     */
    public void setRatio(final float ratio) {
        this.ratio = ratio;
    }

    /**
     * Getter for property textColor.
     *
     * @return  Value of property textColor.
     */
    public java.awt.Color getTextColor() {
        return textColor;
    }

    /**
     * Setter for property textColor.
     *
     * @param  textColor  New value of property textColor.
     */
    public void setTextColor(final java.awt.Color textColor) {
        this.textColor = textColor;
    }

    /**
     * Getter for property backGroundColor.
     *
     * @return  Value of property backGroundColor.
     */
    public java.awt.Color getBackGroundColor() {
        return backGroundColor;
    }

    /**
     * Setter for property backGroundColor.
     *
     * @param  backGroundColor  New value of property backGroundColor.
     */
    public void setBackGroundColor(final java.awt.Color backGroundColor) {
        this.backGroundColor = backGroundColor;
    }

    /**
     * Getter for property image.
     *
     * @return  Value of property image.
     */
    public java.awt.image.BufferedImage getImage() {
        return image;
    }

    /**
     * Setter for property image.
     *
     * @param  image  New value of property image.
     */
    public void setImage(final java.awt.image.BufferedImage image) {
        this.image = image;
    }

    /**
     * Getter for property watermark.
     *
     * @return  Value of property watermark.
     */
    public java.awt.image.BufferedImage getWatermark() {
        return watermark;
    }

    /**
     * Setter for property watermark.
     *
     * @param  watermark  New value of property watermark.
     */
    public void setWatermark(final java.awt.image.BufferedImage watermark) {
        this.watermark = watermark;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public BufferedImage getAnnotatedImage() {
        newHeight = (int)(height * ratio);
        newWidth = width; // evtl ver\u00E4ndern aber heapspace
        final int thickness = (newHeight - height) / 20;

        final BufferedImage bi = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        final Graphics2D g = (Graphics2D)bi.getGraphics();

        // Wuppercolor (Hintergrund)
        g.setColor(backGroundColor);

        // f\u00FClle mit Hintergrundfarbe
        g.fillRect(0, 0, newWidth, newHeight);

        if (watermark != null) {
            // wasserzeichen
            g.drawImage(
                watermark,
                newWidth
                        - (watermark.getWidth() + 5),
                newHeight
                        - (watermark.getHeight() + 5),
                null);
        }

        // bild zeichnen
        g.drawImage(image, 0, 0, null);

        // grenzt ein bischen besser ab
        g.setColor(Color.black);
        g.setStroke(new BasicStroke(thickness));
        final int x = 0;
        final int y = 0;

        g.drawRect(x + (thickness / 2),
            y
                    + (thickness / 2),
            width
                    - (2 * (thickness / 2)),
            newHeight
                    - (2 * (thickness / 2))
                    - 1);

        if (text.length() > 0) {
            drawText(g);
        }

        return bi;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  g  DOCUMENT ME!
     */
    protected void drawText(final Graphics g) {
        // set antialiasing
        if (g instanceof Graphics2D) {
            ((Graphics2D)g).setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            ((Graphics2D)g).setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            System.out.println("set antialiasing");
        }

        final int rectangleHeight = (newHeight - height);
        maximalTextHeight = rectangleHeight / 2;
        int fontSize = maximalTextHeight / LINES;
        maximalTextWidth = width - (2 * (width / 10));
        Font f = calibriFont.deriveFont(fontSize * 1f);
        g.setFont(f);

        FontMetrics fm = g.getFontMetrics();
        String[] linesToDraw = testDraw(fm);

        while (linesToDraw.length > LINES) {
            fontSize--;
            maximalTextWidth = width - (width / 10);
            f = calibriFont.deriveFont(fontSize * 1f);
            g.setFont(f);
            fm = g.getFontMetrics();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Trying new fontsize : " + fontSize);
            }

            linesToDraw = testDraw(fm);
        }

        int stringPos = height + (rectangleHeight / 2) + (fm.getAscent() / 3);
        for (final String line : linesToDraw) {
            g.drawString(line, fontSize / 3, stringPos);
            stringPos += fontSize - 2;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fontMetrics  ppc g DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String[] testDraw(final FontMetrics fontMetrics) {
        String stillToDraw = text;
        final ArrayList<String> linesToDraw = new ArrayList<String>();
        while (!stillToDraw.isEmpty() || !stillToDraw.matches("\\s*")) {
            final String drawString = getSubstringFittingOneLine(stillToDraw, fontMetrics);

            linesToDraw.add(drawString);
            stillToDraw = stillToDraw.substring(drawString.length()).trim();
            if (LOG.isDebugEnabled()) {
                LOG.debug("stillToDraw: " + stillToDraw);
            }
        }
        return linesToDraw.toArray(new String[linesToDraw.size()]);
    }

    /**
     * Gets a String and a FontMetric and checks if that String fits in one line in the image. If it fits that String is
     * returned. If not the
     *
     * @param   stillToDraw  DOCUMENT ME!
     * @param   fontMetrics  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getSubstringFittingOneLine(final String stillToDraw, final FontMetrics fontMetrics) {
        if (fontMetrics.stringWidth(stillToDraw) < maximalTextWidth) {
            return stillToDraw;
        }

        int space = stillToDraw.lastIndexOf(" ");
        String fitsOneLine = stillToDraw.substring(0, space + 1);
        while (fontMetrics.stringWidth(fitsOneLine) > maximalTextWidth) {
            space = fitsOneLine.lastIndexOf(" ");
            fitsOneLine = stillToDraw.substring(0, space);
        }
        return fitsOneLine;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   im        DOCUMENT ME!
     * @param   filename  DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public void saveImage(final BufferedImage im, final String filename) throws IOException {
        String type = extractImageType(filename);

        if (type.length() == 0) {
            type = "TIF";
        }

        final Iterator writers = ImageIO.getImageWritersByFormatName(type);
        final ImageWriter writer = (ImageWriter)writers.next();

        final File f = new File(filename);
        final ImageOutputStream ios = ImageIO.createImageOutputStream(f);
        writer.setOutput(ios);

        writer.write(im);
    }

    /**
     * public void writeImage(BufferedImage im,Object output, String type) throws java.io.IOException { if(type == null
     * || type.length()==0) type="TIF"; Iterator writers = ImageIO.getImageWritersByFormatName(type); ImageWriter writer
     * = (ImageWriter)writers.next(); ImageOutputStream ios = ImageIO.createImageOutputStream(output);
     * //writer.setOutput(ios); writer.write(im); }.
     *
     * @param   filename  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String extractImageType(final String filename) {
        String type = "";
        final int endingIndex = filename.lastIndexOf(".");

        if (endingIndex > 0) {
            type = filename.substring(endingIndex + 1, filename.length());
        }

        return type;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   args  the command line arguments
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static void main(final String[] args) throws Exception {
        org.apache.log4j.BasicConfigurator.configure();

        String txt =
            "Kommunale Geodaten der Stadt Wuppertal (Bildnummer $bnr$), © Ressort Vermessung, Katasteramt und Geodaten";
        final String bildnummer = "004711";
        txt = txt.replace("$bnr$", bildnummer);

        URL imgUrl = null;
        ImageAnnotator t = null;

//        imgUrl = new URL("http://127.0.0.1:8000/hi-res.jpg");
//        t = new ImageAnnotator(imgUrl, txt);
//        t.saveImage(t.getAnnotatedImage(), "/tmp/hi-res.jpg");
//
//        imgUrl = new URL("http://127.0.0.1:8000/hi-res_hochkant.jpg");
//        t = new ImageAnnotator(imgUrl, txt);
//        t.saveImage(t.getAnnotatedImage(), "/tmp/hi-res_hochkant.jpg");

        imgUrl = new URL("http://127.0.0.1:8000/haus.jpg");
        t = new ImageAnnotator(imgUrl, txt);
        t.saveImage(t.getAnnotatedImage(), "/tmp/haus.jpg");

//        imgUrl = new URL("http://127.0.0.1:8000/baustelle.jpg");
//        t = new ImageAnnotator(imgUrl, txt);
//        t.saveImage(t.getAnnotatedImage(), "/tmp/baustelle.jpg");

        imgUrl = new URL("http://127.0.0.1:8000/treppe.jpg");
        t = new ImageAnnotator(imgUrl, txt);
        t.saveImage(t.getAnnotatedImage(), "/tmp/treppe.jpg");

//        imgUrl = new URL("http://127.0.0.1:8000/MARBLES.TIF");
//        t = new ImageAnnotator(imgUrl, txt);
//        t.saveImage(t.getAnnotatedImage(), "/tmp/marbles.tif");
//
//        imgUrl = new URL("http://127.0.0.1:8000/MARBLES_hochkant.TIF");
//        t = new ImageAnnotator(imgUrl, txt);
//        t.saveImage(t.getAnnotatedImage(), "/tmp/marbles_hochkant.tif");

        System.out.println("done");
    }
}
