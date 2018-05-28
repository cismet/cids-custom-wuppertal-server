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
package de.cismet.cids.custom.utils.alkis;

import de.aedsicad.aaaweb.service.alkis.catalog.ALKISCatalogServices;
import de.aedsicad.aaaweb.service.alkis.catalog.ALKISCatalogServicesServiceLocator;
import de.aedsicad.aaaweb.service.alkis.info.ALKISInfoServices;
import de.aedsicad.aaaweb.service.alkis.info.ALKISInfoServicesServiceLocator;
import de.aedsicad.aaaweb.service.alkis.search.ALKISSearchServices;
import de.aedsicad.aaaweb.service.alkis.search.ALKISSearchServicesServiceLocator;

import java.net.URL;

/**
 * TODO: Should be made (lazy) Singleton? - But check about timeouts!
 *
 * @author   srichter
 * @version  $Revision$, $Date$
 */
public final class SOAPAccessProvider {

    //~ Instance fields --------------------------------------------------------

    private final String identityCard;
    private final String service;
    private final ALKISCatalogServices alkisCatalogServices;
    private final ALKISInfoServices alkisInfoService;
    private final ALKISSearchServices alkisSearchService;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SOAPAccessProvider object.
     *
     * @param   alkisConf  identityCard DOCUMENT ME!
     *
     * @throws  IllegalStateException  DOCUMENT ME!
     */
    public SOAPAccessProvider(final AlkisConf alkisConf) {
        final String identityCard = alkisConf.getUser() + "," + alkisConf.getPassword();
        final String service = alkisConf.getService();
        final String catalogService = alkisConf.getServer() + alkisConf.getCatalogService();
        final String infoService = alkisConf.getServer() + alkisConf.getInfoService();
        final String searchService = alkisConf.getServer() + alkisConf.getSearchService();

        this.identityCard = identityCard;
        this.service = service;
        try {
            this.alkisCatalogServices = new ALKISCatalogServicesServiceLocator().getALKISCatalogServices(new URL(
                        catalogService));
            this.alkisInfoService = new ALKISInfoServicesServiceLocator().getALKISInfoServices(new URL(infoService));
            this.alkisSearchService = new ALKISSearchServicesServiceLocator().getALKISSearchServices(new URL(
                        searchService));
        } catch (Exception ex) {
            throw new IllegalStateException("Can not create SOAPAccessProvider" + alkisConf.getServer()
                        + "|"
                        + alkisConf.getCatalogService() + "|"
                        + alkisConf.getServer() + "|"
                        + alkisConf.getInfoService() + "|"
                        + alkisConf.getServer() + "|"
                        + alkisConf.getSearchService(),
                ex);
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  the identityCard
     */
    public String getIdentityCard() {
        return identityCard;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  the service
     */
    public String getService() {
        return service;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  the alkisCatalogServices
     */
    public ALKISCatalogServices getAlkisCatalogServices() {
        return alkisCatalogServices;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  the alkisInfoService
     */
    public ALKISInfoServices getAlkisInfoService() {
        return alkisInfoService;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  the alkisSearchService
     */
    public ALKISSearchServices getAlkisSearchService() {
        return alkisSearchService;
    }
}
