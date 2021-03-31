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

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CustomDeletionProvider.class)
public class FsBestellungDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(FsBestellungDeletionProvider.class);
    public static final String TABLE_NAME = "fs_bestellung";

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public boolean customDeleteMetaObject(final User user, final MetaObject metaObject) throws Exception {
        throw new DeletionProviderClientException("Das Löschen von Produktbestellungen ist nicht erlaubt.");
    }
}
