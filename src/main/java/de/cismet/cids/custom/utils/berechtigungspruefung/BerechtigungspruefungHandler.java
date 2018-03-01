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
package de.cismet.cids.custom.utils.berechtigungspruefung;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.newuser.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import java.sql.Timestamp;

import java.text.NumberFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.katasterauszug.BerechtigungspruefungAlkisDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.katasterauszug.BerechtigungspruefungAlkisEinzelnachweisDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.katasterauszug.BerechtigungspruefungAlkisKarteDownloadInfo;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.connectioncontext.ServerConnectionContext;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;
import de.cismet.cids.server.messages.CidsServerMessageManagerImpl;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class BerechtigungspruefungHandler implements ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static BerechtigungspruefungHandler INSTANCE;
    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungHandler.class);

    //~ Instance fields --------------------------------------------------------

    private MetaService metaService;

    private ServerConnectionContext serverConnectionContext = ServerConnectionContext.create(getClass()
                    .getSimpleName());

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BerechtigungspruefungHandler object.
     */
    private BerechtigungspruefungHandler() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BerechtigungspruefungHandler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BerechtigungspruefungHandler();
        }
        return INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  user  DOCUMENT ME!
     */
    public void deleteOldDateianhangFiles(final User user) {
        final File directory = new File(BerechtigungspruefungProperties.getInstance().getAnhangAbsPath());
        final Date thresholdDate = getThresholdAnhangDate();

        // look for all anhang files
        for (final File file : directory.listFiles()) {
            if (file.isFile()) {
                try {
                    final String fileName = file.getName();
                    final BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                    final Date creationDate = new Date(attr.creationTime().toMillis());

                    // file older then threshold date (1 month) ?
                    if (creationDate.before(thresholdDate)) {
                        final CidsBean anfrageBean = loadAnfrageBean(user, fileName);
                        // assuring, that the file corresponds to an existing bean This prevents accidental deletion of
                        // non-anhang files (i.e. if AnhangAbsPath was set to a path that contains also other files)
                        if (anfrageBean != null) {
                            final Timestamp anfrageTs = (Timestamp)anfrageBean.getProperty("anfrage_timestamp");
                            // timestamp filed in the bean agrees with the file creation date ?
                            if (anfrageTs.before(thresholdDate)) {
                                LOG.info("deleting old Anhang file: " + file.getName() + " (date: "
                                            + creationDate.toString() + ")");
                                // now we can delete (hopefully)
                                file.delete();
                            }
                        }
                    }
                } catch (final IOException ex) {
                    LOG.warn("could not delete Anhang file: " + file.getName(), ex);
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  user  DOCUMENT ME!
     */
    public void sendMessagesForAllOpenAnfragen(final User user) {
        final Collection<CidsBean> allOpenPruefungen = loadOpenAnfrageBeans(user);
        sendAnfrageMessages(allOpenPruefungen);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey  DOCUMENT ME!
     * @param  user     DOCUMENT ME!
     */
    public void sendMessagesForAllOpenFreigaben(final String userKey, final User user) {
        final Collection<CidsBean> allOpenDownloads = loadOpenFreigabeBeans(
                userKey,
                user);
        sendFreigabeMessage(userKey, allOpenDownloads);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  schluessel  DOCUMENT ME!
     * @param  user        DOCUMENT ME!
     */
    public void sendProcessingMessage(final String schluessel, final User user) {
        CidsServerMessageManagerImpl.getInstance()
                .publishMessage(BerechtigungspruefungProperties.getInstance().getCsmBearbeitung(), schluessel, true);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  user  DOCUMENT ME!
     */
    public void sendMessagesForAllOpenFreigaben(final User user) {
        final Collection<CidsBean> allOpenDownloads = loadOpenFreigabeBeans(user);
        if (allOpenDownloads != null) {
            final Map<String, Collection> dm = new HashMap<String, Collection>();
            for (final CidsBean openDownload : allOpenDownloads) {
                final String userKey = (String)openDownload.getProperty("benutzer");
                if (userKey != null) {
                    if (!dm.containsKey(userKey)) {
                        dm.put(userKey, new ArrayList<CidsBean>());
                    }
                    dm.get(userKey).add(openDownload);
                }
            }
            for (final String userKey : dm.keySet()) {
                sendFreigabeMessage(userKey, dm.get(userKey));
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  metaService  DOCUMENT ME!
     */
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  anfrageBeans  schluessel openPruefung DOCUMENT ME!
     */
    private void sendAnfrageMessages(final Collection<CidsBean> anfrageBeans) {
        final Collection<String> schluesselList = new ArrayList<String>();
        if (anfrageBeans != null) {
            for (final CidsBean anfrageBean : anfrageBeans) {
                schluesselList.add((String)anfrageBean.getProperty("schluessel"));
            }
        }

        CidsServerMessageManagerImpl.getInstance()
                .publishMessage(BerechtigungspruefungProperties.getInstance().getCsmAnfrage(), schluesselList, true);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey        DOCUMENT ME!
     * @param  pruefungBeans  DOCUMENT ME!
     */
    public void sendFreigabeMessage(final String userKey, final Collection<CidsBean> pruefungBeans) {
        if (pruefungBeans != null) {
            try {
                final List<String> schluesselList = new ArrayList<String>();

                for (final CidsBean pruefungBean : pruefungBeans) {
                    schluesselList.add((String)pruefungBean.getProperty("schluessel"));
                }

                if (!schluesselList.isEmpty()) {
                    // an den Pruefer
                    CidsServerMessageManagerImpl.getInstance()
                            .publishMessage(BerechtigungspruefungProperties.getInstance().getCsmBearbeitung(),
                                schluesselList,
                                true);
                    // an den Anfragenden
                    CidsServerMessageManagerImpl.getInstance()
                            .publishMessage(BerechtigungspruefungProperties.getInstance().getCsmFreigabe(),
                                schluesselList,
                                false,
                                new HashSet(Arrays.asList(userKey)),
                                true);
                }
            } catch (final Exception ex) {
                LOG.error("error while producing or sending message", ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user        DOCUMENT ME!
     * @param   schluessel  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void closeAnfrage(final User user, final String schluessel) throws Exception {
        final CidsBean pruefungBean = loadAnfrageBean(user, schluessel);
        pruefungBean.setProperty("abholung_timestamp", new Timestamp(new Date().getTime()));
        pruefungBean.setProperty("abgeholt", true);

        DomainServerImpl.getServerInstance()
                .updateMetaObject(user, pruefungBean.getMetaObject(), getServerConnectionContext());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user          DOCUMENT ME!
     * @param   downloadInfo  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String createNewSchluessel(final User user, final BerechtigungspruefungDownloadInfo downloadInfo) {
        synchronized (this) {
            final String type;
            if (BerechtigungspruefungBescheinigungDownloadInfo.PRODUKT_TYP.equals(downloadInfo.getProduktTyp())) {
                type = "BlaB";
            } else if (BerechtigungspruefungAlkisDownloadInfo.PRODUKT_TYP.equals(downloadInfo.getProduktTyp())) {
                type = "LB";
            } else {
                type = "?";
            }

            final int year = Calendar.getInstance().get(Calendar.YEAR);
            final CidsBean lastAnfrageBean = loadLastAnfrageBeanByTypeAndYear(user, type, year);
            final String lastAnfrageSchluessel = (lastAnfrageBean != null)
                ? (String)lastAnfrageBean.getProperty("schluessel") : null;
            final int lastNumber;
            if (lastAnfrageSchluessel != null) {
                final Pattern pattern = Pattern.compile("^" + type + "-" + year + "-(\\d{5})$");
                final Matcher matcher = pattern.matcher(lastAnfrageSchluessel);
                if (matcher.matches()) {
                    final String group = matcher.group(1);
                    lastNumber = (group != null) ? Integer.parseInt(group) : 0;
                } else {
                    lastNumber = 0;
                }
            } else {
                lastNumber = 0;
            }

            final int newNumber = lastNumber + 1;

            final NumberFormat format = NumberFormat.getIntegerInstance();
            format.setMinimumIntegerDigits(5);
            format.setGroupingUsed(false);

            final String newAnfrageSchluessel = type + "-" + Integer.toString(year) + "-" + format.format(newNumber);
            return newAnfrageSchluessel;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user                DOCUMENT ME!
     * @param   schluessel          DOCUMENT ME!
     * @param   downloadInfo        produktbezeichnung DOCUMENT ME!
     * @param   berechtigungsgrund  DOCUMENT ME!
     * @param   begruendung         DOCUMENT ME!
     * @param   dateiname           DOCUMENT ME!
     * @param   data                DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void addNewAnfrage(final User user,
            final String schluessel,
            final BerechtigungspruefungDownloadInfo downloadInfo,
            final String berechtigungsgrund,
            final String begruendung,
            final String dateiname,
            final byte[] data) throws Exception {
        final String userKey = (String)user.getKey();

        if ((data != null) && (dateiname != null)) {
            final File file = new File(BerechtigungspruefungProperties.getInstance().getAnhangAbsPath() + "/"
                            + schluessel);
            try {
                FileUtils.writeByteArrayToFile(file, data);
            } catch (final IOException ex) {
                throw new Exception("Datei konnte nicht geschrieben werden.", ex);
            }
        }

        final CidsBean newPruefungBean = CidsBean.createNewCidsBeanFromTableName(
                "WUNDA_BLAU",
                "berechtigungspruefung");
        newPruefungBean.setProperty("dateiname", dateiname);
        newPruefungBean.setProperty("schluessel", schluessel);
        newPruefungBean.setProperty("anfrage_timestamp", new Timestamp(new Date().getTime()));
        newPruefungBean.setProperty("berechtigungsgrund", berechtigungsgrund);
        newPruefungBean.setProperty("begruendung", begruendung);
        newPruefungBean.setProperty("benutzer", userKey);
        newPruefungBean.setProperty("abgeholt", false);
        newPruefungBean.setProperty("pruefstatus", null);
        newPruefungBean.setProperty("produkttyp", downloadInfo.getProduktTyp());
        newPruefungBean.setProperty("downloadinfo_json", new ObjectMapper().writeValueAsString(downloadInfo));

        DomainServerImpl.getServerInstance()
                .insertMetaObject(user, newPruefungBean.getMetaObject(), getServerConnectionContext());

        sendAnfrageMessages(Arrays.asList(newPruefungBean));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<CidsBean> loadOpenAnfrageBeans(final User user) {
        final MetaObject[] mos;
        try {
            final List<CidsBean> beans = new ArrayList<CidsBean>();
            final MetaClass mcBerechtigungspruefung = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "berechtigungspruefung");

            final String pruefungQuery = "SELECT DISTINCT " + mcBerechtigungspruefung.getID() + ", "
                        + mcBerechtigungspruefung.getTableName() + "." + mcBerechtigungspruefung.getPrimaryKey() + " "
                        + "FROM " + mcBerechtigungspruefung.getTableName() + " "
                        + "WHERE " + mcBerechtigungspruefung.getTableName() + ".pruefstatus IS NULL;";

            mos = metaService.getMetaObject(user, pruefungQuery, getServerConnectionContext());
            if ((mos != null) && (mos.length > 0)) {
                for (final MetaObject mo : mos) {
                    beans.add(mo.getBean());
                }
            }
            return beans;
        } catch (final Exception ex) {
            LOG.error("error while loading openPruefung beans", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   userKey  DOCUMENT ME!
     * @param   user     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<CidsBean> loadOpenFreigabeBeans(final String userKey, final User user) {
        final MetaObject[] mos;
        try {
            final List<CidsBean> beans = new ArrayList<CidsBean>();
            final MetaClass mcBerechtigungspruefung = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "berechtigungspruefung");

            final String pruefungQuery = "SELECT DISTINCT " + mcBerechtigungspruefung.getID() + ", "
                        + mcBerechtigungspruefung.getTableName() + "." + mcBerechtigungspruefung.getPrimaryKey() + " "
                        + "FROM " + mcBerechtigungspruefung.getTableName() + " "
                        + "WHERE benutzer ILIKE '" + userKey + "' "
                        + "AND " + mcBerechtigungspruefung.getTableName() + ".pruefstatus IS NOT NULL "
                        + "AND (" + mcBerechtigungspruefung.getTableName() + ".abgeholt IS NULL "
                        + "OR " + mcBerechtigungspruefung.getTableName() + ".abgeholt IS FALSE);";

            mos = metaService.getMetaObject(user, pruefungQuery, getServerConnectionContext());
            if ((mos != null) && (mos.length > 0)) {
                for (final MetaObject mo : mos) {
                    beans.add(mo.getBean());
                }
            }
            return beans;
        } catch (final Exception ex) {
            LOG.error("error while loading openDownload beans", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<CidsBean> loadOpenFreigabeBeans(final User user) {
        final MetaObject[] mos;
        try {
            final List<CidsBean> beans = new ArrayList<CidsBean>();
            final MetaClass mcBerechtigungspruefung = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "berechtigungspruefung");

            final String pruefungQuery = "SELECT DISTINCT " + mcBerechtigungspruefung.getID() + ", "
                        + mcBerechtigungspruefung.getTableName() + "." + mcBerechtigungspruefung.getPrimaryKey() + " "
                        + "FROM " + mcBerechtigungspruefung.getTableName() + " "
                        + "WHERE " + mcBerechtigungspruefung.getTableName() + ".pruefstatus IS NOT NULL "
                        + "AND (" + mcBerechtigungspruefung.getTableName() + ".abgeholt IS NULL "
                        + "OR " + mcBerechtigungspruefung.getTableName() + ".abgeholt IS FALSE);";

            mos = metaService.getMetaObject(user, pruefungQuery, getServerConnectionContext());
            if ((mos != null) && (mos.length > 0)) {
                for (final MetaObject mo : mos) {
                    beans.add(mo.getBean());
                }
            }
            return beans;
        } catch (final Exception ex) {
            LOG.error("error while loading openDownload beans", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user        DOCUMENT ME!
     * @param   schluessel  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public CidsBean loadAnfrageBean(final User user, final String schluessel) {
        try {
            final MetaClass mcBerechtigungspruefung = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "berechtigungspruefung");

            final String pruefungQuery = "SELECT DISTINCT " + mcBerechtigungspruefung.getID() + ", "
                        + mcBerechtigungspruefung.getTableName() + "." + mcBerechtigungspruefung.getPrimaryKey() + " "
                        + "FROM " + mcBerechtigungspruefung.getTableName() + " "
                        + "WHERE " + mcBerechtigungspruefung.getTableName() + ".schluessel LIKE '" + schluessel + "' "
                        + "LIMIT 1;";

            final MetaObject[] mos = metaService.getMetaObject(user, pruefungQuery, getServerConnectionContext());
            if ((mos != null) && (mos.length > 0)) {
                return mos[0].getBean();
            }
        } catch (final Exception ex) {
            LOG.error("error while loading pruefung bean", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     * @param   id    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public CidsBean loadBillingBean(final User user, final Integer id) {
        try {
            final MetaClass mcBillingBilling = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "billing_billing");

            final MetaObject mo = metaService.getMetaObject(
                    user,
                    id,
                    mcBillingBilling.getID(),
                    getServerConnectionContext());
            return mo.getBean();
        } catch (final Exception ex) {
            LOG.error("error while loading billing_billing bean", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public CidsBean loadBillingStornogrundBean(final User user) {
        try {
            final MetaClass mcBillingStornogrund = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "billing_stornogrund");

            final MetaObject mo = metaService.getMetaObject(
                    user,
                    BerechtigungspruefungProperties.getInstance().getBillingStornogrundId(),
                    mcBillingStornogrund.getID(),
                    getServerConnectionContext());
            return mo.getBean();
        } catch (final Exception ex) {
            LOG.error("error while loading billing_billing bean", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     * @param   type  DOCUMENT ME!
     * @param   year  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public CidsBean loadLastAnfrageBeanByTypeAndYear(final User user, final String type, final int year) {
        try {
            final MetaClass mcBerechtigungspruefung = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "berechtigungspruefung");

            final String tester = "^" + type + "-" + Integer.toString(year) + "-\\\\d{5}$";
            final String pruefungQuery = "SELECT " + mcBerechtigungspruefung.getID() + ", "
                        + mcBerechtigungspruefung.getTableName() + "." + mcBerechtigungspruefung.getPrimaryKey() + " "
                        + "FROM " + mcBerechtigungspruefung.getTableName() + " "
                        + "WHERE " + mcBerechtigungspruefung.getTableName() + ".schluessel ~ E'" + tester + "' "
                        + "ORDER BY schluessel DESC "
                        + "LIMIT 1;";

            final MetaObject[] mos = metaService.getMetaObject(user, pruefungQuery, getServerConnectionContext());
            if ((mos != null) && (mos.length > 0)) {
                return mos[0].getBean();
            }
        } catch (final Exception ex) {
            LOG.error("error while loading pruefung bean", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   downloadinfoJson  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static BerechtigungspruefungDownloadInfo extractDownloadInfo(final String downloadinfoJson)
            throws Exception {
        final BerechtigungspruefungDownloadInfo berechtigungspruefungDownloadInfo = (BerechtigungspruefungDownloadInfo)
            MAPPER.readValue(downloadinfoJson, BerechtigungspruefungDownloadInfo.class);

        if (BerechtigungspruefungBescheinigungDownloadInfo.PRODUKT_TYP.equals(
                        berechtigungspruefungDownloadInfo.getProduktTyp())) {
            return MAPPER.readValue(downloadinfoJson, BerechtigungspruefungBescheinigungDownloadInfo.class);
        } else if (BerechtigungspruefungAlkisDownloadInfo.PRODUKT_TYP.equals(
                        berechtigungspruefungDownloadInfo.getProduktTyp())) {
            final BerechtigungspruefungAlkisDownloadInfo alkisDownloadInfo = MAPPER.readValue(
                    downloadinfoJson,
                    BerechtigungspruefungAlkisDownloadInfo.class);
//            if  (BerechtigungspruefungAlkisDownloadInfo.AlkisObjektTyp.FLURSTUECKE.equals(alkisDownloadInfo.getAlkisObjectTyp())) {
            if (BerechtigungspruefungAlkisDownloadInfo.AlkisDownloadTyp.EINZELNACHWEIS.equals(
                            alkisDownloadInfo.getAlkisDownloadTyp())) {
                return MAPPER.readValue(downloadinfoJson, BerechtigungspruefungAlkisEinzelnachweisDownloadInfo.class);
            } else if (BerechtigungspruefungAlkisDownloadInfo.AlkisDownloadTyp.KARTE.equals(
                            alkisDownloadInfo.getAlkisDownloadTyp())) {
                return MAPPER.readValue(downloadinfoJson, BerechtigungspruefungAlkisKarteDownloadInfo.class);
            }
//            } else if (BerechtigungspruefungAlkisDownloadInfo.AlkisObjektTyp.BUCHUNGSBLAETTER.equals(alkisDownloadInfo.getAlkisObjectTyp())) {
//
//            }
        } else if (BerechtigungspruefungAlkisEinzelnachweisDownloadInfo.PRODUKT_TYP.equals(
                        berechtigungspruefungDownloadInfo.getProduktTyp())) {
            return MAPPER.readValue(downloadinfoJson, BerechtigungspruefungAlkisEinzelnachweisDownloadInfo.class);
        }
        throw new Exception("unbekannter Download-Typ");
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static Date getThresholdAnhangDate() {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        final Date threshold = new Date(calendar.getTimeInMillis());
        return threshold;
    }

    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return serverConnectionContext;
    }

    @Override
    public void setServerConnectionContext(final ServerConnectionContext serverConnectionContext) {
        this.serverConnectionContext = serverConnectionContext;
    }
}
