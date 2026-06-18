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

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.trigger.AbstractDBAwareCidsTrigger;
import de.cismet.cids.trigger.CidsTrigger;
import de.cismet.cids.trigger.CidsTriggerKey;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CidsTrigger.class)
public class GpEntdeckenTrigger extends AbstractDBAwareCidsTrigger {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            GpEntdeckenTrigger.class);
    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String TABLE_GP_ENTDECKEN = "gp_entdecken";

    //~ Methods ----------------------------------------------------------------

    @Override
    public void beforeInsert(final CidsBean cb, final User user) {
        try {
            cb.setProperty("created_by", user.getName());
        } catch (Exception e) {
            LOG.error("Cannot set created_by in GpEntdeckenTrigger", e);
        }
    }

    @Override
    public void afterInsert(final CidsBean cb, final User user) {
    }

    @Override
    public void beforeUpdate(final CidsBean cb, final User user) {
        try {
            cb.setProperty("created_by", user.getName());
        } catch (Exception e) {
            LOG.error("Cannot set created_by in GpEntdeckenTrigger", e);
        }
    }

    @Override
    public void afterUpdate(final CidsBean cb, final User user) {
    }

    @Override
    public void beforeDelete(final CidsBean cb, final User user) {
    }

    @Override
    public void afterDelete(final CidsBean cb, final User user) {
    }

    @Override
    public void afterCommittedInsert(final CidsBean cb, final User user) {
    }

    @Override
    public void afterCommittedUpdate(final CidsBean cb, final User user) {
    }

    @Override
    public void afterCommittedDelete(final CidsBean cb, final User user) {
    }

    @Override
    public CidsTriggerKey getTriggerKey() {
        return new CidsTriggerKey(DOMAIN, TABLE_GP_ENTDECKEN);
    }

    @Override
    public int compareTo(final CidsTrigger o) {
        return 0;
    }
}
