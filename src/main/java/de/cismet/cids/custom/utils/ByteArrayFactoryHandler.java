/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.utils;

import Sirius.server.newuser.User;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;
import de.cismet.connectioncontext.ConnectionContext;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.Properties;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author jruiz
 */
public class ByteArrayFactoryHandler {
    
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(ByteArrayFactoryHandler.class);

    private static final String PROPERTY_CMD_TEMPLATE = "cmd";
    
    private static final String PROPERTY_JWT_PLACEHOLDER = "jwt";
    private static final String PROPERTY_FACTORYCLASS_PLACEHOLDER = "factory";    
    private static final String PROPERTY_PARAMETERS_PLACEHOLDER = "parameters";    

    private static final String DEFAULT_JWT_PLACEHOLDER = "<jwt>";
    private static final String DEFAULT_FACTORYCLASS_PLACEHOLDER = "<factoryClass>";    
    private static final String DEFAULT_PARAMETERS_PLACEHOLDER = "<parameters>";    
    
    private final String cmdTemplate;
    private final String jwtPlaceholder;
    private final String factoryClassPlaceholder;
    private final String parametersPlaceholder;
    
    private ByteArrayFactoryHandler(final Properties properties) throws Exception {
        cmdTemplate = properties.getProperty(PROPERTY_CMD_TEMPLATE, null);
        
        if (cmdTemplate == null) {
            throw new Exception(String.format("The property %s can't be null !", PROPERTY_CMD_TEMPLATE));
        }
        
        jwtPlaceholder = properties.getProperty(PROPERTY_JWT_PLACEHOLDER, DEFAULT_JWT_PLACEHOLDER);
        factoryClassPlaceholder = properties.getProperty(PROPERTY_FACTORYCLASS_PLACEHOLDER, DEFAULT_FACTORYCLASS_PLACEHOLDER);
        parametersPlaceholder = properties.getProperty(PROPERTY_PARAMETERS_PLACEHOLDER, DEFAULT_PARAMETERS_PLACEHOLDER);
    }

    public String getExecutionTemplate() {
        return cmdTemplate;
    }
        
    public byte[] execute(final String factoryClassName, final String parameters, final User user, final ConnectionContext connectionContext) throws Exception {
        final String cmd = cmdTemplate
                    .replaceAll(jwtPlaceholder, user.getJwsToken())
                    .replaceAll(factoryClassPlaceholder, factoryClassName)
                    .replaceAll(parametersPlaceholder, parameters);
        if (LOG.isDebugEnabled()) {
            LOG.debug(cmd);
        }
        final String response = executeCmd(cmd);
        return Base64.getMimeDecoder().decode(response);
    }
    
    private static String executeCmd(final String cmd) throws Exception {
        final ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", cmd);
        final Process process = builder.start();
        final InputStream inputStream = process.getInputStream();
        return IOUtils.toString(new InputStreamReader(inputStream));
    }
    
    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static ByteArrayFactoryHandler getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final ByteArrayFactoryHandler INSTANCE;

        static {
            try {
                INSTANCE = new ByteArrayFactoryHandler(ServerResourcesLoader.getInstance().loadProperties(
                            WundaBlauServerResources.BYTEARRAYFACTORY_PROPERTIES.getValue()));
            } catch (final Exception ex) {
                throw new RuntimeException(String.format("Exception while initializing %s", WundaBlauServerResources.BYTEARRAYFACTORY_PROPERTIES.toString()), ex);
            }
        }

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
