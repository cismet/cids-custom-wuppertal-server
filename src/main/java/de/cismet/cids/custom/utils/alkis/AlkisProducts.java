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
package de.cismet.cids.custom.utils.alkis;

import de.aedsicad.aaaweb.rest.model.Address;
import de.aedsicad.aaaweb.rest.model.Buchungsblatt;
import de.aedsicad.aaaweb.rest.model.Buchungsstelle;
import de.aedsicad.aaaweb.rest.model.LandParcel;
import de.aedsicad.aaaweb.rest.model.Namensnummer;
import de.aedsicad.aaaweb.rest.model.Owner;

import org.apache.commons.lang.StringUtils;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.awt.Point;

import java.io.StringReader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.cismet.cids.custom.wunda_blau.search.actions.AlkisRestAction;

import de.cismet.cids.dynamics.CidsBean;

import static de.cismet.cids.custom.utils.alkis.AlkisPunktReportScriptlet.SUFFIXES;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class AlkisProducts {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(AlkisProducts.class);

    public static final String HEADER_CONTENTTYPE_KEY = "Content-Type";
    public static final String HEADER_CONTENTTYPE_VALUE_POST = "application/x-www-form-urlencoded";
    public static final HashMap<String, String> POST_HEADER = new HashMap<String, String>();

    public static final String NEWLINE = "<br>";
    public static final String LINK_SEPARATOR_TOKEN = "::";

    public static final String PRODUCT_ACTION_TAG_BESTANDSNACHWEIS_NRW =
        "custom.alkis.product.bestandsnachweis_nrw@WUNDA_BLAU";
    public static final String PRODUCT_ACTION_TAG_BESTANDSNACHWEIS_KOM =
        "custom.alkis.product.bestandsnachweis_kom@WUNDA_BLAU";
    public static final String PRODUCT_ACTION_TAG_BESTANDSNACHWEIS_KOM_INTERN =
        "custom.alkis.product.bestandsnachweis_kom_intern@WUNDA_BLAU";
    public static final String PRODUCT_ACTION_TAG_BESTANDSNACHWEIS_STICHSTAGSBEZOGEN_NRW =
        "custom.alkis.product.bestandsnachweis_stichtagsbezogen_nrw@WUNDA_BLAU";
    public static final String PRODUCT_ACTION_TAG_GRUNDSTUECKSNACHWEIS_NRW =
        "custom.alkis.product.grundstuecksnachweis_nrw@WUNDA_BLAU";
    public static final String PRODUCT_ACTION_TAG_FLURSTUECKSNACHWEIS =
        "custom.alkis.product.flurstuecksnachweis@WUNDA_BLAU";
    public static final String PRODUCT_ACTION_TAG_FLURSTUECKS_EIGENTUMSNACHWEIS_NRW =
        "custom.alkis.product.flurstuecks_eigentumsnachweis_nrw@WUNDA_BLAU";
    public static final String PRODUCT_ACTION_TAG_FLURSTUECKS_EIGENTUMSNACHWEIS_KOM =
        "custom.alkis.product.flurstuecks_eigentumsnachweis_kom@WUNDA_BLAU";
    public static final String PRODUCT_ACTION_TAG_FLURSTUECKS_EIGENTUMSNACHWEIS_KOM_INTERN =
        "custom.alkis.product.flurstuecks_eigentumsnachweis_kom_intern@WUNDA_BLAU";
    public static final String PRODUCT_ACTION_TAG_KARTE = "custom.alkis.product.karte@WUNDA_BLAU";
    public static final String PRODUCT_ACTION_TAG_BAULASTBESCHEINIGUNG_ENABLED =
        "baulast.report.bescheinigung_enabled@WUNDA_BLAU";
    public static final String PRODUCT_ACTION_TAG_BAULASTBESCHEINIGUNG_DISABLED =
        "baulast.report.bescheinigung_disabled@WUNDA_BLAU";

    public static final String ALKIS_HTML_PRODUCTS_ENABLED = "custom.alkis.products.html.enabled";
    public static final String ALKIS_EIGENTUEMER = "custom.alkis.buchungsblatt@WUNDA_BLAU";

    public static final String ADRESS_HERKUNFT_KATASTERAMT = "Katasteramt";
    public static final String ADRESS_HERKUNFT_GRUNDBUCHAMT = "Grundbuchamt";

    private static final DateFormat DF = new SimpleDateFormat("dd.MM.yyyy");

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Type {

        //~ Enum constants -----------------------------------------------------

        FLURSTUECKSNACHWEIS_PDF, FLURSTUECKSNACHWEIS_HTML, FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_PDF,
        FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_HTML, FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF,
        FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_HTML, FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_PDF,
        FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_HTML,

        BESTANDSNACHWEIS_NRW_PDF, BESTANDSNACHWEIS_STICHTAGSBEZOGEN_NRW_PDF, BESTANDSNACHWEIS_NRW_HTML,
        BESTANDSNACHWEIS_KOMMUNAL_PDF, BESTANDSNACHWEIS_KOMMUNAL_HTML, BESTANDSNACHWEIS_KOMMUNAL_INTERN_PDF,
        BESTANDSNACHWEIS_KOMMUNAL_INTERN_HTML, GRUNDSTUECKSNACHWEIS_NRW_PDF, GRUNDSTUECKSNACHWEIS_NRW_HTML,

        PUNKTLISTE_PDF, PUNKTLISTE_HTML, PUNKTLISTE_TXT
    }

    //~ Instance fields --------------------------------------------------------

    private final List<AlkisProductDescription> alkisMapProducts;
    private final String nachverarbeitungScript;

    private final AlkisConf alkisConf;
    //
    private final Map<Type, String> productMap = new HashMap();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlkisProducts object.
     *
     * @param   alkisConf               DOCUMENT ME!
     * @param   productProperties       DOCUMENT ME!
     * @param   formatProperties        DOCUMENT ME!
     * @param   produktbeschreibungXml  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public AlkisProducts(final AlkisConf alkisConf,
            final Properties productProperties,
            final Properties formatProperties,
            final String produktbeschreibungXml) throws Exception {
        this.alkisConf = alkisConf;

        POST_HEADER.put(HEADER_CONTENTTYPE_KEY, HEADER_CONTENTTYPE_VALUE_POST);

        final List<AlkisProductDescription> mapProducts = new ArrayList<>();
        final Map<String, Point> formatMap = new HashMap<>();
        alkisMapProducts = Collections.unmodifiableList(mapProducts);
        nachverarbeitungScript = productProperties.getProperty("NACHVERARBEITUNG_SCRIPT");
        productMap.put(Type.FLURSTUECKSNACHWEIS_PDF, productProperties.getProperty("FLURSTUECKSNACHWEIS_PDF"));
        productMap.put(Type.FLURSTUECKSNACHWEIS_HTML, productProperties.getProperty("FLURSTUECKSNACHWEIS_HTML"));
        productMap.put(
            Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF,
            productProperties.getProperty("FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF"));
        productMap.put(
            Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_HTML,
            productProperties.getProperty("FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_HTML"));
        productMap.put(
            Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_PDF,
            productProperties.getProperty("FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_PDF"));
        productMap.put(
            Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_HTML,
            productProperties.getProperty("FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_HTML"));
        productMap.put(
            Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_PDF,
            productProperties.getProperty("FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_PDF"));
        productMap.put(
            Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_HTML,
            productProperties.getProperty("FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_HTML"));

        productMap.put(
            Type.GRUNDSTUECKSNACHWEIS_NRW_PDF,
            productProperties.getProperty("GRUNDSTUECKSNACHWEIS_NRW_PDF"));
        productMap.put(
            Type.GRUNDSTUECKSNACHWEIS_NRW_HTML,
            productProperties.getProperty("GRUNDSTUECKSNACHWEIS_NRW_HTML"));
        productMap.put(Type.BESTANDSNACHWEIS_NRW_PDF, productProperties.getProperty("BESTANDSNACHWEIS_NRW_PDF"));
        productMap.put(
            Type.BESTANDSNACHWEIS_STICHTAGSBEZOGEN_NRW_PDF,
            productProperties.getProperty("BESTANDSNACHWEIS_STICHTAGSBEZOGEN_NRW_PDF"));
        productMap.put(Type.BESTANDSNACHWEIS_NRW_HTML, productProperties.getProperty("BESTANDSNACHWEIS_NRW_HTML"));
        productMap.put(
            Type.BESTANDSNACHWEIS_KOMMUNAL_PDF,
            productProperties.getProperty("BESTANDSNACHWEIS_KOMMUNAL_PDF"));
        productMap.put(
            Type.BESTANDSNACHWEIS_KOMMUNAL_HTML,
            productProperties.getProperty("BESTANDSNACHWEIS_KOMMUNAL_HTML"));
        productMap.put(
            Type.BESTANDSNACHWEIS_KOMMUNAL_INTERN_PDF,
            productProperties.getProperty("BESTANDSNACHWEIS_KOMMUNAL_INTERN_PDF"));
        productMap.put(
            Type.BESTANDSNACHWEIS_KOMMUNAL_INTERN_HTML,
            productProperties.getProperty("BESTANDSNACHWEIS_KOMMUNAL_INTERN_HTML"));

        productMap.put(Type.PUNKTLISTE_PDF, productProperties.getProperty("PUNKTLISTE_PDF"));
        productMap.put(Type.PUNKTLISTE_HTML, productProperties.getProperty("PUNKTLISTE_HTML"));
        productMap.put(Type.PUNKTLISTE_TXT, productProperties.getProperty("PUNKTLISTE_TXT"));

        final Document document = new SAXBuilder().build(new StringReader(produktbeschreibungXml));
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
                                    final String layoutDim = formatProperties.getProperty(dinFormatCode);
                                    int width = -1;
                                    int height = -1;
                                    if (layoutDim == null) {
                                        org.apache.log4j.Logger.getLogger(ServerAlkisProducts.class)
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
                                        final float toX = stempelFeldInfoElement.getAttribute("toX").getFloatValue();
                                        final float toY = stempelFeldInfoElement.getAttribute("toY").getFloatValue();
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
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<AlkisProductDescription> getAlkisMapProducts() {
        return alkisMapProducts;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getNachverarbeitungScript() {
        return nachverarbeitungScript;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   type  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String get(final Type type) {
        return productMap.get(type);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   pointBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String getPointDataForProduct(final CidsBean pointBean) {
        final StringBuilder sb = new StringBuilder("AX_");
        sb.append(pointBean.getProperty("pointtype"));
        sb.append(":");
        sb.append(pointBean.getProperty("pointcode"));
        return sb.toString().replace(" ", "");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   pointcode  dgkBlattnummer the value of dgkBlattnummer
     *
     * @return  DOCUMENT ME!
     */
    public Collection<String> getCorrespondingPointDocuments(final String pointcode) {
        final Collection<String> validDocuments = new LinkedList<>();

        // The pointcode of a alkis point has a specific format:
        // 25xx56xx1xxxxx
        // ^  ^
        // |  Part 2 of the "Kilometerquadrat"
        // Part 1 of the "Kilometerquadrat"
        if ((pointcode == null) || (pointcode.trim().length() < 9) || (pointcode.trim().length() > 15)) {
            return validDocuments;
        }

        final StringBuilder urlBuilder;
        if (pointcode.trim().length() < 15) {
            final String kilometerquadratPart1 = pointcode.substring(2, 4);
            final String kilometerquadratPart2 = pointcode.substring(6, 8);

            urlBuilder = new StringBuilder(alkisConf.getApmapsHost());
            urlBuilder.append(kilometerquadratPart1);
            urlBuilder.append(kilometerquadratPart2);
            urlBuilder.append('/');
            urlBuilder.append(alkisConf.getApmapsPrefix());
            urlBuilder.append(pointcode);
            urlBuilder.append('.');
        } else {
            urlBuilder = new StringBuilder(alkisConf.getApmapsEtrsHost());
            urlBuilder.append(alkisConf.getApmapsPrefix());
            urlBuilder.append(pointcode);
            urlBuilder.append('.');
        }
        for (final String suffix : SUFFIXES) {
            validDocuments.add(urlBuilder.toString() + suffix);
        }
        return validDocuments;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   dgkBlattnummer  the value of dgkBlattnummer
     * @param   laufendeNummer  the value of laufendeNummer
     *
     * @return  DOCUMENT ME!
     */
    public Collection<String> getCorrespondingNivPURLs(final java.lang.String dgkBlattnummer,
            final String laufendeNummer) {
        final Collection<String> validDocuments = new LinkedList<>();
        final StringBuilder urlBuilder = new StringBuilder(alkisConf.getNivpHost());
        urlBuilder.append(dgkBlattnummer);
        urlBuilder.append('/');
        urlBuilder.append(alkisConf.getNivpPrefix());
        urlBuilder.append(dgkBlattnummer);
        urlBuilder.append(getFormattedLaufendeNummerNivP(laufendeNummer));
        urlBuilder.append('.');
        final String documentWithoutPrefix = urlBuilder.toString();
        for (final String suffix : SUFFIXES) {
            validDocuments.add(documentWithoutPrefix + suffix);
        }
        return validDocuments;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   laufendeNummer  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String getFormattedLaufendeNummerNivP(final String laufendeNummer) {
        final StringBuilder result;

        if (laufendeNummer == null) {
            result = new StringBuilder("000");
        } else {
            result = new StringBuilder(laufendeNummer);
        }

        while (result.length() < 3) {
            result.insert(0, "0");
        }

        return result.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   product  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getActionTag(final String product) {
        final String actionTag;
        if (get(AlkisProducts.Type.FLURSTUECKSNACHWEIS_PDF).equals(product)
                    || get(AlkisProducts.Type.FLURSTUECKSNACHWEIS_HTML).equals(
                        product)) {
            actionTag = PRODUCT_ACTION_TAG_FLURSTUECKSNACHWEIS;
        } else if (get(AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_PDF).equals(product)
                    || get(
                        AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_HTML).equals(product)) {
            actionTag = PRODUCT_ACTION_TAG_FLURSTUECKS_EIGENTUMSNACHWEIS_NRW;
        } else if (get(
                        AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_PDF).equals(product)
                    || get(
                        AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_HTML).equals(
                        product)) {
            actionTag = PRODUCT_ACTION_TAG_FLURSTUECKS_EIGENTUMSNACHWEIS_KOM_INTERN;
        } else if (get(
                        AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF).equals(product)
                    || get(
                        AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_HTML).equals(product)) {
            actionTag = PRODUCT_ACTION_TAG_FLURSTUECKS_EIGENTUMSNACHWEIS_KOM;
        } else if (get(AlkisProducts.Type.BESTANDSNACHWEIS_KOMMUNAL_PDF).equals(
                        product)
                    || get(AlkisProducts.Type.BESTANDSNACHWEIS_KOMMUNAL_HTML).equals(product)) {
            actionTag = PRODUCT_ACTION_TAG_BESTANDSNACHWEIS_KOM;
        } else if (get(AlkisProducts.Type.BESTANDSNACHWEIS_KOMMUNAL_INTERN_PDF).equals(
                        product)
                    || get(
                        AlkisProducts.Type.BESTANDSNACHWEIS_KOMMUNAL_INTERN_HTML).equals(product)) {
            actionTag = PRODUCT_ACTION_TAG_BESTANDSNACHWEIS_KOM_INTERN;
        } else if (get(AlkisProducts.Type.BESTANDSNACHWEIS_NRW_PDF).equals(product)
                    || get(AlkisProducts.Type.BESTANDSNACHWEIS_NRW_HTML).equals(
                        product)) {
            actionTag = PRODUCT_ACTION_TAG_BESTANDSNACHWEIS_NRW;
        } else if (get(
                        AlkisProducts.Type.BESTANDSNACHWEIS_STICHTAGSBEZOGEN_NRW_PDF).equals(product)) {
            actionTag = PRODUCT_ACTION_TAG_BESTANDSNACHWEIS_STICHSTAGSBEZOGEN_NRW;
        } else if (get(AlkisProducts.Type.GRUNDSTUECKSNACHWEIS_NRW_PDF).equals(product)
                    || get(AlkisProducts.Type.GRUNDSTUECKSNACHWEIS_NRW_HTML).equals(product)) {
            actionTag = PRODUCT_ACTION_TAG_GRUNDSTUECKSNACHWEIS_NRW;
        } else {
            actionTag = "3wbgW§$%Q&/"; // unknown product, prevent NPE while checking action tag with null
        }
        return actionTag;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   product  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getProductName(final String product) {
        final String downloadTitle;
        if (get(AlkisProducts.Type.FLURSTUECKSNACHWEIS_PDF).equals(product)
                    || get(AlkisProducts.Type.FLURSTUECKSNACHWEIS_HTML).equals(
                        product)) {
            downloadTitle = "Flurstücksnachweis";
        } else if (get(AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_PDF).equals(product)
                    || get(
                        AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_HTML).equals(product)) {
            downloadTitle = "Flurstücks- und Eigentumsnachweis NRW";
        } else if (get(
                        AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF).equals(product)
                    || get(
                        AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_HTML).equals(product)) {
            downloadTitle = "Flurstücks- und Eigentumsnachweis (kommunal)";
        } else if (get(
                        AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_PDF).equals(product)
                    || get(
                        AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_INTERN_HTML).equals(
                        product)) {
            downloadTitle = "Flurstücks- und Eigentumsnachweis (NRW, intern)";
        } else if (get(AlkisProducts.Type.BESTANDSNACHWEIS_NRW_PDF).equals(product)
                    || get(AlkisProducts.Type.BESTANDSNACHWEIS_NRW_HTML).equals(
                        product)) {
            downloadTitle = "Bestandsnachweis (NRW)";
        } else if (get(
                        AlkisProducts.Type.BESTANDSNACHWEIS_STICHTAGSBEZOGEN_NRW_PDF).equals(product)) {
            downloadTitle = "Bestandsnachweis stichtagsbezogen (NRW)";
        } else if (get(AlkisProducts.Type.BESTANDSNACHWEIS_KOMMUNAL_PDF).equals(
                        product)
                    || get(AlkisProducts.Type.BESTANDSNACHWEIS_KOMMUNAL_HTML).equals(product)) {
            downloadTitle = "Bestandsnachweis (kommunal)";
        } else if (get(AlkisProducts.Type.BESTANDSNACHWEIS_KOMMUNAL_INTERN_PDF).equals(
                        product)
                    || get(
                        AlkisProducts.Type.BESTANDSNACHWEIS_KOMMUNAL_INTERN_HTML).equals(product)) {
            downloadTitle = "Bestandsnachweis (NRW, intern)";
        } else if (get(AlkisProducts.Type.GRUNDSTUECKSNACHWEIS_NRW_PDF).equals(product)
                    || get(AlkisProducts.Type.GRUNDSTUECKSNACHWEIS_NRW_HTML).equals(product)) {
            downloadTitle = "Grundstücksnachweis (NRW)";
        } else {
            downloadTitle = null;
        }
        return downloadTitle;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   product  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getBillingKey(final String product) {
        final String billingKey;
        if (get(AlkisProducts.Type.FLURSTUECKSNACHWEIS_PDF).equals(product)) {
            billingKey = "fsnw";
        } else if (get(
                        AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_NRW_PDF).equals(product)) {
            billingKey = "fsuenw";
        } else if (get(
                        AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF).equals(product)) {
            billingKey = "fsuekom";
        } else if (get(AlkisProducts.Type.BESTANDSNACHWEIS_NRW_PDF).equals(
                        product)) {
            billingKey = "benw";
        } else if (get(
                        AlkisProducts.Type.BESTANDSNACHWEIS_STICHTAGSBEZOGEN_NRW_PDF).equals(product)) {
            billingKey = "bestnw";
        } else if (get(AlkisProducts.Type.BESTANDSNACHWEIS_KOMMUNAL_PDF).equals(
                        product)) {
            billingKey = "bekom";
        } else if (get(AlkisProducts.Type.GRUNDSTUECKSNACHWEIS_NRW_PDF).equals(
                        product)) {
            billingKey = "grnw";
        } else {
            billingKey = null;
        }
        return billingKey;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   bean         DOCUMENT ME!
     * @param   description  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String generateLinkFromCidsBean(final CidsBean bean, final String description) {
        if ((bean != null) && (description != null)) {
            final int objectID = bean.getMetaObject().getId();
            final StringBuilder result = new StringBuilder("<a href=\"");
//            result.append(bean.getMetaObject().getMetaClass().getID()).append(LINK_SEPARATOR_TOKEN).append(objectID);
            result.append(bean.getMetaObject().getMetaClass().getID()).append(LINK_SEPARATOR_TOKEN).append(objectID);
            result.append("\">");
            result.append(description);
            result.append("</a>");
            return result.toString();
        } else {
            return "";
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   address  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String addressToString(final Address address) {
        if (address != null) {
            final StringBuilder addressStringBuilder = new StringBuilder();
            addressStringBuilder.append(getAddressBoldOpenTag(address));
            if (address.getStreet() != null) {
                addressStringBuilder.append(address.getStreet()).append(" ");
            }
            if (address.getHouseNumber() != null) {
                addressStringBuilder.append(address.getHouseNumber());
            }
            if (addressStringBuilder.length() > 0) {
                addressStringBuilder.append(NEWLINE);
            }
            if (address.getPostalCode() != null) {
                addressStringBuilder.append(address.getPostalCode()).append(" ");
            }
            if (address.getCity() != null) {
                addressStringBuilder.append(address.getCity());
            }
            if ((address.getCountry() != null) && !address.getCountry().equalsIgnoreCase("DEUTSCHLAND")) {
                addressStringBuilder.append(NEWLINE);
                addressStringBuilder.append(address.getCountry());
            }
            if (addressStringBuilder.length() > 0) {
                addressStringBuilder.append(NEWLINE);
            }
            addressStringBuilder.append(getAdressPostfix(address));
            addressStringBuilder.append(NEWLINE);
            addressStringBuilder.append(getAddressBoldCloseTag(address));
            return addressStringBuilder.toString();
        } else {
            return "";
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   address  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String getAdressPostfix(final Address address) {
        if ((address.getHerkunftAdress() != null) && address.getHerkunftAdress().equals(ADRESS_HERKUNFT_KATASTERAMT)) {
            return java.util.ResourceBundle.getBundle("de/cismet/cids/custom/wunda_blau/res/alkis/AdressPostfixStrings")
                        .getString("kataster");
        } else if ((address.getHerkunftAdress() != null)
                    && address.getHerkunftAdress().equals(ADRESS_HERKUNFT_GRUNDBUCHAMT)) {
            return java.util.ResourceBundle.getBundle("de/cismet/cids/custom/wunda_blau/res/alkis/AdressPostfixStrings")
                        .getString("grundbuch");
        } else {
            String herkunft = address.getHerkunftAdress();
            if (herkunft == null) {
                herkunft = "-";
            }
            return String.format(java.util.ResourceBundle.getBundle(
                        "de/cismet/cids/custom/wunda_blau/res/alkis/AdressPostfixStrings").getString("else"),
                    herkunft);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   address  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String getAddressBoldOpenTag(final Address address) {
        if ((address.getHerkunftAdress() != null) && address.getHerkunftAdress().equals(ADRESS_HERKUNFT_KATASTERAMT)) {
            return "<b>";
        } else {
            return "";
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   address  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String getAddressBoldCloseTag(final Address address) {
        if ((address.getHerkunftAdress() != null) && address.getHerkunftAdress().equals(ADRESS_HERKUNFT_KATASTERAMT)) {
            return "</b>";
        } else {
            return "";
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   landParcel  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String getLandparcelCodeFromParcelBeanObject(final Object landParcel) {
        if (landParcel instanceof CidsBean) {
            final CidsBean cidsBean = (CidsBean)landParcel;
            final Object parcelCodeObj = cidsBean.getProperty("alkis_id");
            if (parcelCodeObj != null) {
                return parcelCodeObj.toString();
            }
        }
        return "";
    }

    /**
     * DOCUMENT ME!
     *
     * @param   buchungsstelle  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static List<LandParcel> getLandparcelFromBuchungsstelle(final Buchungsstelle buchungsstelle) {
        final List<LandParcel> result = new ArrayList<>();
        if ((buchungsstelle.getBuchungsstellen() == null) && (buchungsstelle.getLandParcel() == null)) {
            LOG.warn("getLandparcelFromBuchungsstelle returns null. Problem on landparcel with number:"
                        + buchungsstelle.getSequentialNumber());
        } else {
            final List<LandParcel> landparcels = buchungsstelle.getLandParcel();
            if (landparcels != null) {
                result.addAll(landparcels);
            }
            if (buchungsstelle.getBuchungsstellen() != null) {
                for (final Buchungsstelle b : buchungsstelle.getBuchungsstellen()) {
                    result.addAll(getLandparcelFromBuchungsstelle(b));
                }
            }
        }
        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fullLandparcelCode  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String prettyPrintLandparcelCode(final String fullLandparcelCode) {
        final String[] tiles = fullLandparcelCode.split("-");
        switch (tiles.length) {
            case 1: {
                final String flurstueck = tiles[0];
                return _prettyPrintLandparcelCode(flurstueck);
            }
            case 2: {
                final String flurstueck = tiles[1];
                final String flur = tiles[0];
                final String result = _prettyPrintLandparcelCode(flurstueck, flur);
                return result;
            }
            case 3: {
                final String flurstueck = tiles[2];
                final String flur = tiles[1];
                final String gemarkung = tiles[0];
                return _prettyPrintLandparcelCode(flurstueck, flur, gemarkung);
            }
            default: {
                return fullLandparcelCode;
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   toEscape  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String escapeHtmlSpaces(String toEscape) {
        if (toEscape != null) {
            toEscape = toEscape.replace(" ", "%20");
        }
        return toEscape;
    }

    /**
     * ----------------private.
     *
     * @param   in  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String removeLeadingZeros(final String in) {
        return in.replaceAll("^0*", "");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   nameNumber  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String normalizeNameNumber(final String nameNumber) {
        final String[] tokens = nameNumber.split("\\.");
        final StringBuilder result = new StringBuilder();
        for (String token : tokens) {
            token = removeLeadingZeros(token);
            if (token.length() > 0) {
                result.append(token).append(".");
            }
        }
        if (result.length() > 0) {
            result.deleteCharAt(result.length() - 1);
            return result.toString();
        } else {
            return "0";
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstueck  DOCUMENT ME!
     * @param   flur        DOCUMENT ME!
     * @param   gemarkung   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String _prettyPrintLandparcelCode(final String flurstueck,
            final String flur,
            final String gemarkung) {
        return _prettyPrintLandparcelCode(flurstueck, flur) + " - Gemarkung " + gemarkung;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstueck  DOCUMENT ME!
     * @param   flur        DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String _prettyPrintLandparcelCode(final String flurstueck, final String flur) {
        return _prettyPrintLandparcelCode(flurstueck) + " - Flur " + removeLeadingZeros(flur);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstueck  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String _prettyPrintLandparcelCode(final String flurstueck) {
        return "Flurstück " + prettyPrintFlurstueck(flurstueck);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fsZahlerNenner  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String prettyPrintFlurstueck(final String fsZahlerNenner) {
        final String[] tiles = fsZahlerNenner.split("/");
        if (tiles.length == 2) {
            return removeLeadingZeros(tiles[0]) + "/" + removeLeadingZeros(tiles[1]);
        } else if (tiles.length == 1) {
            return removeLeadingZeros(tiles[0]);
        }
        return fsZahlerNenner;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   blatt  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String getBuchungsartFromBuchungsblatt(final Buchungsblatt blatt) {
        final List<Buchungsstelle> buchungsstellen = blatt.getBuchungsstellen();
        if ((buchungsstellen != null) && !buchungsstellen.isEmpty()) {
            final ArrayList<Buchungsstelle> alleStellen = new ArrayList<>();
            alleStellen.addAll(buchungsstellen);
            if (isListOfSameBuchungsart(alleStellen)) {
                return alleStellen.get(0).getBuchungsart();
            } else {
                return "diverse";
            }
        }
        return "";
    }

    /**
     * Check if in list of Buchungsstellen, all Buchungsstellen have the same Buchungsart. Return true if all have the
     * same Buchungsart, false otherwise. The check is realized with adding the buchungsart to a set. As soon the set
     * contains a second buchungsart, false can be returned.
     *
     * @param   buchungsstellen  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static boolean isListOfSameBuchungsart(final List<Buchungsstelle> buchungsstellen) {
        final Set<String> set = new HashSet<>();
        for (final Buchungsstelle o : buchungsstellen) {
            if (set.isEmpty()) {
                set.add(o.getBuchungsart());
            } else {
                if (set.add(o.getBuchungsart())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   aufteilungsnummer  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String prettyPrintAufteilungsnummer(final String aufteilungsnummer) {
        if (aufteilungsnummer != null) {
            return "ATP Nr. " + aufteilungsnummer;
        } else {
            return "";
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fraction  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String prettyPrintFraction(final String fraction) {
        if (fraction != null) {
            return "Anteil " + fraction;
        }
        return "";
    }

    /**
     * fixes the buchungsblatt code.
     *
     * @param   buchungsblattCode  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String fixBuchungslattCode(final String buchungsblattCode) {
        boolean useNewVersion = false;

        if (AlkisRestAction.getAlkisAccessProvider().getAlkisRestConf().getNewRestServiceUsed()) {
            useNewVersion = true;
        } else {
            useNewVersion = false;
        }

        return fixBuchungslattCode(buchungsblattCode, useNewVersion);
    }

    /**
     * fixes the buchungsblattcode. Beschreibung aus dem Migrationskonzept: "Folgende Definition ist einzuhalten: Die
     * Elemente sind rechtsbündig zu belegen, fehlende Stellen sind mit führenden Nullen zu belegen. Es ergibt sich kein
     * Leerzeichen am Ende des Buchungsblattkennzeichens bei fehlender Buchstabenerweiterung. Die Gesamtlänge des
     * Buchungsblattkennzeichens beträgt immer 13 Zeichen." (Anmerkung therter: 13 Zeichen ohne Bindestrich).
     *
     * @param   buchungsblattCode  the code to fix
     * @param   newVersion         DOCUMENT ME!
     *
     * @return  the fixed code
     */
    public static String fixBuchungslattCode(final String buchungsblattCode, final boolean newVersion) {
        if (buchungsblattCode != null) {
            if (newVersion) {
                final StringBuffer buchungsblattCodeSB = new StringBuffer();

                if (((buchungsblattCode.length() < 14) || buchungsblattCode.endsWith(" "))
                            && buchungsblattCode.contains("-")) {
                    String blattCode = buchungsblattCode;

                    if (blattCode.endsWith(" ")) {
                        blattCode = blattCode.substring(0, blattCode.length() - 1);
                    }
                    buchungsblattCodeSB.append(blattCode.substring(0, blattCode.indexOf("-") + 1));

                    for (int i = 0; i < (14 - blattCode.length()); ++i) {
                        buchungsblattCodeSB.append("0");
                    }
                    buchungsblattCodeSB.append(blattCode.substring(blattCode.indexOf("-") + 1));
                    return buchungsblattCodeSB.toString();
                } else {
                    return buchungsblattCode;
                }
            } else {
                final StringBuffer buchungsblattCodeSB = new StringBuffer(buchungsblattCode);
                // Fix SICAD-API-strangeness...
                while (buchungsblattCodeSB.length() < 14) {
                    buchungsblattCodeSB.append(" ");
                }
                return buchungsblattCodeSB.toString();
            }
        } else {
            return "";
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   level              DOCUMENT ME!
     * @param   namensnummerUuids  DOCUMENT ME!
     * @param   namensnummernMap   DOCUMENT ME!
     * @param   ownerHashMap       DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String buchungsblattOwnersToHtml(final int level,
            final List<String> namensnummerUuids,
            final HashMap<String, Namensnummer> namensnummernMap,
            final HashMap<String, Owner> ownerHashMap) {
        final String style;
        if (level > 0) {
            style = "style=\"border-left: 1px solid black\"";
        } else {
            style = "";
        }

        final StringBuffer sb = new StringBuffer(
                "<table cellspacing=\"0\" "
                        + style
                        + " cellpadding=\"10\" border=\"0\" align=\"left\" valign=\"top\">");

        for (final String uuid : namensnummerUuids) {
            final Namensnummer namensnummer = namensnummernMap.get(uuid);

            sb.append("<tr>");

            if (namensnummer.getArtRechtsgemeinschaft() != null) {
                final String artRechtsgemeinschaft = namensnummer.getArtRechtsgemeinschaft().trim();
                sb.append("<td width=\"80\">")
                        .append("ohne Nr.")
                        .append("</td><td><b>")
                        .append(artRechtsgemeinschaft.equals("Sonstiges") ? "Rechtsgemeinschaft"
                                                                          : artRechtsgemeinschaft)
                        .append(":</b> ")
                        .append((namensnummer.getBeschriebRechtsgemeinschaft() != null)
                                    ? namensnummer.getBeschriebRechtsgemeinschaft() : "")
                        .append(NEWLINE);
            } else {
                final Owner owner = ownerHashMap.get(namensnummer.getEigentuemerUUId());
                sb.append(buchungsblattOwnerToHtml(namensnummer, owner));
            }

            sb.append("<td style=\"padding-left: 30px\">");
            if ((namensnummer.getZaehler() != null) && (namensnummer.getNenner() != null)) {
                final String part = namensnummer.getZaehler().intValue() + "/"
                            + namensnummer.getNenner().intValue();
                sb.append("<nobr>").append("zu ").append(part).append("</nobr>");
            }
            sb.append("</td></tr>");
        }

        sb.append("</table>");
        return sb.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   buchungsblatt  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String buchungsblattOwnersToHtml(final Buchungsblatt buchungsblatt) {
        final HashMap<String, Owner> ownersMap = extractOwnersMap(buchungsblatt);
        final HashMap<String, Namensnummer> namensnummernMap = extractNamensnummernMap(buchungsblatt);
        final List<String> rootUuids = getSortedRootUuids(namensnummernMap);

        return buchungsblattOwnersToHtml(0, rootUuids, namensnummernMap, ownersMap);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   buchungsblatt  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static HashMap<String, Owner> extractOwnersMap(final Buchungsblatt buchungsblatt) {
        final HashMap<String, Owner> ownersMap = new HashMap<>();
        for (final Owner owner : buchungsblatt.getOwners()) {
            ownersMap.put(owner.getOwnerId(), owner);
        }
        return ownersMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   namensnummernMap  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static List<String> getSortedRootUuids(final HashMap<String, Namensnummer> namensnummernMap) {
        final List<String> redirectedUuids = new ArrayList<>();
        for (final Namensnummer namensnummer : namensnummernMap.values()) {
            final String uuid = namensnummer.getUuid();
            namensnummernMap.put(uuid, namensnummer);
            final List<String> namensnummerUuids = namensnummer.getNamensnummernUUIds();
            if (namensnummerUuids != null) {
                redirectedUuids.addAll(namensnummerUuids);
            }
        }

        final List<String> rootUuids = new ArrayList<>(namensnummernMap.keySet());
//        rootUuids.removeAll(redirectedUuids);
        Collections.sort(rootUuids, new NamensnummerComparator(namensnummernMap));
        return rootUuids;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   buchungsblatt  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static HashMap<String, Namensnummer> extractNamensnummernMap(final Buchungsblatt buchungsblatt) {
        final HashMap<String, Namensnummer> namensnummernMap = new HashMap<>();
        for (final Namensnummer namensnummer : buchungsblatt.getNamensnummern()) {
            namensnummernMap.put(namensnummer.getUuid(), namensnummer);
        }
        return namensnummernMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   namensnummer  DOCUMENT ME!
     * @param   owner         DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String buchungsblattOwnerToHtml(final Namensnummer namensnummer, final Owner owner) {
        final StringBuffer sb = new StringBuffer();
        sb.append("<td width=\"60\">");
        if (owner.getNameNumber() != null) {
            sb.append(normalizeNameNumber(namensnummer.getLaufendeNummer()));
        }
        sb.append("</td><td><p>");
        if (owner.getSalutation() != null) {
            sb.append(owner.getSalutation()).append(" ");
        }
        if (owner.getForeName() != null) {
            sb.append(owner.getForeName()).append(" ");
        }
        if (owner.getSurName() != null) {
            sb.append(owner.getSurName());
        }
        if (owner.getDateOfBirth() != null) {
            Date date;
            try {
                date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(owner.getDateOfBirth());
            } catch (final Exception ex) {
                date = null;
            }
            sb.append(", *").append(DF.format(date));
        }
        sb.append("</p>");
        if (owner.getNameOfBirth() != null) {
            sb.append("<p>").append("geb. ").append(owner.getNameOfBirth()).append("</p>");
        }
        final List<Address> addresses = owner.getAddresses();
        if (addresses != null) {
            boolean first = true;
            for (final Address address : addresses) {
                if ((address != null) && (address.getHerkunftAdress() != null)
                            && address.getHerkunftAdress().equals(ADRESS_HERKUNFT_KATASTERAMT)) {
                    sb.append(first ? "" : NEWLINE).append("<p>").append(addressToString(address)).append("</p>");
                }
                first = false;
            }
            for (final Address address : addresses) {
                if ((address != null)
                            && ((address.getHerkunftAdress() == null)
                                || (!address.getHerkunftAdress().equals(ADRESS_HERKUNFT_KATASTERAMT)))) {
                    sb.append(first ? "" : NEWLINE).append("<p>").append(addressToString(address)).append("</p>");
                }
                first = false;
            }
        }

        sb.append("</td>");

        return sb.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   originatingFlurstueck  DOCUMENT ME!
     * @param   buchungsblatt          DOCUMENT ME!
     * @param   buchungsblattBean      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String buchungsblattToHtml(final CidsBean originatingFlurstueck,
            final Buchungsblatt buchungsblatt,
            final CidsBean buchungsblattBean) {
        final String alkisId = (String)originatingFlurstueck.getProperty("alkis_id");

        String pos = "";
        final List<Buchungsstelle> buchungsstellen = buchungsblatt.getBuchungsstellen();
        for (final Buchungsstelle b : buchungsstellen) {
            for (final LandParcel lp : getLandparcelFromBuchungsstelle(b)) {
                if (lp.getLandParcelCode().equals(alkisId)) {
                    pos = b.getSequentialNumber();
                }
            }
        }

        final List<Owner> owners = buchungsblatt.getOwners();
        if ((owners != null) && (owners.size() > 0)) {
            final StringBuilder sb = new StringBuilder();
            sb.append(
                "<table cellspacing=\"0\" cellpadding=\"10\" border=\"0\" align=\"left\" valign=\"top\">");
//            infoBuilder.append("<tr><td width=\"200\"><b><a href=\"").append(generateBuchungsblattLinkInfo(buchungsblatt)).append("\">").append(buchungsblatt.getBuchungsblattCode()).append("</a></b></td><td>");
            sb.append("<tr><td width=\"200\">Nr. ")
                    .append(pos)
                    .append(" auf  <b>")
                    .append(generateLinkFromCidsBean(
                                buchungsblattBean,
                                buchungsblatt.getBuchungsblattCode()))
                    .append("</b></td><td>");
//            final Iterator<Owner> ownerIterator = owners.iterator();
//            if (ownerIterator.hasNext()) {
//                infoBuilder.append(ownerToString(ownerIterator.next(), ""));
//            }
            sb.append(buchungsblattOwnersToHtml(buchungsblatt));
            sb.append("</td></tr>");
            sb.append("</table>");
//            infoBuilder.append("</html>");
            return sb.toString();
//            lblBuchungsblattEigentuemer.setText(infoBuilder.toString());
        } else {
            return "";
//            lblBuchungsblattEigentuemer.setText("-");
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static class NamensnummerComparator implements Comparator<String> {

        //~ Instance fields ----------------------------------------------------

        private final HashMap<String, Namensnummer> namensnummernMap;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new NnComparator object.
         *
         * @param  namensnummernMap  DOCUMENT ME!
         */
        public NamensnummerComparator(final HashMap<String, Namensnummer> namensnummernMap) {
            this.namensnummernMap = namensnummernMap;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public int compare(final String einzelUuid1, final String einzelUuid2) {
            final Namensnummer n1 = namensnummernMap.get(einzelUuid1);
            final Namensnummer n2 = namensnummernMap.get(einzelUuid2);

            final String tmp1 = ((n1 == null) || (n1.getLaufendeNummer() == null)) ? "ZZZ" : n1.getLaufendeNummer();
            final String tmp2 = ((n2 == null) || (n2.getLaufendeNummer() == null)) ? "ZZZ" : n2.getLaufendeNummer();
            final String[] lfds1 = tmp1.split("\\.");
            final String[] lfds2 = tmp2.split("\\.");

            for (int i = 0; i < Math.max(lfds1.length, lfds2.length); i++) {
                String lfd1 = (i < lfds1.length) ? lfds1[i] : StringUtils.repeat("0", lfds2[i].length());
                String lfd2 = (i < lfds2.length) ? lfds2[i] : StringUtils.repeat("0", lfds1[i].length());

                if (lfd1.length() < lfd2.length()) {
                    lfd1 = StringUtils.repeat("0", lfd2.length() - lfd1.length()) + lfd1;
                } else {
                    lfd2 = StringUtils.repeat("0", lfd1.length() - lfd2.length()) + lfd2;
                }

                final int compare = lfd1.compareTo(lfd2);
                if (compare != 0) {
                    return compare;
                }
            }

            return 0;
        }
    }
}
