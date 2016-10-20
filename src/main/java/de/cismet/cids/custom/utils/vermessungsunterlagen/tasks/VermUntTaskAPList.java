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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;

import java.util.Collection;

import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;

import de.cismet.cids.dynamics.CidsBean;

import static de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper.doGetRequest;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskAPList extends VermUntTaskAP {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "AP_List";
    private static final ServerAlkisProducts PRODUCTS = ServerAlkisProducts.getInstance();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskAPList object.
     *
     * @param  jobkey       DOCUMENT ME!
     * @param  alkisPoints  DOCUMENT ME!
     */
    public VermUntTaskAPList(final String jobkey, final Collection<CidsBean> alkisPoints) {
        super(TYPE, jobkey, alkisPoints);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void performTask() throws Exception {
        final String punktListenString = getPunktlistenStringForChosenPoints(getAlkisPoints());
        final String code = PRODUCTS.PUNKTLISTE_TXT;
        final String filename = getPath() + "/" + ServerAlkisProducts.getInstance().PUNKTLISTE_TXT + ".plst";
        final File fileToSaveTo = new File(filename);

        if (punktListenString.length() > 3) {
            if ((code != null) && (code.length() > 0)) {
                final String url = PRODUCTS.productListenNachweisUrl(punktListenString, code);
                if ((url != null) && (url.trim().length() > 0)) {
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = doGetRequest(new URL(url));
                        out = new FileOutputStream(fileToSaveTo);
                        VermessungsunterlagenHelper.downloadStream(in, out);
                    } finally {
                        VermessungsunterlagenHelper.closeStream(in);
                        VermessungsunterlagenHelper.closeStream(out);
                    }
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   alkisPoints  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String getPunktlistenStringForChosenPoints(final Collection<CidsBean> alkisPoints) {
        final StringBuffer punktListeString = new StringBuffer();

        for (final CidsBean alkisPoint : alkisPoints) {
            if (punktListeString.length() > 0) {
                punktListeString.append(",");
            }
            punktListeString.append(PRODUCTS.getPointDataForProduct(alkisPoint));
        }

        return punktListeString.toString();
    }
}
