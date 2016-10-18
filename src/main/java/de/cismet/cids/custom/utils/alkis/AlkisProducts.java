/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.alkis;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.awt.Point;

import java.io.UnsupportedEncodingException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.utils.serverresources.CachedServerResourcesLoader;

import de.cismet.tools.StaticHtmlTools;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public final class AlkisProducts {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AlkisProducts.class);

    private static AlkisProducts INSTANCE;

    //~ Instance fields --------------------------------------------------------

    // Flurstueck
    public final String FLURSTUECKSNACHWEIS_PDF;
    public final String FLURSTUECKSNACHWEIS_HTML;
    public final String FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_PDF;
    public final String FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_HTML;
    public final String FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF;
    public final String FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_HTML;
    public final String FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_PDF;
    public final String FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_HTML;
    // Buchungsblatt
    public final String BESTANDSNACHWEIS_NRW_PDF;
    public final String BESTANDSNACHWEIS_STICHTAGSBEZOGEN_NRW_PDF;
    public final String BESTANDSNACHWEIS_NRW_HTML;
    public final String BESTANDSNACHWEIS_KOMMUNAL_PDF;
    public final String BESTANDSNACHWEIS_KOMMUNAL_HTML;
    public final String BESTANDSNACHWEIS_KOMMUNAL_INTERN_PDF;
    public final String BESTANDSNACHWEIS_KOMMUNAL_INTERN_HTML;
    public final String GRUNDSTUECKSNACHWEIS_NRW_PDF;
    public final String GRUNDSTUECKSNACHWEIS_NRW_HTML;
    // Punkt
    public final String PUNKTLISTE_PDF;
    public final String PUNKTLISTE_HTML;
    public final String PUNKTLISTE_TXT;
    //
    public final Map<String, Point> ALKIS_FORMATS;
    public final List<AlkisProductDescription> ALKIS_MAP_PRODUCTS;
    private final String IDENTIFICATIONANDMORE;
    private final SimpleDateFormat stichtagDateFormat = new SimpleDateFormat("dd.MM.yyyy");
    //

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlkisProducts object.
     *
     * @param  user     DOCUMENT ME!
     * @param  pw       DOCUMENT ME!
     * @param  service  DOCUMENT ME!
     */
    private AlkisProducts(final String user, final String pw, final String service) {
        final Properties productProperties = CachedServerResourcesLoader.getInstance()
                    .getPropertiesResource(WundaBlauServerResources.ALKIS_PRODUCTS.getValue());
        final List<AlkisProductDescription> mapProducts = new ArrayList<AlkisProductDescription>();
        final Map<String, Point> formatMap = new HashMap<String, Point>();
        ALKIS_FORMATS = Collections.unmodifiableMap(formatMap);
        ALKIS_MAP_PRODUCTS = Collections.unmodifiableList(mapProducts);
        IDENTIFICATIONANDMORE = "user=" + user + "&password=" + pw + "&service=" + service
                    + "&script=" + productProperties.getProperty("NACHVERARBEITUNG_SCRIPT");
        FLURSTUECKSNACHWEIS_PDF = productProperties.getProperty("FLURSTUECKSNACHWEIS_PDF");
        FLURSTUECKSNACHWEIS_HTML = productProperties.getProperty("FLURSTUECKSNACHWEIS_HTML");
        FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF = productProperties.getProperty(
                "FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF");
        FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_HTML = productProperties.getProperty(
                "FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_HTML");
        FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_PDF = productProperties.getProperty(
                "FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_PDF");
        FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_HTML = productProperties.getProperty(
                "FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_HTML");
        FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_PDF = productProperties.getProperty(
                "FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_PDF");
        FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_HTML = productProperties.getProperty(
                "FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_HTML");
        //
        GRUNDSTUECKSNACHWEIS_NRW_PDF = productProperties.getProperty("GRUNDSTUECKSNACHWEIS_NRW_PDF");
        GRUNDSTUECKSNACHWEIS_NRW_HTML = productProperties.getProperty("GRUNDSTUECKSNACHWEIS_NRW_HTML");
        BESTANDSNACHWEIS_NRW_PDF = productProperties.getProperty("BESTANDSNACHWEIS_NRW_PDF");
        BESTANDSNACHWEIS_STICHTAGSBEZOGEN_NRW_PDF = productProperties.getProperty(
                "BESTANDSNACHWEIS_STICHTAGSBEZOGEN_NRW_PDF");
        BESTANDSNACHWEIS_NRW_HTML = productProperties.getProperty("BESTANDSNACHWEIS_NRW_HTML");
        BESTANDSNACHWEIS_KOMMUNAL_PDF = productProperties.getProperty("BESTANDSNACHWEIS_KOMMUNAL_PDF");
        BESTANDSNACHWEIS_KOMMUNAL_HTML = productProperties.getProperty("BESTANDSNACHWEIS_KOMMUNAL_HTML");
        BESTANDSNACHWEIS_KOMMUNAL_INTERN_PDF = productProperties.getProperty("BESTANDSNACHWEIS_KOMMUNAL_INTERN_PDF");
        BESTANDSNACHWEIS_KOMMUNAL_INTERN_HTML = productProperties.getProperty("BESTANDSNACHWEIS_KOMMUNAL_INTERN_HTML");
        //
        PUNKTLISTE_PDF = productProperties.getProperty("PUNKTLISTE_PDF");
        PUNKTLISTE_HTML = productProperties.getProperty("PUNKTLISTE_HTML");
        PUNKTLISTE_TXT = productProperties.getProperty("PUNKTLISTE_TXT");
        try {
            final Properties formats = CachedServerResourcesLoader.getInstance()
                        .getPropertiesResource(WundaBlauServerResources.ALKIS_FORMATS.getValue());
            final Document document =
                new SAXBuilder().build(CachedServerResourcesLoader.getInstance().getStringReaderResource(
                        WundaBlauServerResources.ALKIS_PRODUKTBESCHREIBUNG_XML.getValue()));
            // ---------Kartenprodukte----------
            for (final Object o0 : document.getRootElement().getChildren()) {
                final Element category = (Element)o0;
                final String catName = category.getName();
                if ("Karte".equals(catName)) {
                    for (final Object o1 : category.getChildren()) {
                        final Element productClass = (Element)o1;
                        if (productClass.getName().matches(".*[Kk]lasse.*")) {
                            final String clazz = productClass.getAttribute("Name").getValue();
                            for (final Object o2 : productClass.getChildren()) {
                                final Element guiProduct = (Element)o2;
                                final String type = guiProduct.getAttribute("ProduktnameAuswertung").getValue();
                                final Attribute defaultProductAttr = guiProduct.getAttribute(
                                        "defaultProduct");
                                boolean defaultProduct;
                                if (defaultProductAttr != null) {
                                    defaultProduct = defaultProductAttr.getBooleanValue();
                                } else {
                                    defaultProduct = false;
                                }
                                final Attribute productDefaultScaleAttr = guiProduct.getAttribute(
                                        "productDefaultScale");
                                Integer productDefaultScale;
                                if (productDefaultScaleAttr != null) {
                                    productDefaultScale = productDefaultScaleAttr.getIntValue();
                                } else {
                                    productDefaultScale = null;
                                }
                                for (final Object o3 : guiProduct.getChildren()) {
                                    final Element singleProduct = (Element)o3;
                                    final Attribute codeAttr = singleProduct.getAttribute("ID");
                                    if (codeAttr != null) {
                                        final String code = codeAttr.getValue();
                                        final String dinFormatCode = singleProduct.getAttribute("Layout").getValue();
                                        final String layoutDim = formats.getProperty(dinFormatCode);
                                        int width = -1;
                                        int height = -1;
                                        if (layoutDim == null) {
                                            org.apache.log4j.Logger.getLogger(AlkisConstants.class)
                                                    .info("Can not find format dimensions for: " + dinFormatCode);
                                        } else {
                                            final String[] dims = layoutDim.split("(x|X)");
                                            width = Integer.parseInt(dims[0]);
                                            height = Integer.parseInt(dims[1]);
                                            formatMap.put(dinFormatCode, new Point(width, height));
                                        }

                                        // Preisfaktoren
                                        final Element preisFaktoren = (Element)singleProduct.getChildren().get(0);
                                        final String dinFormat = preisFaktoren.getAttribute("DINFormat").getValue();
                                        final String fileFormat = preisFaktoren.getAttribute("Dateiformat").getValue();
                                        final Attribute massstabAttr = preisFaktoren.getAttribute("Massstab");
                                        String massstab;
                                        if (massstabAttr != null) {
                                            massstab = preisFaktoren.getAttribute("Massstab").getValue();
                                        } else {
                                            massstab = "-";
                                        }
                                        final Attribute massstabMinAttr = preisFaktoren.getAttribute("MassstabMin");
                                        String massstabMin = null;
                                        if (massstabMinAttr != null) {
                                            massstabMin = preisFaktoren.getAttribute("MassstabMin").getValue();
                                        }
                                        final Attribute massstabMaxAttr = preisFaktoren.getAttribute("MassstabMin");
                                        String massstabMax = null;
                                        if (massstabMaxAttr != null) {
                                            massstabMax = preisFaktoren.getAttribute("MassstabMax").getValue();
                                        }

                                        // Stempelfeld
                                        StempelfeldInfo stempelfeldInfo = null;
                                        final Element stempelFeldInfoElement = (Element)singleProduct.getChild(
                                                "Stempelfeld",
                                                singleProduct.getNamespace());
                                        if (stempelFeldInfoElement != null) {
                                            final float fromX = stempelFeldInfoElement.getAttribute("fromX")
                                                        .getFloatValue();
                                            final float fromY = stempelFeldInfoElement.getAttribute("fromY")
                                                        .getFloatValue();
                                            final float toX = stempelFeldInfoElement.getAttribute("toX")
                                                        .getFloatValue();
                                            final float toY = stempelFeldInfoElement.getAttribute("toY")
                                                        .getFloatValue();
                                            stempelfeldInfo = new StempelfeldInfo(fromX, fromY, toX, toY);
                                        }

                                        final AlkisProductDescription currentProduct = new AlkisProductDescription(
                                                clazz,
                                                type,
                                                code,
                                                dinFormat,
                                                massstab,
                                                massstabMin,
                                                massstabMax,
                                                fileFormat,
                                                width,
                                                height,
                                                defaultProduct,
                                                stempelfeldInfo,
                                                productDefaultScale);
                                        mapProducts.add(currentProduct);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error while parsing Alkis Product Description!", ex);
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static AlkisProducts getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AlkisProducts(
                    AlkisConstants.COMMONS.USER,
                    AlkisConstants.COMMONS.PASSWORD,
                    AlkisConstants.COMMONS.SERVICE);
        }
        return INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   pointBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getPointDataForProduct(final CidsBean pointBean) {
        final StringBuilder sb = new StringBuilder("AX_");
        sb.append(pointBean.getProperty("pointtype"));
        sb.append(":");
        sb.append(pointBean.getProperty("pointcode"));
        return sb.toString().replace(" ", "");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   objectID           DOCUMENT ME!
     * @param   productCode        DOCUMENT ME!
     * @param   fertigungsVermerk  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MalformedURLException  DOCUMENT ME!
     */
    public URL productEinzelNachweisUrl(final String objectID, final String productCode, final String fertigungsVermerk)
            throws MalformedURLException {
        final String fabricationNotice = generateFabricationNotice(fertigungsVermerk);
        return new URL(AlkisConstants.COMMONS.EINZEL_NACHWEIS_SERVICE + "?" + AlkisConstants.MLESSNUMBER + "&product="
                        + productCode + "&id=" + objectID + "&" + IDENTIFICATIONANDMORE
                        + ((fabricationNotice != null) ? ("&" + fabricationNotice) : ""));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   objectID     DOCUMENT ME!
     * @param   productCode  DOCUMENT ME!
     * @param   stichtag     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MalformedURLException  DOCUMENT ME!
     */
    public URL productEinzelnachweisStichtagsbezogenUrl(final String objectID,
            final String productCode,
            final Date stichtag) throws MalformedURLException {
        return new URL(AlkisConstants.COMMONS.EINZEL_NACHWEIS_SERVICE + "?" + AlkisConstants.MLESSNUMBER
                        + "&reportingDate=" + stichtagDateFormat.format(stichtag)
                        + "&product=" + productCode + "&id=" + objectID + "&" + IDENTIFICATIONANDMORE);
    }

    /**
     * Returns a URL to a document for given points.
     *
     * @param   punktliste   The points.
     * @param   productCode  format The format of the document.
     *
     * @return  DOCUMENT ME!
     */
    public String productListenNachweisUrl(final String punktliste, final String productCode) {
        return AlkisConstants.COMMONS.LISTEN_NACHWEIS_SERVICE + "?" + AlkisConstants.MLESSNUMBER + "&product="
                    + productCode + "&ids=" + punktliste + "&" + IDENTIFICATIONANDMORE;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fertigungsVermerk  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String generateFabricationNotices(final String fertigungsVermerk) {
        if (fertigungsVermerk != null) {
            try {
                final String notice1 = URLEncoder.encode(
                        "Gefertigt im Auftrag der Stadt Wuppertal durch: Öffentlich bestellter Vermessungsingenieur",
                        "UTF-8");
                final String notice3 = URLEncoder.encode(fertigungsVermerk, "UTF-8");

                return "fabricationNotice1=" + notice1 + "&fabricationNotice2=&fabricationNotice3=" + notice3;
            } catch (final UnsupportedEncodingException ex) {
                log.error("error while encoding fabricationnotice", ex);
                return null;
            }
        } else {
            return "";
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fertigungsVermerk  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String generateFabricationNotice(final String fertigungsVermerk) {
        if (fertigungsVermerk != null) {
            try {
                final String note = URLEncoder.encode(
                        "Gefertigt im Auftrag der Stadt Wuppertal durch: Öffentlich bestellter Vermessungsingenieur "
                                + fertigungsVermerk,
                        "UTF-8");
                return "fabricationNotice=" + note;
            } catch (final UnsupportedEncodingException ex) {
                log.error("error while encoding fabricationnotice", ex);
                return null;
            }
        } else {
            return "";
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   parcelCode         DOCUMENT ME!
     * @param   fertigungsVermerk  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MalformedURLException  DOCUMENT ME!
     */
    public URL productKarteUrl(final String parcelCode, final String fertigungsVermerk) throws MalformedURLException {
        final String fabricationNotices = generateFabricationNotices(fertigungsVermerk);
        return new URL(AlkisConstants.COMMONS.LIEGENSCHAFTSKARTE_SERVICE + "?" + AlkisConstants.MLESSNUMBER
                        + "&landparcel=" + parcelCode + "&" + IDENTIFICATIONANDMORE
                        + ((fabricationNotices != null) ? ("&" + fabricationNotices) : ""));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   parcelCode         DOCUMENT ME!
     * @param   produkt            DOCUMENT ME!
     * @param   winkel             DOCUMENT ME!
     * @param   centerX            DOCUMENT ME!
     * @param   centerY            DOCUMENT ME!
     * @param   zusText            DOCUMENT ME!
     * @param   auftragsNr         DOCUMENT ME!
     * @param   moreThanOneParcel  DOCUMENT ME!
     * @param   fertigungsVermerk  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MalformedURLException  DOCUMENT ME!
     */
    public URL productKarteUrl(final String parcelCode,
            final AlkisProductDescription produkt,
            final int winkel,
            final int centerX,
            final int centerY,
            final String zusText,
            final String auftragsNr,
            final boolean moreThanOneParcel,
            final String fertigungsVermerk) throws MalformedURLException {
        final StringBuilder url = new StringBuilder(AlkisConstants.COMMONS.LIEGENSCHAFTSKARTE_SERVICE);
        url.append('?');
        url.append(AlkisConstants.MLESSNUMBER);
        url.append("&landparcel=");
        url.append(parcelCode);
        url.append("&angle=");
        url.append(winkel);
        url.append("&product=");
        url.append(produkt.getCode());
        url.append("&centerx=");
        url.append(centerX);
        url.append("&centery=");
        url.append(centerY);

        if ((zusText != null) && (zusText.length() > 0)) {
            url.append("&text=");
            url.append(StaticHtmlTools.encodeURLParameter(zusText));
        }
        if ((auftragsNr != null) && (auftragsNr.length() > 0)) {
            url.append("&ordernumber=");
            url.append(StaticHtmlTools.encodeURLParameter(auftragsNr));
        }
        if (moreThanOneParcel) {
            url.append("&additionalLandparcel=true");
        }
        url.append('&');
        url.append(IDENTIFICATIONANDMORE);
        if ((produkt.getMassstabMin() != null) && (produkt.getMassstabMax() != null)) {
            url.append("&scale=");
            url.append(produkt.getMassstab());
        }
        final String fabricationNotices = generateFabricationNotices(fertigungsVermerk);
        if (fabricationNotices != null) {
            url.append("&").append(fabricationNotices);
        }

        return new URL(url.toString());
    }
}
