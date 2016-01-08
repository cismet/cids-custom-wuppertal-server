/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.utils.formsolutions;

import de.cismet.cids.custom.utils.alkis.AlkisConstants;
import de.cismet.tools.PropertyReader;

/**
 *
 * @author jruiz
 */
public class FormSolutionsConstants {
    
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(AlkisConstants.class);
    
    public static final String USER;
    public static final String PASSWORD;
    
    static {
        String user;
        String password;
        
        try {
            final PropertyReader serviceProperties = new PropertyReader(
                    "/de/cismet/cids/custom/wunda_blau/res/formsolutions/fs_conf.properties");

            user = serviceProperties.getProperty("USER");
            password = serviceProperties.getProperty("PASSWORD");
        } catch (final Exception ex) {
            LOG.fatal("FormSolutionsConstants Error!", ex);
            throw new RuntimeException(ex);
        }
        
        USER = user;
        PASSWORD = password;
    }

}
