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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import de.cismet.cids.custom.utils.alkis.AlkisProducts;

import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.connectioncontext.ConnectionContext;
import org.apache.commons.httpclient.methods.multipart.StringPart;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class StamperUtils {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(StamperUtils.class);

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
     * @param   url                DOCUMENT ME!
     * @param   postParams         requestParameter DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public byte[] stampRequest(final URL url,
            final String postParams,
            final ConnectionContext connectionContext) throws Exception {
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
        final String connectionContextJsonAsString = new ObjectMapper().writeValueAsString(connectionContext);
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

            return IOUtils.toByteArray(
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
     * @param   inputStream        DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  java.lang.Exception
     */
    public byte[] stampDocument(final InputStream inputStream, final ConnectionContext connectionContext)
            throws Exception {
        final URL serviceUrl = new URL(getConf().getStamperService() + getConf().getStamperDocument());
        final String connectionContextJsonAsString = new ObjectMapper().writeValueAsString(connectionContext);
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

            return IOUtils.toByteArray(
                    new SimpleHttpAccessHandler().doMultipartRequest(serviceUrl, parts.toArray(new Part[0]), null));
        } finally {
            fileContext.delete();
            fileDocument.delete();
            uniqueTmpDir.delete();
        }
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
}
