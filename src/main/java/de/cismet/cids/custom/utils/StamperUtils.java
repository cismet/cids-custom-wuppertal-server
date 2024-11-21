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
package de.cismet.cids.custom.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import de.cismet.cids.custom.utils.alkis.AlkisProducts;

import de.cismet.cids.server.actions.UploadableInputStream;

import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class StamperUtils {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(StamperUtils.class);
    private static final DateFormat DATE_FORMAT_SKIPPING_LOG = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
    private static final DateFormat DATE_FORMAT_STAMPER_CONTEXT = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");

    //~ Instance fields --------------------------------------------------------

    private final StamperConf conf;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new StamperUtils object.
     *
     * @param  conf  DOCUMENT ME!
     */
    protected StamperUtils(final StamperConf conf) {
        this.conf = conf;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected StamperConf getConf() {
        return conf;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private File createUniqueTmpDir() {
        final String unique = UUID.randomUUID().toString();
        final File uniqueTmpDir = new File(getConf().getTmpDir(), unique);
        uniqueTmpDir.mkdirs();
        if (LOG.isDebugEnabled()) {
            LOG.debug("uniqueTmpDir created: " + uniqueTmpDir.toString());
        }
        return uniqueTmpDir;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   documentType  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isStampEnabledFor(final String documentType) {
        boolean isStampEnabledFor = false;
        if (documentType != null) {
            for (final String enabledFor : getConf().getEnabledFor()) {
                final String pattern = enabledFor.trim().replace("?", ".?").replace("*", ".*?");
                if (documentType.matches(pattern)) {
                    isStampEnabledFor = true;
                    break;
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("stampEnabledFor(" + documentType + ") : " + isStampEnabledFor);
        }
        return isStampEnabledFor;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isSkippingOnErrorEnabled() {
        final String onErrorSkipAndLogInto = getConf().getOnErrorSkipAndLogInto();
        return (onErrorSkipAndLogInto != null) && !onErrorSkipAndLogInto.trim().isEmpty();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public File getSkippingLogFile() {
        if (isSkippingOnErrorEnabled()) {
            return new File(getConf().getOnErrorSkipAndLogInto());
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   documentType       DOCUMENT ME!
     * @param   url                DOCUMENT ME!
     * @param   postParams         DOCUMENT ME!
     * @param   fallback           DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public UploadableInputStream stampRequest(final String documentType,
            final URL url,
            final String postParams,
            final StamperFallback fallback,
            final ConnectionContext connectionContext) throws Exception {
        if (isStampEnabledFor(documentType)) {
            try {
                return stampRequest(
                        url,
                        postParams,
                        createStamperContext(StamperContext.Type.Request, documentType, connectionContext));
            } catch (final Exception ex) {
                if (isSkippingOnErrorEnabled()) {
                    final File skippingLogFile = getSkippingLogFile();
                    LOG.info("Error while stamping request. skipping is enabled. Logging into "
                                + skippingLogFile.getCanonicalPath(),
                        ex);
                    final StringBuffer sb = new StringBuffer();
                    sb.append(DATE_FORMAT_SKIPPING_LOG.format(new Date()))
                            .append(" | stampRequest(\n\t")
                            .append(documentType)
                            .append(",\n\t")
                            .append(url.toExternalForm())
                            .append(",\n\t")
                            .append(postParams)
                            .append("\n) => ")
                            .append(ExceptionUtils.getStackTrace(ex))
                            .append("\n");
                    FileUtils.writeStringToFile(skippingLogFile, sb.toString(), "UTF-8", true);
                    return fallback.createProduct();
                } else {
                    throw ex;
                }
            }
        } else {
            return fallback.createProduct();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   url             DOCUMENT ME!
     * @param   postParams      requestParameter DOCUMENT ME!
     * @param   stamperContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private UploadableInputStream stampRequest(final URL url,
            final String postParams,
            final StamperContext stamperContext) throws Exception {
        final URL serviceUrl = new URL(getConf().getStamperService() + getConf().getStamperRequest());

        final OptionsJson optionsJson;
        if (postParams != null) {
            optionsJson = new OptionsJson(
                    OptionsJson.Method.POST,
                    new HeadersJson(AlkisProducts.HEADER_CONTENTTYPE_VALUE_POST),
                    postParams);
        } else {
            optionsJson = new OptionsJson(OptionsJson.Method.GET, null, null);
        }

        final String optionsJsonAsString = new ObjectMapper().writeValueAsString(new FetchJson(url, optionsJson));
        final String connectionContextJsonAsString = new ObjectMapper().writeValueAsString(stamperContext);
        if (LOG.isDebugEnabled()) {
            LOG.debug("serviceUrl: " + serviceUrl);
            LOG.debug("optionsJsonAsString: " + optionsJsonAsString);
            LOG.debug("connectionContextJsonAsString: " + connectionContextJsonAsString);
        }

        final File uniqueTmpDir = createUniqueTmpDir();
        final File fileRequest = new File(uniqueTmpDir, "request.json");
        final File fileContext = new File(uniqueTmpDir, "context.json");

        try {
            FileUtils.writeStringToFile(fileRequest, optionsJsonAsString, "UTF-8");
            FileUtils.writeStringToFile(fileContext, connectionContextJsonAsString, "UTF-8");

            final Collection<Part> parts = new ArrayList<>();
            parts.add(new StringPart("password", getConf().getPassword()));
            parts.add(new FilePart("requestJson", fileRequest));
            parts.add(new FilePart("context", fileContext));

            return new UploadableInputStream(
                    new SimpleHttpAccessHandler().doMultipartRequest(serviceUrl, parts.toArray(new Part[0]), null));
        } finally {
            fileContext.delete();
            fileRequest.delete();
            uniqueTmpDir.delete();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   documentType       DOCUMENT ME!
     * @param   inputStream        DOCUMENT ME!
     * @param   fallback           DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public UploadableInputStream stampDocument(final String documentType,
            final InputStream inputStream,
            final StamperFallback fallback,
            final ConnectionContext connectionContext) throws Exception {
        if (isStampEnabledFor(documentType)) {
            try {
                return stampDocument(
                        inputStream,
                        createStamperContext(StamperContext.Type.Document, documentType, connectionContext));
            } catch (final Exception ex) {
                if (isSkippingOnErrorEnabled()) {
                    final File skippingLogFile = getSkippingLogFile();
                    final StringBuffer sb = new StringBuffer();
                    sb.append(DATE_FORMAT_SKIPPING_LOG.format(new Date()))
                            .append(" | stampDocument(\n\t")
                            .append(documentType)
                            .append(",\n\t")
                            .append(inputStream)
                            .append("\n) => ")
                            .append(ExceptionUtils.getStackTrace(ex))
                            .append("\n");
                    FileUtils.writeStringToFile(skippingLogFile, sb.toString(), "UTF-8", true);
                    return fallback.createProduct();
                } else {
                    throw ex;
                }
            }
        } else {
            return fallback.createProduct();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   inputStream     DOCUMENT ME!
     * @param   stamperContext  connectionContext DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  java.lang.Exception
     */
    private UploadableInputStream stampDocument(final InputStream inputStream, final StamperContext stamperContext)
            throws Exception {
        final URL serviceUrl = new URL(getConf().getStamperService() + getConf().getStamperDocument());
        final String connectionContextJsonAsString = new ObjectMapper().writeValueAsString(stamperContext);
        if (LOG.isDebugEnabled()) {
            LOG.debug("serviceUrl: " + serviceUrl);
            LOG.debug("connectionContextJsonAsString: " + connectionContextJsonAsString);
        }

        final File uniqueTmpDir = createUniqueTmpDir();
        final File fileDocument = new File(uniqueTmpDir, "document.pdf");
        final File fileContext = new File(uniqueTmpDir, "context.json");

        try {
            FileUtils.copyInputStreamToFile(inputStream, fileDocument);
            FileUtils.writeStringToFile(fileContext, connectionContextJsonAsString, "UTF-8");

            final Collection<Part> parts = new ArrayList<>();
            parts.add(new StringPart("password", getConf().getPassword()));
            parts.add(new FilePart("document", fileDocument));
            parts.add(new FilePart("context", fileContext));

            return new UploadableInputStream(
                    new SimpleHttpAccessHandler().doMultipartRequest(serviceUrl, parts.toArray(new Part[0]), null));
        } finally {
            fileContext.delete();
            fileDocument.delete();
            uniqueTmpDir.delete();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   contextType        DOCUMENT ME!
     * @param   documentType       DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static StamperContext createStamperContext(final StamperContext.Type contextType,
            final String documentType,
            final ConnectionContext connectionContext) {
        final HashMap<String, Object> infoFields = connectionContext.getInfoFields();
        infoFields.remove(ConnectionContext.FIELD__CLIENT_IP);
        final StamperContext stamperContext = new StamperContext(
                DATE_FORMAT_STAMPER_CONTEXT.format(new Date()),
                contextType,
                documentType,
                new StamperContextInfoConnectionContext(
                    connectionContext.getCategory(),
                    infoFields));
        return stamperContext;
    }

    //~ Inner Interfaces -------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public interface StamperFallback {

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         *
         * @throws  Exception  DOCUMENT ME!
         */
        UploadableInputStream createProduct() throws Exception;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @AllArgsConstructor
    public static class FetchJson {

        //~ Instance fields ----------------------------------------------------

        private final URL url;
        private final OptionsJson options;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @AllArgsConstructor
    static class OptionsJson {

        //~ Enums --------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @version  $Revision$, $Date$
         */
        public enum Method {

            //~ Enum constants -------------------------------------------------

            POST, GET, PUT
        }

        //~ Instance fields ----------------------------------------------------

        private final Method method;
        private final HeadersJson headers;
        private final String body;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @AllArgsConstructor
    static class HeadersJson {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty("Content-Type")
        private final String contentType;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @AllArgsConstructor
    public static class StamperContext {

        //~ Enums --------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @version  $Revision$, $Date$
         */
        public enum Type {

            //~ Enum constants -------------------------------------------------

            Request, Document
        }

        //~ Instance fields ----------------------------------------------------

        private final String date;
        private final Type type;
        private final String documentType;
        private final Object info;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @AllArgsConstructor
    public static class StamperContextInfoConnectionContext {

        //~ Instance fields ----------------------------------------------------

        private final ConnectionContext.Category category;
        private final HashMap<String, Object> infoFields;
    }
}
