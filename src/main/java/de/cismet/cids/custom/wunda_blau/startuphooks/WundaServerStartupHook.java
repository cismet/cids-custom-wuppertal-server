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
package de.cismet.cids.custom.wunda_blau.startuphooks;

import Sirius.server.middleware.interfaces.domainserver.DomainServerStartupHook;

import de.cismet.cids.custom.utils.alkis.AlkisConstants;
import de.cismet.cids.custom.utils.alkis.AlkisProducts;
import de.cismet.cids.custom.utils.butler.ButlerProductGenerator;
import de.cismet.cids.custom.utils.nas.NASProductGenerator;
import de.cismet.cids.custom.utils.pointnumberreservation.PointNumberReservationService;
import de.cismet.cids.custom.wunda_blau.search.actions.NasZaehlObjekteSearch;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = DomainServerStartupHook.class)
public class WundaServerStartupHook implements DomainServerStartupHook {

    //~ Methods ----------------------------------------------------------------

    @Override
    public void domainServerStarted() {
        /*
         * this class is used to guarantee that all classes that need to loads resources from the res.jar are
         * initialized during server startup to avoid that these resources are loaded from an eventually changed jar,
         * which will cause an vm crash or Exception
         */

        final AlkisProducts pr = new AlkisProducts(
                AlkisConstants.COMMONS.USER,
                AlkisConstants.COMMONS.PASSWORD,
                AlkisConstants.COMMONS.SERVICE);

        final NasZaehlObjekteSearch search = new NasZaehlObjekteSearch(
                null,
                NasZaehlObjekteSearch.NasSearchType.ADRESSE);

        ButlerProductGenerator.getInstance();
        NASProductGenerator.instance();
        PointNumberReservationService.instance();
    }
}
