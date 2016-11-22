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

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;

import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import java.net.URL;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.alkis.AlkisPointReportBean;
import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskNivPBeschreibungen extends VermUntTaskNivP {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "NivP_Beschreibungen";
    private static final ServerAlkisProducts PRODUCTS = ServerAlkisProducts.getInstance();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskAPList object.
     *
     * @param  jobkey     DOCUMENT ME!
     * @param  nivPoints  DOCUMENT ME!
     */
    public VermUntTaskNivPBeschreibungen(final String jobkey, final Collection<CidsBean> nivPoints) {
        super(TYPE, jobkey, nivPoints);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void performTask() throws Exception {
        OutputStream out = null;
        try {
            final String filename = getPath() + "/" + ServerAlkisProducts.getInstance().PUNKTLISTE_PDF + ".pdf";
            out = new FileOutputStream(filename);
            final Map parameters = new HashMap();
            parameters.put("SUBREPORT_DIR", DomainServerImpl.getServerProperties().getServerResourcesBasePath() + "/");

            VermessungsunterlagenHelper.jasperReportDownload(
                ServerResourcesLoader.getInstance().loadJasperReport(
                    WundaBlauServerResources.APMAPS_JASPER.getValue()),
                parameters,
                new JRBeanCollectionDataSource(Arrays.asList(new AlkisPointReportBean(getNivPoints()))),
                out);
        } finally {
            VermessungsunterlagenHelper.closeStream(out);
        }
    }
}
