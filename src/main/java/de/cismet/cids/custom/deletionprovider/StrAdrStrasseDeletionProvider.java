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

import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

import de.cismet.cids.dynamics.CidsBean;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CustomDeletionProvider.class)
public class StrAdrStrasseDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(StrAdrStrasseDeletionProvider.class);
    public static final String TABLE_NAME = "str_adr_strasse";
    public static final String FIELD__KEY = "schluessel.name";
    private static final String AMTL_STR_GRENZE = "04000";
    private static final String DELETE_KLEINER =
        "Diese Straße darf nicht gelöscht werden, sie muss historisiert werden.";

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

        final CidsBean strBean = metaObject.getBean();
        return (strBean.getProperty(FIELD__KEY).toString().compareTo(AMTL_STR_GRENZE) <= 0);
    }

    @Override
    public boolean customDeleteMetaObject(final User user, final MetaObject metaObject) throws Exception {
        if (metaObject != null) {
            final CidsBean strBean = metaObject.getBean();
            final String strasse = strBean.getProperty(FIELD__KEY).toString();

            // finde amtliche (historische) Straßen
            if (strasse.compareTo(AMTL_STR_GRENZE) < 0) {
                throw new DeletionProviderClientException(
                    DELETE_KLEINER);
            }
        }
        return false;
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}
