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

import java.io.File;

import de.cismet.cids.custom.utils.formsolutions.FormSolutionsConstants;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.DownloadFileAction;
import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class FormSolutionDownloadBestellungAction extends DownloadFileAction implements UserAwareServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(FormSolutionDownloadBestellungAction.class);
    public static final String TASK_NAME = "formSolutionDownloadBestellung";

    //~ Instance fields --------------------------------------------------------

    private User user;

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
                final MetaObjectNode mon = (MetaObjectNode)body;

                final CidsBean bestellungBean = DomainServerImpl.getServerInstance()
                            .getMetaObject(getUser(), mon.getObjectId(), mon.getClassId())
                            .getBean();
                final String filePath = (String)bestellungBean.getProperty("produkt_dateipfad");

                final ServerProperties serverProps = DomainServerImpl.getServerProperties();
                final String s = serverProps.getFileSeparator();
                final String fullFilePath = FormSolutionsConstants.PRODUKT_BASEPATH + s
                            + filePath.replace("../", "");
                final Object ret;
                if ("/".equals(s)) {
                    ret = super.execute(fullFilePath);
                } else {
                    ret = super.execute(fullFilePath.replace("/", s));
                }
                if (ret == null) {
                    throw new RuntimeException("File not found: " + fullFilePath);
                }
                return ret;
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
}
