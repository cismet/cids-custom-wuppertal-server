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
public class VkThemaDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(VkThemaDeletionProvider.class);
    private static final String TABLE_NAME = "vk_thema";
    private static final String FIELD__FK = "fk_thema";
    private static final String TABLE_NAME_SEARCH = "vk_vorhaben";
    private static final String CAUSE =
        "Dieses Thema kann nicht gelöscht werden, da dieses bei mindestens einem Vorhaben verwendet wird.";

    //~ Instance fields --------------------------------------------------------

    private String deleteText = "Dieses Thema kann nicht gelöscht werden, da dieses verwendet wird.";

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

        final CidsBean themaBean = metaObject.getBean();
        final Integer thema_id = themaBean.getPrimaryKeyValue();

        final String queryThema = String.format("SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH,
                FIELD__FK,
                thema_id);

        try {
            final ArrayList<ArrayList> themaArray = getMetaService().performCustomSearch(
                    queryThema,
                    getConnectionContext());
            if (!themaArray.isEmpty()) {
                deleteText = CAUSE;
                return true;
            }
        } catch (RemoteException ex) {
            LOG.error("Cannot delete Thema object", ex);
        }
        return false;
    }

    @Override
    public boolean customDeleteMetaObject(final User user, final MetaObject metaObject) throws Exception {
        // darf nicht geloescht werden
        throw new DeletionProviderClientException(deleteText);
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}
