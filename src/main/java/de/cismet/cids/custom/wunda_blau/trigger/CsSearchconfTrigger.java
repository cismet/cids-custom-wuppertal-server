/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.wunda_blau.trigger;

import Sirius.server.newuser.User;

import org.openide.util.lookup.ServiceProvider;

import de.cismet.cids.custom.wunda_blau.search.server.StorableSearch;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.trigger.AbstractDBAwareCidsTrigger;
import de.cismet.cids.trigger.CidsTrigger;
import de.cismet.cids.trigger.CidsTriggerKey;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CidsTrigger.class)
public class CsSearchconfTrigger extends AbstractDBAwareCidsTrigger {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            CsSearchconfTrigger.class);
    private static final String TABLE_NAME = "cs_searchconf";

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   cidsBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isForMe(final CidsBean cidsBean) {
        return ((cidsBean != null)
                        && (cidsBean.getMetaObject() != null)
                        && (cidsBean.getMetaObject().getMetaClass() != null)
                        && TABLE_NAME.equalsIgnoreCase(cidsBean.getMetaObject().getMetaClass().getTableName()));
    }

    /**
     * DOCUMENT ME!
     *
     * @param  cidsBean  DOCUMENT ME!
     */
    private void storeQuery(final CidsBean cidsBean) {
        if (isForMe(cidsBean)) {
            try {
                final String confJson = (String)cidsBean.getProperty("conf_json");
                final String searchName = (String)cidsBean.getProperty("search_name");
                final String searchQuery;
                if (searchName != null) {
                    final StorableSearch search = CsSearchconfHandler.getInstance().getStorableSearches(searchName);
                    search.setConfiguration(confJson);
                    searchQuery = search.createQuery();
                } else {
                    searchQuery = null;
                }
                cidsBean.setProperty("children_query", searchQuery);
            } catch (final Exception ex) {
                LOG.error(ex, ex);
            }
        }
    }

    @Override
    public void beforeInsert(final CidsBean cidsBean, final User user) {
        storeQuery(cidsBean);
    }

    @Override
    public void afterInsert(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void beforeUpdate(final CidsBean cidsBean, final User user) {
        storeQuery(cidsBean);
    }

    @Override
    public void afterUpdate(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void beforeDelete(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void afterDelete(final CidsBean cb, final User user) {
    }

    @Override
    public void afterCommittedInsert(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void afterCommittedUpdate(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void afterCommittedDelete(final CidsBean cidsBean, final User user) {
    }

    @Override
    public CidsTriggerKey getTriggerKey() {
        return new CidsTriggerKey(CidsTriggerKey.ALL, TABLE_NAME);
    }

    @Override
    public int compareTo(final CidsTrigger cidsTrigger) {
        return 0;
    }
}
