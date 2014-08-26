/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.server.search.CidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = CidsServerSearch.class)
public class MetaObjectNodesBillingSearchStatement extends CidsBillingSearchStatement
        implements MetaObjectNodeServerSearch {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MetaObjectNodesBillingSearchStatement object.
     */
    public MetaObjectNodesBillingSearchStatement() {
        super();
    }

    /**
     * Creates a new MetaObjectNodesBillingSearchStatement object.
     *
     * @param  user             DOCUMENT ME!
     * @param  kundeMetaObject  DOCUMENT ME!
     */
    public MetaObjectNodesBillingSearchStatement(final User user, final MetaObject kundeMetaObject) {
        super(user, kundeMetaObject);
    }

    /**
     * Creates a new MetaObjectNodesBillingSearchStatement object.
     *
     * @param  user              DOCUMENT ME!
     * @param  kundeMetaObjects  DOCUMENT ME!
     */
    public MetaObjectNodesBillingSearchStatement(final User user, final ArrayList<MetaObject> kundeMetaObjects) {
        super(user, kundeMetaObjects);
    }

    /**
     * Creates a new MetaObjectNodesBillingSearchStatement object.
     *
     * @param  user        DOCUMENT ME!
     * @param  kundenname  DOCUMENT ME!
     */
    public MetaObjectNodesBillingSearchStatement(final User user, final String kundenname) {
        super(user, kundenname);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        final Collection<MetaObject> metaObjects = super.performServerSearch();
        final Collection<MetaObjectNode> metaObjectsNodes = new ArrayList<MetaObjectNode>(metaObjects.size());
        for (final MetaObject metaObject : metaObjects) {
            final MetaObjectNode node = new MetaObjectNode(
                    "WUNDA_BLAU",
                    metaObject.getID(),
                    metaObject.getClassID(),
                    metaObject.getName());
            metaObjectsNodes.add(node);
        }
        return metaObjectsNodes;
    }
}
