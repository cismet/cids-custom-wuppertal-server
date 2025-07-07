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
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.newuser.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.log4j.Logger;

import de.cismet.cids.custom.utils.GeneralUtils;
import de.cismet.cids.custom.utils.VkMailConfigJson;
import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class VkSendMailServerAction implements ServerAction, ConnectionContextStore, UserAwareServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(VkSendMailServerAction.class);
    public static final String TASK_NAME = "vkSendMail";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        ABSENDER, MAIL_ADRESS, BETREFF, CONTENT, ENCODING
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();
    private User user;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VkSendMailServerAction object.
     */
    public VkSendMailServerAction() {
    }

    /**
     * Creates a new VkSendMailHelper object.
     *
     * @param  user               DOCUMENT ME!
     * @param  connectionContext  DOCUMENT ME!
     */
    private VkSendMailServerAction(final User user,
            final ConnectionContext connectionContext) {
        this.user = user;
        this.connectionContext = connectionContext;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object o, final ServerActionParameter... params) {
        try {
            String absender = null;
            String mail_adress = null;
            String betreff = null;
            String content = null;
            String encoding = null;
            final String sendEmailEncoding = "utf-8";
            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(Parameter.ENCODING.toString())) {
                        encoding = (String)sap.getValue();
                        break;
                    }
                }
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(Parameter.ABSENDER.toString())) {
                        absender = (String)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.MAIL_ADRESS.toString())) {
                        mail_adress = (String)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.BETREFF.toString())) {
                        betreff = (encoding != null)
                            ? new String(((String)sap.getValue()).getBytes(encoding), sendEmailEncoding): (String)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.CONTENT.toString())) {
                        content = (encoding != null)
                            ? new String(((String)sap.getValue()).getBytes(encoding), sendEmailEncoding): (String)sap.getValue();
                    } 
                }
                
                final VkMailConfigJson mailConfig =
                    new ObjectMapper().readValue(ServerResourcesLoader.getInstance().loadText(
                            WundaBlauServerResources.VK_MAIL_CONFIGURATION.getValue()),
                        VkMailConfigJson.class);

                final String cmdTemplate = mailConfig.getCmdTemplate();

                return GeneralUtils.sendMail(cmdTemplate, absender, mail_adress, betreff, content);
            }
        } catch (final Exception ex) {
            LOG.error(ex, ex);
            return ex;
        }
        return null;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }

    @Override
    public void initWithConnectionContext(final ConnectionContext cc) {
        this.connectionContext = cc;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
