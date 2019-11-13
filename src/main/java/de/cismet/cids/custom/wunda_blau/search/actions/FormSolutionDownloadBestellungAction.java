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

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import de.cismet.cids.custom.utils.formsolutions.FormSolutionsFtpClient;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionsProperties;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class FormSolutionDownloadBestellungAction implements ServerAction,
    UserAwareServerAction,
    ConnectionContextStore {

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

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

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
                            .getMetaObject(getUser(), mon.getObjectId(), mon.getClassId(), getConnectionContext())
                            .getBean();
                final String fileName = rechung ? (String)bestellungBean.getProperty("rechnung_dateipfad")
                                                : (String)bestellungBean.getProperty("produkt_dateipfad");

                final String ftpSubDir = (rechung ? FormSolutionsProperties.getInstance().getRechnungBasepath()
                                                  : FormSolutionsProperties.getInstance().getProduktBasepath());

                final String ftpFilePath = (ftpSubDir.endsWith("/") ? ftpSubDir : (ftpSubDir + "/")) + fileName;
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                if (FormSolutionsProperties.getInstance().isFtpEnabled()) {
                    FormSolutionsFtpClient.getInstance().download(ftpFilePath, out);
                } else {
                    final String mntFile = FormSolutionsProperties.getInstance().getFtpMountAbsPath() + "/"
                                + ftpFilePath;
                    try(final InputStream in = new FileInputStream(mntFile)) {
                        IOUtils.copy(in, out);
                    }
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
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
