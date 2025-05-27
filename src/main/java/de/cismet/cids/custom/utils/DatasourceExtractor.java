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

import org.apache.log4j.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import sun.misc.BASE64Encoder;

import java.io.File;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.commons.utils.datasource.DatasourcesPojoConverter;
import de.cismet.commons.utils.datasource.DatasourcesUtils;
import de.cismet.commons.utils.datasource.HtmlConverter;
import de.cismet.commons.utils.datasource.HtmlDetailConverter;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
public class DatasourceExtractor {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(DatasourceExtractor.class);

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static void extractDatasources() throws Exception {
        String[] credentialArray = null;
        Element rootObject = null;
        File file = null;
        File file2 = null;

        try {
            // read the capabilities list
            final StringReader capabilityList = ServerResourcesLoader.getInstance()
                        .loadStringReader(
                            WundaBlauServerResources.DATASOURCES_CAPABILITYLIST_TEXT.getValue());
            final SAXBuilder builder = new SAXBuilder(false);
            final Document doc = builder.build(capabilityList);

            rootObject = doc.getRootElement();
        } catch (final Exception ex) {
            LOG.error("Datasource could not load the capabilities list", ex);
        }

        try {
            // read the credentials
            final Properties credentials = ServerResourcesLoader.getInstance()
                        .loadProperties(
                            WundaBlauServerResources.DATASOURCES_CREDENTIALS_PROPERTIES.getValue());
            final BASE64Encoder base64 = new BASE64Encoder();
            final List<String> credentialList = new ArrayList<String>();

            if ((credentials.stringPropertyNames() != null)
                        && (credentials.stringPropertyNames().size() > 0)) {
                for (final String key : credentials.stringPropertyNames()) {
                    final String credential = key + ":" + credentials.getProperty(key);
                    credentialList.add(base64.encode(credential.getBytes()));
                }

                credentialArray = credentialList.toArray(
                        new String[credentials.stringPropertyNames().size()]);
            }
        } catch (final Exception ex) {
            LOG.error("Datasource could not load the credential properties", ex);
        }

        try {
            // read the output file
            final Properties general = ServerResourcesLoader.getInstance()
                        .loadProperties(
                            WundaBlauServerResources.DATASOURCES_GENERAL_PROPERTIES.getValue());
            final String filename = general.getProperty("output_file");

            if (filename != null) {
                file = new File(filename);
                if (filename.contains(".")) {
                    file2 = new File(filename.substring(0, filename.lastIndexOf(".")) + "_neu.html");
                }
            }
        } catch (final Exception ex) {
            LOG.error("Datasource could not load the general properties", ex);
        }

        if ((rootObject != null) && (file != null)) {
            DatasourcesPojoConverter converter = new HtmlConverter();
            DatasourcesUtils.createLayerListHeadless(
                rootObject,
                rootObject,
                credentialArray,
                converter,
                file);
            if (file2 != null) {
                converter = new HtmlDetailConverter();
                DatasourcesUtils.createLayerListHeadless(
                    rootObject,
                    rootObject,
                    credentialArray,
                    converter,
                    file2);
            }
        }
    }
}
