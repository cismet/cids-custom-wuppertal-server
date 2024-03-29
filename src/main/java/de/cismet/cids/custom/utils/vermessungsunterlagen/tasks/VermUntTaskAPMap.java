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

import java.io.FileOutputStream;
import java.io.OutputStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.alkis.AlkisPointReportBean;
import de.cismet.cids.custom.utils.alkis.AlkisProducts;
import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenUtils;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenTaskException;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskAPMap extends VermUntTaskAP {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "AP_Karten";

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
    public void performTask() throws VermessungsunterlagenTaskException {
        try {
            final String filename = getPath() + "/"
                        + ServerAlkisProducts.getInstance().get(AlkisProducts.Type.PUNKTLISTE_PDF) + ".pdf";
            final Map parameters = new HashMap();
            parameters.put("SUBREPORT_DIR", DomainServerImpl.getServerProperties().getServerResourcesBasePath() + "/");

            try(final OutputStream out = new FileOutputStream(filename)) {
                VermessungsunterlagenUtils.jasperReportDownload(
                    ServerResourcesLoader.getInstance().loadJasperReport(
                        WundaBlauServerResources.APMAPS_JASPER.getValue()),
                    parameters,
                    new JRBeanCollectionDataSource(Arrays.asList(new AlkisPointReportBean(getAlkisPoints()))),
                    out);
            }
        } catch (final Exception ex) {
            final String message = "Beim Erstellen des Punktlisten-Berichtes kam es zu einem unerwarteten Fehler.";
            throw new VermessungsunterlagenTaskException(getType(), message, ex);
        }
    }
}
