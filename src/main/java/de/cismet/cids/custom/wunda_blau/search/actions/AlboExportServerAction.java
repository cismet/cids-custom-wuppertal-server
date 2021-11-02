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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import java.io.File;

import de.cismet.cids.custom.utils.AlboProperties;

import de.cismet.cids.server.actions.AbstractPostgresToShapefileServerAction;
import de.cismet.cids.server.actions.ServerAction;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class AlboExportServerAction extends AbstractPostgresToShapefileServerAction {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "alboExport";
    private static final String SELECT_QUERY = "SELECT * FROM %s ORDER BY %s;";

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getRowId() {
        return AlboProperties.getInstance().getExportRowidField();
    }

    @Override
    public File getTmpDir() {
        return new File(AlboProperties.getInstance().getExportTmpAbsPath());
    }

    @Override
    public String getQuery() {
        return String.format(
                SELECT_QUERY,
                AlboProperties.getInstance().getExportViewName(),
                AlboProperties.getInstance().getExportOrderbyField());
    }

    @Override
    public String getWrtProjection() {
        return AlboProperties.getInstance().getWrtProjection();
    }

    @Override
    public Class<? extends Geometry> getGeometryClass(final String columnName) {
        return Polygon.class;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
