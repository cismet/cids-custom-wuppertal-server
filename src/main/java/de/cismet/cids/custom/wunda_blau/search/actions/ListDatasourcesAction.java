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

import org.apache.log4j.Logger;

import de.cismet.cids.custom.utils.DatasourceExtractor;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class ListDatasourcesAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(ListDatasourcesAction.class);

    public static final String TASK_NAME = "listDatasources";

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... saps) {
        try {
            DatasourceExtractor.extractDatasources();
            return true;
        } catch (Exception e) {
            LOG.error("Error while extracting the data sources");
            return false;
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
