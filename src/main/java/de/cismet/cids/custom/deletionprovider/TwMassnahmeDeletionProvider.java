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
public class TwMassnahmeDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(TwMassnahmeDeletionProvider.class);
    private static final String TABLE_NAME = "tw_massnahmen";
    private static final String FIELD__ID = "id";
    private static final String FIELD__FK = "fk_massnahmen";
    private static final String TABLE_NAME_SEARCH = "tw_brunnen_massnahmen";
    private static final String ERROR_MESSAGE = "Der folgende Fehler ist aufgetreten: ";
    private static final String CAUSE =
        "Diese Massnahme kann nicht gelöscht werden, da diese bei mindestens einem Trinkwasserbrunnen verwendet wird.";

    //~ Instance fields --------------------------------------------------------

    private String deleteText = "Diese Massnahme kann nicht gelöscht werden, da diese verwendet wird.";

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

        final CidsBean massBean = metaObject.getBean();
        final Integer mass_id = massBean.getPrimaryKeyValue();

        final String queryMassIn = String.format(
                "SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH,
                FIELD__FK,
                mass_id);

        try {
            final ArrayList<ArrayList> massArray = getMetaService().performCustomSearch(
                    queryMassIn,
                    getConnectionContext());
            if (!massArray.isEmpty()) {
                deleteText = CAUSE;
                return true;
            }
        } catch (RemoteException ex) {
            LOG.error("Cannot delete Massnahme object", ex);
            deleteText = ERROR_MESSAGE + ex.getMessage();
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
