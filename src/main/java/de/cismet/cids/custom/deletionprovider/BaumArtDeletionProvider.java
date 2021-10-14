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
public class BaumArtDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumArtDeletionProvider.class);
    private static final String TABLE_NAME = "baum_art";
    private static final String FIELD__ID = "id";
    private static final String FIELD__FK = "fk_art";
    private static final String TABLE_NAME_SEARCH_S = "baum_schaden";
    private static final String TABLE_NAME_SEARCH_E = "baum_ersatz";
    private static final String TABLE_NAME_SEARCH_F = "baum_festsetzung";
    private static final String TABLE_NAME_SEARCH_SORTE = "baum_sorte";
    private static final String CAUSE_SCHADEN =
        "Diese Art kann nicht gelöscht werden, da diese bei mindestens einem Schaden verwendet wird.";
    private static final String CAUSE_ERSATZ =
        "Diese Art kann nicht gelöscht werden, da diese bei mindestens einer Ersatzpflanzung verwendet wird.";
    private static final String CAUSE_FEST =
        "Diese Art kann nicht gelöscht werden, da diese bei mindestens einer Festsetzung verwendet wird.";
    private static final String CAUSE_SORTE =
        "Diese Art kann nicht gelöscht werden, da diese mindestens eine Sorte hat.";

    //~ Instance fields --------------------------------------------------------

    private String deleteText = "Diese Art kann nicht gelöscht werden, da diese verwendet wird.";

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
        final Integer art_id = (Integer)artBean.getProperty(FIELD__ID);

        final String queryArtInSchaden = String.format(
                "SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH_S,
                FIELD__FK,
                art_id);
        final String queryArtInErsatz = String.format(
                "SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH_E,
                FIELD__FK,
                art_id);
        final String queryArtInFest = String.format(
                "SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH_F,
                FIELD__FK,
                art_id);
        final String queryArtInSorte = String.format(
                "SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH_SORTE,
                FIELD__FK,
                art_id);
        try {
            final ArrayList<ArrayList> artArrayS = getMetaService().performCustomSearch(
                    queryArtInSchaden,
                    getConnectionContext());
            if (!artArrayS.isEmpty()) {
                deleteText = CAUSE_SCHADEN;
                return true;
            }
            final ArrayList<ArrayList> artArrayE = getMetaService().performCustomSearch(
                    queryArtInErsatz,
                    getConnectionContext());
            if (!artArrayE.isEmpty()) {
                deleteText = CAUSE_ERSATZ;
                return true;
            }
            final ArrayList<ArrayList> artArrayF = getMetaService().performCustomSearch(
                    queryArtInFest,
                    getConnectionContext());
            if (!artArrayF.isEmpty()) {
                deleteText = CAUSE_FEST;
                return true;
            }
            final ArrayList<ArrayList> artArraySorte = getMetaService().performCustomSearch(
                    queryArtInSorte,
                    getConnectionContext());
            if (!artArraySorte.isEmpty()) {
                deleteText = CAUSE_SORTE;
                return true;
            }
        } catch (RemoteException ex) {
            LOG.error("Cannot delete Art object", ex);
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
