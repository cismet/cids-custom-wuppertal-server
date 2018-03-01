/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;
import Sirius.server.property.ServerProperties;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;

import de.cismet.cids.custom.utils.formsolutions.FormSolutionFtpClient;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionsProperties;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;
import de.cismet.cids.server.connectioncontext.ServerConnectionContext;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class FormSolutionDownloadBestellungAction implements ServerAction,
    UserAwareServerAction,
    ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(FormSolutionDownloadBestellungAction.class);
    public static final String TASK_NAME = "formSolutionDownloadBestellung";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        TYPE
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Type {

        //~ Enum constants -----------------------------------------------------

        PRODUCT, RECHNUNG
    }

    //~ Instance fields --------------------------------------------------------

    private User user;

    private ServerConnectionContext serverConnectionContext = ServerConnectionContext.create(getClass()
                    .getSimpleName());

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        if (body == null) {
            throw new RuntimeException("The body is missing.");
        } else if (!(body instanceof MetaObjectNode)) {
            throw new RuntimeException("Wrong type for body, have to be an MetaObjectNode.");
        } else {
            try {
                Type type = Type.PRODUCT;
                if (params != null) {
                    for (final ServerActionParameter sap : params) {
                        if (sap.getKey().equals(Parameter.TYPE.toString())) {
                            type = (Type)sap.getValue();
                        }
                    }
                }

                final boolean rechung = Type.RECHNUNG.equals(type);

                final MetaObjectNode mon = (MetaObjectNode)body;

                final CidsBean bestellungBean = DomainServerImpl.getServerInstance()
                            .getMetaObject(getUser(), mon.getObjectId(), mon.getClassId(), getServerConnectionContext())
                            .getBean();
                final String filePath = rechung ? (String)bestellungBean.getProperty("rechnung_dateipfad")
                                                : (String)bestellungBean.getProperty("produkt_dateipfad");

                final ServerProperties serverProps = DomainServerImpl.getServerProperties();
                final String s = serverProps.getFileSeparator();
                final String fullFilePath = (rechung ? FormSolutionsProperties.getInstance().getRechnungBasepath()
                                                     : FormSolutionsProperties.getInstance().getProduktBasepath()) + s
                            + filePath;

                final ByteArrayOutputStream out = new ByteArrayOutputStream();

                if ("/".equals(s)) {
                    FormSolutionFtpClient.getInstance().download(fullFilePath, out);
                } else {
                    FormSolutionFtpClient.getInstance().download(fullFilePath.replace("/", s), out);
                }

                return out.toByteArray();
            } catch (final Exception ex) {
                LOG.error(ex, ex);
                return ex;
            }
        }
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }

    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return serverConnectionContext;
    }

    @Override
    public void setServerConnectionContext(final ServerConnectionContext serverConnectionContext) {
        this.serverConnectionContext = serverConnectionContext;
    }
}
