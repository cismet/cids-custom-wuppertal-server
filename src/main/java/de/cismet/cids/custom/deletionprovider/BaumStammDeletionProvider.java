/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.deletionprovider;

import Sirius.server.localserver.object.AbstractCustomDeletionProvider;
import Sirius.server.localserver.object.CustomDeletionProvider;
import Sirius.server.localserver.object.DeletionProviderClientException;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.rmi.RemoteException;

import java.util.ArrayList;

import de.cismet.cids.dynamics.CidsBean;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CustomDeletionProvider.class)
public class BaumStammDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumStammDeletionProvider.class);
    private static final String TABLE_NAME = "baum_stamm";
    private static final String FIELD__ID = "id";
    private static final String FIELD__FK = "fk_stamm";
    private static final String TABLE_NAME_SEARCH_S = "baum_schaden_stamm";
    private static final String DELETE_TEXT =
        "Dieser Stammschaden kann nicht gelöscht werden, da dieser verwendet wird.";

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public boolean isMatching(final User user, final MetaObject metaObject) {
        if (!super.isMatching(user, metaObject)) {
            return false;
        }

        final CidsBean schadenBean = metaObject.getBean();
        final Integer schaden_id = (Integer)schadenBean.getProperty(FIELD__ID);

        final String queryStammInSchaden = String.format(
                "SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH_S,
                FIELD__FK,
                schaden_id);

        try {
            final ArrayList<ArrayList> artArrayS = getMetaService().performCustomSearch(
                    queryStammInSchaden,
                    getConnectionContext());
            if (!artArrayS.isEmpty()) {
                return true;
            }
        } catch (RemoteException ex) {
            LOG.error("Cannot delete stamm object", ex);
        }

        return false;
    }

    @Override
    public boolean customDeleteMetaObject(final User user, final MetaObject metaObject) throws Exception {
        // darf nicht geloescht werden
        throw new DeletionProviderClientException(DELETE_TEXT);
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}
