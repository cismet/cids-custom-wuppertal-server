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

import de.aedsicad.aaaweb.rest.api.AlkisInformationApi;
import de.aedsicad.aaaweb.rest.api.AlkisSucheApi;
import de.aedsicad.aaaweb.rest.api.TokenApi;
import de.aedsicad.aaaweb.rest.client.ApiClient;
import de.aedsicad.aaaweb.rest.model.TokenInfo;
import java.util.Date;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * TODO: Should be made (lazy) Singleton? - But check about timeouts!
 *
 * @author   srichter
 * @version  $Revision$, $Date$
 */
@Getter
public final class AlkisAccessProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(AlkisAccessProvider.class);

    //~ Instance fields --------------------------------------------------------

    private final AlkisRestConf alkisRestConf;

    private final AlkisInformationApi alkisInfoService;
    private final AlkisSucheApi alkisSearchService;
    private final TokenApi tokenService;

    private TokenInfo token;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlkisAccessProvider object.
     *
     * @param   alkisRestConf  identityCard DOCUMENT ME!
     *
     * @throws  IllegalStateException  DOCUMENT ME!
     */
    public AlkisAccessProvider(final AlkisRestConf alkisRestConf) {
        this.alkisRestConf = alkisRestConf;

        final String tokenServiceUrl = alkisRestConf.getTokenApi();
        final String aaaWebApiUrl = alkisRestConf.getAaaWebApi();

        try {
            final ApiClient tokenApiClient = new ApiClient();
            tokenApiClient.setBasePath(tokenServiceUrl);

            final ApiClient aaaWebApiClient = new ApiClient();
            aaaWebApiClient.setBasePath(aaaWebApiUrl);
            aaaWebApiClient.getHttpClient().setReadTimeout(30, TimeUnit.SECONDS);

            this.tokenService = new TokenApi(tokenApiClient);
            this.alkisInfoService = new AlkisInformationApi(aaaWebApiClient);
            this.alkisSearchService = new AlkisSucheApi(aaaWebApiClient);
        } catch (Exception ex) {
            throw new IllegalStateException("Can not create RestAccessProvider" + tokenServiceUrl + "|"
                        + aaaWebApiUrl,
                ex);
        }
    }

    //~ Methods ----------------------------------------------------------------

    public boolean isTokenValid(final String token) {
        boolean valid = false;
        try {
            valid = tokenService.getToken(token) != null;
        } catch (final Exception ex) {
            LOG.info("could not check token info. probably invalid", ex);
        }
        return valid;        
    }
    
    /**
     * DOCUMENT ME!
     *
     * @return  the identityCard
     */
    public String login() {
        try {
            boolean valid =  token != null && isTokenValid(token.getToken());
            if (!valid) {
                final String user = alkisRestConf.getCreds().getUser();
                final String pass = alkisRestConf.getCreds().getPassword();
                final String extendSecret = "";  // TODO ???

                this.token = getTokenService().createToken(user, pass, extendSecret).getToken();
            }
        } catch (final Exception ex) {
            LOG.fatal("login failed", ex);
            token = null;
            //throw new Exception("login failed", ex);
            return null;
        }
        return token.getToken();        
    }

    /**
     * DOCUMENT ME!
     */
    public void logout() {
        try {
            getTokenService().deleteToken(token.getToken());
        } catch (final Exception ex) {
        }
        token = null;
    }
}
