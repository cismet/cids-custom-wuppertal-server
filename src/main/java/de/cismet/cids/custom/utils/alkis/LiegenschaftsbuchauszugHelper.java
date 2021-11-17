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
package de.cismet.cids.custom.utils.alkis;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.newuser.User;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.cismet.cids.custom.utils.berechtigungspruefung.katasterauszug.BerechtigungspruefungAlkisDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.katasterauszug.BerechtigungspruefungAlkisEinzelnachweisDownloadInfo;
import de.cismet.cids.custom.wunda_blau.search.actions.AlkisProductServerAction;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerActionParameter;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
public class LiegenschaftsbuchauszugHelper {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(LiegenschaftsbuchauszugHelper.class);
    private static final Map<String, MetaClass> METACLASS_CACHE = new HashMap();

    //~ Instance fields --------------------------------------------------------

    private final ConnectionContext connectionContext;
    private final MetaService metaService;
    private final User user;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaulastBescheinigungHelper object.
     *
     * @param  user               DOCUMENT ME!
     * @param  metaService        DOCUMENT ME!
     * @param  connectionContext  DOCUMENT ME!
     */
    public LiegenschaftsbuchauszugHelper(final User user,
            final MetaService metaService,
            final ConnectionContext connectionContext) {
        this.user = user;
        this.metaService = metaService;
        this.connectionContext = connectionContext;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public MetaService getMetaService() {
        return metaService;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public User getUser() {
        return user;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstuecke     DOCUMENT ME!
     * @param   protocolBuffer  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void prepareFlurstuecke(final List<CidsBean> flurstuecke, final ProtocolBuffer protocolBuffer)
            throws Exception {
        protocolBuffer.appendLine("Liegenschaftsbuchauszugs-Protokoll für "
                    + ((flurstuecke.size() == 1) ? "folgendes Flurstück" : "folgende Flurstücke") + ":");

        Collections.sort(flurstuecke, new Comparator<CidsBean>() {

                @Override
                public int compare(final CidsBean o1, final CidsBean o2) {
                    final String s1 = (o1 == null) ? "" : (String)o1.getProperty("alkis_id");
                    final String s2 = (o2 == null) ? "" : (String)o2.getProperty("alkis_id");
                    return s1.compareTo(s2);
                }
            });

        for (final CidsBean flurstueck : flurstuecke) {
            protocolBuffer.appendLine(" * " + flurstueck);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   protocol  DOCUMENT ME!
     * @param   zipOut    DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public void writeProcotol(final String protocol, final ZipOutputStream zipOut) throws IOException {
        writeToZip("liegenschaftsbuch_protokoll.txt", IOUtils.toInputStream(protocol, "UTF-8"), zipOut);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   downloadInfo  DOCUMENT ME!
     * @param   file          transId DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public void writeFullBescheinigung(final BerechtigungspruefungAlkisEinzelnachweisDownloadInfo downloadInfo,
            final File file) throws IOException {
        writeAlkisReports(downloadInfo.getAlkisCodes(),
            downloadInfo.getAuftragsnummer(),
            downloadInfo.getAlkisProdukt(),
            file);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fileName  DOCUMENT ME!
     * @param   in        DOCUMENT ME!
     * @param   zipOut    DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    private void writeToZip(final String fileName, final InputStream in, final ZipOutputStream zipOut)
            throws IOException {
        final byte[] buf = new byte[1024];
        int len;
        zipOut.putNextEntry(new ZipEntry(fileName));
        while ((len = in.read(buf)) > 0) {
            zipOut.write(buf, 0, len);
        }
        zipOut.flush();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   alkisCode  type DOCUMENT ME!
     * @param   jobNumber  DOCUMENT ME!
     * @param   product    selectedBaulasten DOCUMENT ME!
     * @param   file       projectName DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    private void writeAlkisReports(final List<String> alkisCode,
            final String jobNumber,
            final String product,
            final File file) throws IOException {
        if ((alkisCode != null) && (alkisCode.size() == 1)) {
            final ServerActionParameter[] saps = new ServerActionParameter[] {
                    new ServerActionParameter<>(
                        AlkisProductServerAction.Parameter.PRODUKT.toString(),
                        product),
                    new ServerActionParameter<>(
                        AlkisProductServerAction.Parameter.ALKIS_CODE.toString(),
                        AlkisProducts.escapeHtmlSpaces(AlkisProducts.fixBuchungslattCode(alkisCode.get(0)))),
                    new ServerActionParameter<>(
                        AlkisProductServerAction.Parameter.AUFTRAGSNUMMER.toString(),
                        jobNumber)
                };
            final AlkisProductServerAction serverAction = new AlkisProductServerAction();
            serverAction.setMetaService(getMetaService());
            serverAction.setUser(getUser());
            serverAction.initWithConnectionContext(getConnectionContext());

            final Object o = serverAction.execute(AlkisProductServerAction.Body.EINZELNACHWEIS, saps);

            if (o instanceof Exception) {
                throw new IOException((Exception)o);
            }

            try(final FileOutputStream out = new FileOutputStream(file)) {
                out.write((byte[])o);
            } catch (final IOException ex) {
                LOG.fatal(ex, ex);
                throw ex;
            }
        } else {
            try(final ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file))) {
                for (final String code : alkisCode) {
                    final ServerActionParameter[] saps = new ServerActionParameter[] {
                            new ServerActionParameter<>(
                                AlkisProductServerAction.Parameter.PRODUKT.toString(),
                                product),
                            new ServerActionParameter<>(
                                AlkisProductServerAction.Parameter.ALKIS_CODE.toString(),
                                AlkisProducts.escapeHtmlSpaces(AlkisProducts.fixBuchungslattCode(code))),
                            new ServerActionParameter<>(
                                AlkisProductServerAction.Parameter.AUFTRAGSNUMMER.toString(),
                                jobNumber)
                        };

                    final AlkisProductServerAction serverAction = new AlkisProductServerAction();
                    serverAction.setMetaService(getMetaService());
                    serverAction.setUser(getUser());
                    serverAction.initWithConnectionContext(getConnectionContext());

                    final Object o = serverAction.execute(AlkisProductServerAction.Body.EINZELNACHWEIS, saps);

                    if (o instanceof Exception) {
                        throw new IOException((Exception)o);
                    }

                    writeToZip(
                        product
                                + "."
                                + code
                                + ".pdf",
                        new ByteArrayInputStream((byte[])o),
                        zipOut);
                }
            } catch (final IOException ex) {
                LOG.fatal(ex, ex);
                throw ex;
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstuecke  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected List<String> createFlurstuecksListFromBeans(final List<CidsBean> flurstuecke) {
        final List<String> flList = new ArrayList<String>();

        for (final CidsBean bean : flurstuecke) {
            flList.add(bean.toString());
        }

        return flList;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   list  flurstuecke DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected String createStringFromList(final List<String> list) {
        final StringBuilder builder = new StringBuilder();
        boolean firstElement = true;

        for (final String tmp : list) {
            if (firstElement) {
                firstElement = false;
            } else {
                builder.append(",");
            }

            builder.append(tmp);
        }

        return builder.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   buchungsblaetter  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected List<String> createBuchungsblattListFromBeans(final List<CidsBean> buchungsblaetter) {
        final List<String> bbList = new ArrayList<String>();

        for (final CidsBean bean : buchungsblaetter) {
            bbList.add(AlkisProducts.fixBuchungslattCode((String)bean.getProperty("buchungsblattcode")));
        }

        return bbList;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   auftragsnummer      DOCUMENT ME!
     * @param   produktBezeichnung  DOCUMENT ME!
     * @param   fertigungsVermerk   DOCUMENT ME!
     * @param   flurstuecke         DOCUMENT ME!
     * @param   buchungsblaetter    DOCUMENT ME!
     * @param   alkisProduct        DOCUMENT ME!
     * @param   protocolBuffer      DOCUMENT ME!
     * @param   statusHolder        DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public BerechtigungspruefungAlkisEinzelnachweisDownloadInfo calculateDownloadInfo(final String auftragsnummer,
            final String produktBezeichnung,
            final String fertigungsVermerk,
            final List<CidsBean> flurstuecke,
            final List<CidsBean> buchungsblaetter,
            final String alkisProduct,
            final LiegenschaftsbuchauszugHelper.ProtocolBuffer protocolBuffer,
            final LiegenschaftsbuchauszugHelper.StatusHolder statusHolder) throws Exception {
        statusHolder.setMessage("Bescheinigung wird vorbereitet...");
        prepareFlurstuecke(flurstuecke, protocolBuffer);

        statusHolder.setMessage("Gebühr wird berechnet...");

        Integer prodAmounts;
        BerechtigungspruefungAlkisDownloadInfo.AlkisObjektTyp alkisObjectTyp;
        List<String> alkisCodes;
        String productType;
        final ServerAlkisProducts alkisProducts = ServerAlkisProducts.getInstance();

        statusHolder.setMessage("Benötigte Daten identifizieren...");
        if (alkisProduct.startsWith("fsueKom")) {
            productType = alkisProducts.get(AlkisProducts.Type.FLURSTUECKS_UND_EIGENTUMSNACHWEIS_KOMMUNAL_PDF);
            prodAmounts = flurstuecke.size();
            alkisObjectTyp = BerechtigungspruefungAlkisDownloadInfo.AlkisObjektTyp.FLURSTUECKE;
            alkisCodes = createFlurstuecksListFromBeans(flurstuecke);
        } else {
            productType = alkisProducts.get(AlkisProducts.Type.BESTANDSNACHWEIS_KOMMUNAL_PDF);
            prodAmounts = buchungsblaetter.size();
            alkisObjectTyp = BerechtigungspruefungAlkisDownloadInfo.AlkisObjektTyp.BUCHUNGSBLAETTER;
            alkisCodes = createBuchungsblattListFromBeans(buchungsblaetter);
        }

        return new BerechtigungspruefungAlkisEinzelnachweisDownloadInfo(
                BerechtigungspruefungAlkisDownloadInfo.PRODUKT_TYP,
                auftragsnummer,
                produktBezeichnung,
                null,
                alkisObjectTyp,
                BerechtigungspruefungAlkisDownloadInfo.AlkisDownloadTyp.EINZELNACHWEIS,
                alkisCodes,
                productType, // alkisProduct
                null,
                prodAmounts);
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class StatusHolder {

        //~ Instance fields ----------------------------------------------------

        private String message;

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @param  message  DOCUMENT ME!
         */
        public void setMessage(final String message) {
            this.message = message;
            LOG.info(message);
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getMessage() {
            return message;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class ProtocolBuffer {

        //~ Instance fields ----------------------------------------------------

        private final StringBuffer buffer = new StringBuffer();

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @param   string  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public LiegenschaftsbuchauszugHelper.ProtocolBuffer appendLine(final String string) {
            buffer.append(string).append("\n");
            return this;
        }

        @Override
        public String toString() {
            return buffer.toString();
        }
    }
}
