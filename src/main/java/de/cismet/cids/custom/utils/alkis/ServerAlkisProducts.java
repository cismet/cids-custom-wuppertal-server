/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.alkis;

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public final class ServerAlkisProducts extends AlkisProducts {

    //~ Static fields/initializers ---------------------------------------------

    private static ServerAlkisProducts INSTANCE;

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
    public ServerAlkisProducts(final AlkisConf alkisConf,
            final Properties productProperties,
            final Properties formats,
            final String produktbeschreibungXml) throws Exception {
        super(alkisConf, productProperties, formats, produktbeschreibungXml);
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
}
