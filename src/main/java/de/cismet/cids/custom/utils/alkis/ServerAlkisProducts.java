/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.alkis;

import Sirius.server.newuser.User;

import java.io.UnsupportedEncodingException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.tools.StaticHtmlTools;

import static de.cismet.cids.custom.wunda_blau.search.actions.AlkisProductServerAction.LOG;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public final class ServerAlkisProducts extends AlkisProducts {

    //~ Static fields/initializers ---------------------------------------------

    private static ServerAlkisProducts INSTANCE;
    private static final SimpleDateFormat STICHTAG_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    //~ Instance fields --------------------------------------------------------

    private final SOAPAccessProvider soapAccessProvider;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ServerAlkisProducts object.
     *
     * @param   alkisConf               DOCUMENT ME!
     * @param   productProperties       DOCUMENT ME!
     * @param   formats                 DOCUMENT ME!
     * @param   produktbeschreibungXml  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private ServerAlkisProducts(final ServerAlkisConf alkisConf,
            final Properties productProperties,
            final Properties formats,
            final String produktbeschreibungXml) throws Exception {
        super(alkisConf, productProperties, formats, produktbeschreibungXml);
        soapAccessProvider = new SOAPAccessProvider(alkisConf);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    public static ServerAlkisProducts getInstance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = new ServerAlkisProducts(
                        ServerAlkisConf.getInstance(),
                        ServerResourcesLoader.getInstance().loadProperties(
                            WundaBlauServerResources.ALKIS_PRODUCTS_PROPERTIES.getValue()),
                        ServerResourcesLoader.getInstance().loadProperties(
                            WundaBlauServerResources.ALKIS_FORMATS_PROPERTIES.getValue()),
                        ServerResourcesLoader.getInstance().loadText(
                            WundaBlauServerResources.ALKIS_PRODUKTBESCHREIBUNG_XML.getValue()));
            } catch (final Exception ex) {
                throw new RuntimeException("Error while parsing Alkis Product Description!", ex);
            }
        }
        return INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   objectID           DOCUMENT ME!
     * @param   productCode        DOCUMENT ME!
     * @param   user               DOCUMENT ME!
     * @param   fertigungsVermerk  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MalformedURLException  DOCUMENT ME!
     */
    public URL productEinzelNachweisUrl(final String objectID,
            final String productCode,
            final User user,
            final String fertigungsVermerk) throws MalformedURLException {
        final String fabricationNotice = generateFabricationNotice(fertigungsVermerk);
        final StringBuilder urlBuilder = new StringBuilder(ServerAlkisConf.getInstance().getEinzelNachweisService())
                    .append("?product=").append(productCode).append("&id=").append(objectID).append(getIdentification())
                    .append(getMore());
        if (user != null) {
            try {
                urlBuilder.append("&ordernumber=").append(URLEncoder.encode(user.getName(), "UTF-8"));
            } catch (final UnsupportedEncodingException ex) {
                throw new MalformedURLException("error while encoding: " + user.getName());
            }
        }
        if (fabricationNotice != null) {
            urlBuilder.append("&fabricationNotice=").append(fabricationNotice);
        }
        return new URL(urlBuilder.toString());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   objectID     DOCUMENT ME!
     * @param   productCode  DOCUMENT ME!
     * @param   stichtag     DOCUMENT ME!
     * @param   user         DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MalformedURLException  DOCUMENT ME!
     */
    public URL productEinzelnachweisStichtagsbezogenUrl(final String objectID,
            final String productCode,
            final Date stichtag,
            final User user) throws MalformedURLException {
        final StringBuilder urlBuilder = new StringBuilder(ServerAlkisConf.getInstance().getEinzelNachweisService())
                    .append("?reportingDate=").append(STICHTAG_DATE_FORMAT.format(stichtag)).append("&product=")
                    .append(productCode)
                    .append("&id=")
                    .append(objectID)
                    .append(getIdentification())
                    .append(getMore());
        if (user != null) {
            try {
                urlBuilder.append("&ordernumber=").append(URLEncoder.encode(user.getName(), "UTF-8"));
            } catch (final UnsupportedEncodingException ex) {
                throw new MalformedURLException("error while encoding: " + user.getName());
            }
        }
        return new URL(urlBuilder.toString());
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

                return new StringBuffer("&fabricationNotice1=").append(notice1)
                            .append("&fabricationNotice2=&fabricationNotice3=")
                            .append(notice3)
                            .toString();
            } catch (final UnsupportedEncodingException ex) {
                LOG.error("error while encoding fabricationnotice", ex);
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
                return note;
            } catch (final UnsupportedEncodingException ex) {
                LOG.error("error while encoding fabricationnotice", ex);
                return null;
            }
        } else {
            return null;
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
        return new URL(new StringBuffer(ServerAlkisConf.getInstance().getLiegenschaftskarteService()).append(
                    "?landparcel=").append(parcelCode).append(getIdentification()).append(getMore()).append(
                    ((fabricationNotices != null) ? (fabricationNotices) : "")).toString());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   parcelCode         DOCUMENT ME!
     * @param   produkt            DOCUMENT ME!
     * @param   winkel             DOCUMENT ME!
     * @param   centerX            DOCUMENT ME!
     * @param   centerY            DOCUMENT ME!
     * @param   massstab           DOCUMENT ME!
     * @param   massstabMin        DOCUMENT ME!
     * @param   massstabMax        DOCUMENT ME!
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
            final String produkt,
            final int winkel,
            final int centerX,
            final int centerY,
            final String massstab,
            final String massstabMin,
            final String massstabMax,
            final String zusText,
            final String auftragsNr,
            final boolean moreThanOneParcel,
            final String fertigungsVermerk) throws MalformedURLException {
        final boolean nachverarbeitung = !"WUP.KOM.FFF.01".equals(produkt)
                    && !"WUP.KOM.FFF.02".equals(produkt)
                    && !"WUP.KOM.FFS.01".equals(produkt)
                    && !"WUP.KOM.FFS.02".equals(produkt);
        final StringBuilder url = new StringBuilder(ServerAlkisConf.getInstance().getLiegenschaftskarteService());
        url.append("?landparcel=").append(parcelCode);
        url.append("&product=").append(produkt);
        url.append("&centerx=").append(centerX);
        url.append("&centery=").append(centerY);
        url.append("&angle=").append(winkel);
        if ((zusText != null) && (zusText.length() > 0)) {
            url.append("&text=").append(StaticHtmlTools.encodeURLParameter(zusText));
        }
        if ((auftragsNr != null) && (auftragsNr.length() > 0)) {
            url.append("&ordernumber=").append(StaticHtmlTools.encodeURLParameter(auftragsNr));
        }
        if (moreThanOneParcel) {
            url.append("&additionalLandparcel=true");
        }
        url.append(getIdentification()).append(nachverarbeitung ? getMore() : getLess());
        final String fabricationNotices = generateFabricationNotices(fertigungsVermerk);
        if ((massstabMin != null) && (massstabMax != null)) {
            url.append("&scale=");
            url.append(massstab);
        }
        if (fabricationNotices != null) {
            url.append(fabricationNotices);
        }
        return new URL(url.toString());
    }

    /**
     * Returns a URL to a document for given points.
     *
     * @param   punktliste   The points.
     * @param   productCode  format The format of the document.
     *
     * @return  DOCUMENT ME!
     *
     * @throws  MalformedURLException  DOCUMENT ME!
     */
    public URL productListenNachweisUrl(final String punktliste, final String productCode)
            throws MalformedURLException {
        return new URL(new StringBuffer(ServerAlkisConf.getInstance().getListenNachweisService()).append("?product=")
                        .append(productCode).append("&ids=").append(punktliste).append(getIdentification()).append(
                    getMore()).toString());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private SOAPAccessProvider getSoapAccessProvider() {
        return soapAccessProvider;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getIdentification() {
        final String token = getSoapAccessProvider().login();
        return new StringBuffer("&token=").append(token).toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getMore() {
        return new StringBuffer("&service=").append(ServerAlkisConf.getInstance().getService())
                    .append("&script=")
                    .append(ServerAlkisProducts.getInstance().getNachverarbeitungScript())
                    .toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getLess() {
        return new StringBuffer("&service=").append(ServerAlkisConf.getInstance().getService()).toString();
    }
}
