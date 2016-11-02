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
package de.cismet.cids.custom.nodepermissions.wunda_blau;

import Sirius.server.newuser.User;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import de.cismet.cids.nodepermissions.AbstractCustomNodePermissionProvider;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
public class DisabledAdresseNodePermissionProvider extends AbstractCustomNodePermissionProvider {

    //~ Static fields/initializers ---------------------------------------------

    static Geometry tester = null;
    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            DisabledAdresseNodePermissionProvider.class);

    static {
        final GeometryFactory f = new GeometryFactory();
        final Geometry home = f.createPoint(new Coordinate(374476.41, 5681643.73));
        home.setSRID(25832);
        tester = home.buffer(100.0);
        if (LOG.isDebugEnabled()) {
            LOG.debug("cs_cache AdresseNodePermissionProvider: tester angelegt");
        }
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public boolean getCustomReadPermissionDecisionforUser(final User u) {
        if ((tester == null) || (getObjectNode() == null)
                    || (getObjectNode().getCashedGeometry() == null)) {
            return false;
        } else {
            return tester.contains(getObjectNode().getCashedGeometry());
        }
    }
}
