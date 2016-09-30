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
package de.cismet.cids.custom.utils.vermessungsunterlagen.tasks;

import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.FileOutputStream;
import java.io.OutputStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import de.cismet.cids.custom.utils.alkis.AlkisPointReportBean;
import de.cismet.cids.custom.utils.alkis.AlkisProducts;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;

import de.cismet.cids.dynamics.CidsBean;

import static de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper.jasperReportDownload;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskAPMap extends VermUntTaskAP {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "APMap";
    private static final String REPORT_APMAPS = "/de/cismet/cids/custom/wunda_blau/res/apmaps.jasper";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskAPMap object.
     *
     * @param  jobkey       DOCUMENT ME!
     * @param  alkisPoints  DOCUMENT ME!
     */
    public VermUntTaskAPMap(final String jobkey, final Collection<CidsBean> alkisPoints) {
        super(TYPE, jobkey, alkisPoints);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void performTask() throws Exception {
        OutputStream out = null;
        try {
            final String filename = getPath() + "/" + AlkisProducts.getInstance().PUNKTLISTE_PDF + ".pdf";
            out = new FileOutputStream(filename);
            jasperReportDownload(
                REPORT_APMAPS,
                new HashMap(),
                new JRBeanCollectionDataSource(Arrays.asList(new AlkisPointReportBean(getAlkisPoints()))),
                out);
        } finally {
            VermessungsunterlagenHelper.closeStream(out);
        }
    }
}
