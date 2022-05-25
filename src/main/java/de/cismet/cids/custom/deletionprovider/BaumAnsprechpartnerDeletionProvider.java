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
public class BaumAnsprechpartnerDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumAnsprechpartnerDeletionProvider.class);
    private static final String TABLE_NAME = "baum_ansprechpartner";
    private static final String FIELD__ID = "id";
    private static final String FIELD__FK = "fk_ansprechpartner";
    private static final String TABLE_NAME_SEARCH_OT = "baum_ortstermin_ansprechpartner";
    private static final String TABLE_NAME_SEARCH_M = "baum_meldung_ansprechpartner";
    private static final String DELETE_TEXT_OT =
        "Dieser Ansprechpartner kann nicht gelöscht werden, da dieser beim Ortstermin verwendet wird.";
    private static final String DELETE_TEXT_M =
        "Dieser Ansprechpartner/Melder kann nicht gelöscht werden, da dieser bei der Meldung verwendet wird.";
    private String deleteText = "Dieser Ansprechpartner kann nicht gelöscht werden, da dieser verwendet wird.";
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

        final CidsBean ansprechpartnerBean = metaObject.getBean();
        final Integer ansprechpartner_id = (Integer)ansprechpartnerBean.getProperty(FIELD__ID);

        final String queryApInOt = String.format(
                "SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH_OT,
                FIELD__FK,
                ansprechpartner_id);
        final String queryApInM = String.format(
                "SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH_M,
                FIELD__FK,
                ansprechpartner_id);
        
        try {
            final ArrayList<ArrayList> artArrayM = getMetaService().performCustomSearch(
                    queryApInM,
                    getConnectionContext());
            if (!artArrayM.isEmpty()) {
                deleteText = DELETE_TEXT_M;
                return true;
            }
            final ArrayList<ArrayList> artArrayOt = getMetaService().performCustomSearch(
                    queryApInOt,
                    getConnectionContext());
            if (!artArrayOt.isEmpty()) {
                deleteText = DELETE_TEXT_OT;
                return true;
            }
        } catch (RemoteException ex) {
            LOG.error("Cannot delete Ap object", ex);
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
