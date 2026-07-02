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
public class VkNutzungDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(VkNutzungDeletionProvider.class);
    private static final String TABLE_NAME = "vk_nutzung";
    private static final String FIELD__FK_1 = "fk_nutzung_waehrend";
    private static final String FIELD__FK_2 = "fk_nutzung_nach";
    private static final String TABLE_NAME_SEARCH = "vk_vorhaben";
    private static final String CAUSE =
        "Diese Nutzung kann nicht gelöscht werden, da diese bei mindestens einem Vorhaben verwendet wird.";

    //~ Instance fields --------------------------------------------------------

    private String deleteText = "Diese Nutzung kann nicht gelöscht werden, da diese verwendet wird.";

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

        final CidsBean nutzungBean = metaObject.getBean();
        final Integer nutzung_id = nutzungBean.getPrimaryKeyValue();

        final String queryNutzung = String.format(
                "SELECT * FROM %s WHERE %s = %d OR %s = %d;",
                TABLE_NAME_SEARCH,
                FIELD__FK_1,
                nutzung_id,
                FIELD__FK_2,
                nutzung_id);

        try {
            final ArrayList<ArrayList> nutzungArray = getMetaService().performCustomSearch(
                    queryNutzung,
                    getConnectionContext());
            if (!nutzungArray.isEmpty()) {
                deleteText = CAUSE;
                return true;
            }
        } catch (RemoteException ex) {
            LOG.error("Cannot delete Nutzung object", ex);
            return true;
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
