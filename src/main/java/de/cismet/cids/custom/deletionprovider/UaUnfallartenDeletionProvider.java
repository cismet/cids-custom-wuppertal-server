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
public class UaUnfallartenDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(UaUnfallartenDeletionProvider.class);
    private static final String TABLE_NAME = "ua_unfallarten";
    private static final String FIELD__ID = "id";
    private static final String FIELD__FK = "fk_unfallarten";
    private static final String TABLE_NAME_SEARCH = "ua_einsatz_unfallarten";
    private static final String CAUSE =
        "Diese Unfallart kann nicht gelöscht werden, da diese bei mindestens einem Einsatz verwendet wird.";

    //~ Instance fields --------------------------------------------------------

    private String deleteText = "Diese Unfallart kann nicht gelöscht werden, da diese verwendet wird.";

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

        final CidsBean artBean = metaObject.getBean();
        final Integer art_id = artBean.getPrimaryKeyValue();

        final String queryArtIn = String.format(
                "SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH,
                FIELD__FK,
                art_id);

        try {
            final ArrayList<ArrayList> artArray = getMetaService().performCustomSearch(
                    queryArtIn,
                    getConnectionContext());
            if (!artArray.isEmpty()) {
                deleteText = CAUSE;
                return true;
            }
        } catch (RemoteException ex) {
            LOG.error("Cannot delete Unfallart object", ex);
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
