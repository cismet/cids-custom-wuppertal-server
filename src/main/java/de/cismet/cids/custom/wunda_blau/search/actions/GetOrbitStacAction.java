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
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import de.cismet.cids.custom.wunda_blau.search.actions.orbit.OrbitStacTools;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

/**
 * DOCUMENT ME!
 *
 * @author   hell
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class GetOrbitStacAction implements ServerAction, UserAwareServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(StamperServerAction.class);

    public static final String TASK_NAME = "getOrbitStac";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        IP, STAC_OPTIONS
    }

    //~ Instance fields --------------------------------------------------------

    private User user;

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... saps) {
        String ipAddress = null;
        String stacOptions = null;

        for (final ServerActionParameter sap : saps) {
            if (sap.getKey().equals(PARAMETER_TYPE.IP.toString())) {
                ipAddress = (String)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.STAC_OPTIONS.toString())) {
                stacOptions = (String)sap.getValue();
            }
        }

        final String stac = OrbitStacTools.getInstance().createStac(user.getName(), ipAddress, stacOptions);
        final String socketChannelId = OrbitStacTools.getInstance().getEntry(stac).getSocketChannelId();
        return "{\"stac\":\"" + stac + "\",\"socketChannelId\":\"" + socketChannelId + "\"}";
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }
}
