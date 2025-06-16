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
public class VkKontaktDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(VkKontaktDeletionProvider.class);
    private static final String TABLE_NAME = "vk_kontakt";
    private static final String FIELD__FK = "fk_kontakt";
    private static final String TABLE_NAME_SEARCH = "vk_vorhaben";
    private static final String CAUSE =
        "Dieser Kontakt kann nicht gelöscht werden, da dieser bei mindestens einem Vorhaben verwendet wird.";

    //~ Instance fields --------------------------------------------------------

    private String deleteText = "Dieser Kontakt kann nicht gelöscht werden, da dieser verwendet wird.";

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

        final CidsBean kontaktBean = metaObject.getBean();
        final Integer kontakt_id = kontaktBean.getPrimaryKeyValue();

        final String queryKontakt = String.format("SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH,
                FIELD__FK,
                kontakt_id);

        try {
            final ArrayList<ArrayList> kontaktArray = getMetaService().performCustomSearch(
                    queryKontakt,
                    getConnectionContext());
            if (!kontaktArray.isEmpty()) {
                deleteText = CAUSE;
                return true;
            }
        } catch (RemoteException ex) {
            LOG.error("Cannot delete Kontakt object", ex);
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
