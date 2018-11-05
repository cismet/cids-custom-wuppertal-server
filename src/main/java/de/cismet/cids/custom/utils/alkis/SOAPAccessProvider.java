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
import de.aedsicad.gisportal.webservices.token.TokenServices;
import de.aedsicad.gisportal.webservices.token.TokenServicesServiceLocator;

import java.net.URL;

/**
 * TODO: Should be made (lazy) Singleton? - But check about timeouts!
 *
 * @author   srichter
 * @version  $Revision$, $Date$
 */
public final class SOAPAccessProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(SOAPAccessProvider.class);

    //~ Instance fields --------------------------------------------------------

    private final AlkisConf alkisConf;
    private final String service;
    private final ALKISCatalogServices alkisCatalogServices;
    private final ALKISInfoServices alkisInfoService;
    private final ALKISSearchServices alkisSearchService;
    private final TokenServices tokenService;
    private String aToken;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SOAPAccessProvider object.
     *
     * @param   alkisConf  identityCard DOCUMENT ME!
     *
     * @throws  IllegalStateException  DOCUMENT ME!
     */
    public SOAPAccessProvider(final ServerAlkisConf alkisConf) {
        this.alkisConf = alkisConf;
        // final String identityCard = alkisConf.getCreds().getUser() + "," + alkisConf.getCreds().getPassword();

//        final String identityCard = alkisConf.USER + "," + alkisConf.PASSWORD;
        final String serviceUrl = alkisConf.getService();
        final String tokenServiceUrl = alkisConf.getServer() + alkisConf.getTokenService();
        final String catalogServiceUrl = alkisConf.getServer() + alkisConf.getCatalogService();
        final String infoServiceUrl = alkisConf.getServer() + alkisConf.getInfoService();
        final String searchServiceUrl = alkisConf.getServer() + alkisConf.getSearchService();

        this.service = serviceUrl;
        try {
            this.tokenService = new TokenServicesServiceLocator().getTokenServices(new URL(tokenServiceUrl));
            this.alkisCatalogServices = new ALKISCatalogServicesServiceLocator().getALKISCatalogServices(new URL(
                        catalogServiceUrl));
            this.alkisInfoService = new ALKISInfoServicesServiceLocator().getALKISInfoServices(new URL(infoServiceUrl));
            this.alkisSearchService = new ALKISSearchServicesServiceLocator().getALKISSearchServices(new URL(
                        searchServiceUrl));
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
    public String login() {
        try {
            if ((aToken == null) || !getTokenService().isTokenValid(aToken)) {
                aToken = getTokenService().login(alkisConf.getUser(), alkisConf.getPassword());
            }
        } catch (final Exception ex) {
            LOG.fatal("login failed", ex);
            aToken = null;
        }
        return aToken;
    }

    /**
     * DOCUMENT ME!
     */
    public void logout() {
        try {
            getTokenService().logout(aToken);
        } catch (final Exception ex) {
        }
        aToken = null;
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
     * @return  DOCUMENT ME!
     */
    public TokenServices getTokenService() {
        return tokenService;
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
