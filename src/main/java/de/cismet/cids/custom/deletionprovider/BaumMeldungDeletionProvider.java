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
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.cismet.cids.custom.wunda_blau.search.server.BaumChildLightweightSearch;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.SearchException;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CustomDeletionProvider.class)
public class BaumMeldungDeletionProvider extends AbstractCustomDeletionProvider implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumMeldungDeletionProvider.class);
    private static final String TABLE_NAME = "baum_meldung";
    private static final String FIELD__ID = "id";
    private static final String FIELD__FK = "fk_meldung";
    private static final String TABLE_NAME_SEARCH_S = "baum_schaden";
    private static final String TABLE_NAME_SEARCH_O = "baum_ortstermin";
    private static final String DELETE_TEXT_DEFAULT =
        "Dieses Meldung kann nicht gelöscht werden, da diese mindestens ein Unterobjekt hat.";
    private static final String DELETE_TEXT_SCHADEN =
        "Diese Meldung kann nicht gelöscht werden, da diese mindestens einen Schaden hat.";
    private static final String DELETE_TEXT_ORT =
        "Diese Meldung kann nicht gelöscht werden, da diese mindestens einen Ortstermin hat.";

    //~ Instance fields --------------------------------------------------------

    private String deleteText;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }
    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public boolean isMatching(final User user, final MetaObject metaObject) {
        if (!super.isMatching(user, metaObject)) {
            return false;
        }
        final CidsBean meldungBean = metaObject.getBean();
        final Integer meldung_id = (Integer)meldungBean.getProperty(FIELD__ID);

        deleteText = DELETE_TEXT_DEFAULT;
        if (checkChildObject(FIELD__FK, meldung_id, TABLE_NAME_SEARCH_O, user)) {
            deleteText = DELETE_TEXT_ORT;
            return true;
        }
        if (checkChildObject(FIELD__FK, meldung_id, TABLE_NAME_SEARCH_S, user)) {
            deleteText = DELETE_TEXT_SCHADEN;
            return true;
        }
        return false;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fkField      DOCUMENT ME!
     * @param   parentId     DOCUMENT ME!
     * @param   searchTable  DOCUMENT ME!
     * @param   user         DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean checkChildObject(
            final String fkField,
            final int parentId,
            final String searchTable,
            final User user) {
        final String[] childFields = { "id" };

        final BaumChildLightweightSearch search = new BaumChildLightweightSearch();
        final Map localServers = new HashMap<>();
        localServers.put("WUNDA_BLAU", getMetaService());
        search.setActiveLocalServers(localServers);
        search.setUser(user);
        search.initWithConnectionContext(connectionContext);
        search.setFkField(fkField);
        search.setParentId(parentId);
        search.setTable(searchTable);
        search.setRepresentationFields(childFields);
        try {
            final Collection<MetaObjectNode> mons = search.performServerSearch();
            if (!mons.isEmpty()) {
                return true; // Kinder vorhanden
            }
        } catch (SearchException ex) {
            LOG.error("Cannot delete Meldung object", ex);
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
