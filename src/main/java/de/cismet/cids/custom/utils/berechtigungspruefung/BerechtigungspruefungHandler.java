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
import Sirius.server.newuser.UserServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import java.rmi.Naming;

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

import de.cismet.cids.server.messages.CidsServerMessageManagerImpl;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class BerechtigungspruefungHandler implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungHandler.class);

    //~ Instance fields --------------------------------------------------------

    @Getter @Setter private MetaService metaService;

    @Getter private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Getter private final User user;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BerechtigungspruefungHandler object.
     */
    private BerechtigungspruefungHandler() {
        User user = null;
        try {
            final Object userServer = Naming.lookup("rmi://localhost/userServer");
            user = ((UserServer)userServer).getUser(
                    null,
                    null,
                    "WUNDA_BLAU",
                    BerechtigungspruefungProperties.getInstance().getCidsLogin(),
                    BerechtigungspruefungProperties.getInstance().getCidsPassword());
        } catch (final Exception ex) {
            LOG.fatal(ex, ex);
        }
        this.user = user;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BerechtigungspruefungHandler getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    /**
     * DOCUMENT ME!
     */
    public void deleteOldDateianhangFiles() {
        final File directory = new File(BerechtigungspruefungProperties.getInstance().getAnhangAbsPath());
        final Date thresholdDate = getThresholdAnhangDate();

        // look for all anhang files
        if (directory.listFiles() != null) {
            for (final File file : directory.listFiles()) {
                if (file.isFile()) {
                    try {
                        final String fileName = file.getName();
                        final BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                        final Date creationDate = new Date(attr.creationTime().toMillis());

                        // file older then threshold date (1 month) ?
                        if (creationDate.before(thresholdDate)) {
                            final CidsBean anfrageBean = loadAnfrageBean(fileName);
                            // assuring, that the file corresponds to an existing bean This prevents accidental deletion
                            // of non-anhang files (i.e. if AnhangAbsPath was set to a path that contains also other
                            // files)
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
    }

    /**
     * DOCUMENT ME!
     */
    public void sendMessagesForAllOpenAnfragen() {
        final Collection<CidsBean> allOpenPruefungen = loadOpenAnfrageBeans();
        sendAnfrageMessages(allOpenPruefungen);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   schluessel          DOCUMENT ME!
     * @param   pruefer             DOCUMENT ME!
     * @param   pruefStatus         DOCUMENT ME!
     * @param   begruendung         DOCUMENT ME!
     * @param   pruefungsAbschluss  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public boolean pruefung(final String schluessel,
            final String pruefer,
            final Boolean pruefStatus,
            final String begruendung,
            final boolean pruefungsAbschluss) throws Exception {
        final CidsBean pruefungBean = loadAnfrageBean(schluessel);

        final BerechtigungspruefungDownloadInfo downloadInfo = BerechtigungspruefungHandler.extractDownloadInfo((String)
                pruefungBean.getProperty("downloadinfo_json"));

        if (!Boolean.TRUE.equals(pruefungBean.getProperty("abgeholt"))
                    && (pruefungBean.getProperty("pruefstatus") != null)) {
            return true;
//                } else if (pruefungsAbschluss && (pruefungBean.getProperty("pruefer") != null)
//                            && !pruefer.equals(pruefungBean.getProperty("pruefer"))) {
//                    return ReturnType.PENDING;
        }
        final String userKey = (String)pruefungBean.getProperty("benutzer");

        final Timestamp now = new Timestamp(new Date().getTime());
        pruefungBean.setProperty("abgeholt", null);
        pruefungBean.setProperty("pruefer", pruefer);
        pruefungBean.setProperty("pruefstatus", pruefStatus);
        if (pruefungsAbschluss) {
            pruefungBean.setProperty("freigabe_timestamp", now);
            pruefungBean.setProperty("pruefkommentar", begruendung);
        } else {
            pruefungBean.setProperty("pruefung_timestamp", now);
        }

        getMetaService().updateMetaObject(
            getUser(),
            pruefungBean.getMetaObject(),
            getConnectionContext());

        if (pruefungsAbschluss) {
            if (downloadInfo instanceof BerechtigungspruefungBillingDownloadInfo) {
                final BerechtigungspruefungBillingDownloadInfo billingDownloadinfo =
                    (BerechtigungspruefungBillingDownloadInfo)downloadInfo;
                final Integer billingId = billingDownloadinfo.getBillingId();
                if (billingId != null) {
                    if (pruefStatus) {
                        try {
                            final String json = (String)pruefungBean.getProperty("downloadinfo_json");
                            updateRequestInBillingBean(billingId, json);
                        } catch (final Exception ex) {
                            LOG.error("error while setting 'request' of billing", ex);
                        }
                    } else {
                        try {
                            updateStornoInBillingBean(billingId);
                        } catch (final Exception ex) {
                            LOG.error("Error while setting 'storniert' of billing", ex);
                        }
                    }
                }
            }

            sendFreigabeMessage(userKey, Arrays.asList(pruefungBean));
        } else {
            sendProcessingMessage(schluessel);
        }
        return false;
    }
    /**
     * DOCUMENT ME!
     *
     * @param  userKey  DOCUMENT ME!
     */
    public void sendMessagesForAllOpenFreigaben(final String userKey) {
        final Collection<CidsBean> allOpenDownloads = loadOpenFreigabeBeans(userKey);
        sendFreigabeMessage(userKey, allOpenDownloads);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  schluessel  DOCUMENT ME!
     */
    private void sendProcessingMessage(final String schluessel) {
        CidsServerMessageManagerImpl.getInstance()
                .publishMessage(BerechtigungspruefungProperties.getInstance().getCsmBearbeitung(),
                    schluessel,
                    true,
                    getConnectionContext());
    }

    /**
     * DOCUMENT ME!
     */
    public void sendMessagesForAllOpenFreigaben() {
        final Collection<CidsBean> allOpenDownloads = loadOpenFreigabeBeans();
        if (allOpenDownloads != null) {
            final Map<String, Collection> dm = new HashMap<>();
            for (final CidsBean openDownload : allOpenDownloads) {
                final String userKey = (String)openDownload.getProperty("benutzer");
                if (userKey != null) {
                    if (!dm.containsKey(userKey)) {
                        dm.put(userKey, new ArrayList<>());
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
     * @param  anfrageBeans  schluessel openPruefung DOCUMENT ME!
     */
    private void sendAnfrageMessages(final Collection<CidsBean> anfrageBeans) {
        final Collection<String> schluesselList = new ArrayList<>();
        if (anfrageBeans != null) {
            for (final CidsBean anfrageBean : anfrageBeans) {
                schluesselList.add((String)anfrageBean.getProperty("schluessel"));
            }
        }

        CidsServerMessageManagerImpl.getInstance()
                .publishMessage(BerechtigungspruefungProperties.getInstance().getCsmAnfrage(),
                    schluesselList,
                    true,
                    getConnectionContext());
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey        DOCUMENT ME!
     * @param  pruefungBeans  DOCUMENT ME!
     */
    private void sendFreigabeMessage(final String userKey, final Collection<CidsBean> pruefungBeans) {
        if (pruefungBeans != null) {
            try {
                final List<String> schluesselList = new ArrayList<>();

                for (final CidsBean pruefungBean : pruefungBeans) {
                    schluesselList.add((String)pruefungBean.getProperty("schluessel"));
                }

                if (!schluesselList.isEmpty()) {
                    // an den Pruefer
                    CidsServerMessageManagerImpl.getInstance()
                            .publishMessage(BerechtigungspruefungProperties.getInstance().getCsmBearbeitung(),
                                schluesselList,
                                true,
                                getConnectionContext());
                    // an den Anfragenden
                    CidsServerMessageManagerImpl.getInstance()
                            .publishMessage(BerechtigungspruefungProperties.getInstance().getCsmFreigabe(),
                                schluesselList,
                                false,
                                new HashSet(Arrays.asList(userKey)),
                                true,
                                getConnectionContext());
                }
            } catch (final Exception ex) {
                LOG.error("error while producing or sending message", ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   schluessel  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void closeAnfrage(final String schluessel) throws Exception {
        final CidsBean pruefungBean = loadAnfrageBean(schluessel);
        pruefungBean.setProperty("abholung_timestamp", new Timestamp(new Date().getTime()));
        pruefungBean.setProperty("abgeholt", true);

        DomainServerImpl.getServerInstance()
                .updateMetaObject(getUser(), pruefungBean.getMetaObject(), getConnectionContext());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   downloadInfo  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String createNewSchluessel(final BerechtigungspruefungDownloadInfo downloadInfo) {
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
            final CidsBean lastAnfrageBean = loadLastAnfrageBeanByTypeAndYear(type, year);
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
     * @param   userKey             DOCUMENT ME!
     * @param   schluessel          DOCUMENT ME!
     * @param   downloadInfo        DOCUMENT ME!
     * @param   berechtigungsgrund  DOCUMENT ME!
     * @param   begruendung         DOCUMENT ME!
     * @param   dateiname           DOCUMENT ME!
     * @param   data                DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public CidsBean addNewAnfrage(final String userKey,
            final String schluessel,
            final BerechtigungspruefungDownloadInfo downloadInfo,
            final String berechtigungsgrund,
            final String begruendung,
            final String dateiname,
            final byte[] data) throws Exception {
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
                "berechtigungspruefung",
                getConnectionContext());
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

        final CidsBean insertedPruefungBean = DomainServerImpl.getServerInstance()
                    .insertMetaObject(getUser(), newPruefungBean.getMetaObject(), getConnectionContext())
                    .getBean();

        sendAnfrageMessages(Arrays.asList(insertedPruefungBean));

        return insertedPruefungBean;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private List<CidsBean> loadOpenAnfrageBeans() {
        final MetaObject[] mos;
        try {
            final List<CidsBean> beans = new ArrayList<>();
            final MetaClass mcBerechtigungspruefung = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "berechtigungspruefung",
                    getConnectionContext());

            final String pruefungQuery = "SELECT DISTINCT " + mcBerechtigungspruefung.getID() + ", "
                        + mcBerechtigungspruefung.getTableName() + "." + mcBerechtigungspruefung.getPrimaryKey() + " "
                        + "FROM " + mcBerechtigungspruefung.getTableName() + " "
                        + "WHERE " + mcBerechtigungspruefung.getTableName() + ".pruefstatus IS NULL;";

            mos = getMetaService().getMetaObject(getUser(), pruefungQuery, getConnectionContext());
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
     *
     * @return  DOCUMENT ME!
     */
    public List<CidsBean> loadOpenFreigabeBeans(final String userKey) {
        final MetaObject[] mos;
        try {
            final List<CidsBean> beans = new ArrayList<>();
            final MetaClass mcBerechtigungspruefung = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "berechtigungspruefung",
                    getConnectionContext());

            final String pruefungQuery = "SELECT DISTINCT " + mcBerechtigungspruefung.getID() + ", "
                        + mcBerechtigungspruefung.getTableName() + "." + mcBerechtigungspruefung.getPrimaryKey() + " "
                        + "FROM " + mcBerechtigungspruefung.getTableName() + " "
                        + "WHERE benutzer ILIKE '" + userKey + "' "
                        + "AND " + mcBerechtigungspruefung.getTableName() + ".pruefstatus IS NOT NULL "
                        + "AND (" + mcBerechtigungspruefung.getTableName() + ".abgeholt IS NULL "
                        + "OR " + mcBerechtigungspruefung.getTableName() + ".abgeholt IS FALSE);";

            mos = getMetaService().getMetaObject(getUser(), pruefungQuery, getConnectionContext());
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
     * @return  DOCUMENT ME!
     */
    private List<CidsBean> loadOpenFreigabeBeans() {
        final MetaObject[] mos;
        try {
            final List<CidsBean> beans = new ArrayList<>();
            final MetaClass mcBerechtigungspruefung = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "berechtigungspruefung",
                    getConnectionContext());

            final String pruefungQuery = "SELECT DISTINCT " + mcBerechtigungspruefung.getID() + ", "
                        + mcBerechtigungspruefung.getTableName() + "." + mcBerechtigungspruefung.getPrimaryKey() + " "
                        + "FROM " + mcBerechtigungspruefung.getTableName() + " "
                        + "WHERE " + mcBerechtigungspruefung.getTableName() + ".pruefstatus IS NOT NULL "
                        + "AND (" + mcBerechtigungspruefung.getTableName() + ".abgeholt IS NULL "
                        + "OR " + mcBerechtigungspruefung.getTableName() + ".abgeholt IS FALSE);";

            mos = getMetaService().getMetaObject(getUser(), pruefungQuery, getConnectionContext());
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
     * @param   schluessel  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private CidsBean loadAnfrageBean(final String schluessel) {
        try {
            final MetaClass mcBerechtigungspruefung = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "berechtigungspruefung",
                    getConnectionContext());

            final String pruefungQuery = "SELECT DISTINCT " + mcBerechtigungspruefung.getID() + ", "
                        + mcBerechtigungspruefung.getTableName() + "." + mcBerechtigungspruefung.getPrimaryKey() + " "
                        + "FROM " + mcBerechtigungspruefung.getTableName() + " "
                        + "WHERE " + mcBerechtigungspruefung.getTableName() + ".schluessel LIKE '" + schluessel + "' "
                        + "LIMIT 1;";

            final MetaObject[] mos = getMetaService().getMetaObject(getUser(), pruefungQuery, getConnectionContext());
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
     * @param   id  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private CidsBean loadBillingBean(final int id) throws Exception {
        final MetaClass mcBillingBilling = CidsBean.getMetaClassFromTableName(
                "WUNDA_BLAU",
                "billing_billing",
                getConnectionContext());

        final MetaObject mo = getMetaService().getMetaObject(
                getUser(),
                id,
                mcBillingBilling.getID(),
                getConnectionContext());
        return mo.getBean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   id    DOCUMENT ME!
     * @param   json  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void updateRequestInBillingBean(final int id, final String json) throws Exception {
        final CidsBean billingBean = loadBillingBean(id);
        billingBean.setProperty("request", json);
        getMetaService().updateMetaObject(
            getUser(),
            billingBean.getMetaObject(),
            getConnectionContext());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   id  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void updateStornoInBillingBean(final int id) throws Exception {
        final CidsBean billingBean = loadBillingBean(id);
        final CidsBean billingStornogrundBean = loadBillingStornogrundBean();

        billingBean.setProperty("storniert", Boolean.TRUE);
        billingBean.setProperty("stornogrund", billingStornogrundBean);
        billingBean.setProperty("storniert_durch", getUser().toString());

        getMetaService().updateMetaObject(
            getUser(),
            billingBean.getMetaObject(),
            getConnectionContext());
    }
    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private CidsBean loadBillingStornogrundBean() {
        try {
            final MetaClass mcBillingStornogrund = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "billing_stornogrund",
                    getConnectionContext());

            final MetaObject mo = getMetaService().getMetaObject(
                    getUser(),
                    BerechtigungspruefungProperties.getInstance().getBillingStornogrundId(),
                    mcBillingStornogrund.getID(),
                    getConnectionContext());
            return mo.getBean();
        } catch (final Exception ex) {
            LOG.error("error while loading billing_billing bean", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   type  DOCUMENT ME!
     * @param   year  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public CidsBean loadLastAnfrageBeanByTypeAndYear(final String type, final int year) {
        try {
            final MetaClass mcBerechtigungspruefung = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "berechtigungspruefung",
                    getConnectionContext());

            final String tester = "^" + type + "-" + Integer.toString(year) + "-\\\\d{5}$";
            final String pruefungQuery = "SELECT " + mcBerechtigungspruefung.getID() + ", "
                        + mcBerechtigungspruefung.getTableName() + "." + mcBerechtigungspruefung.getPrimaryKey() + " "
                        + "FROM " + mcBerechtigungspruefung.getTableName() + " "
                        + "WHERE " + mcBerechtigungspruefung.getTableName() + ".schluessel ~ E'" + tester + "' "
                        + "ORDER BY schluessel DESC "
                        + "LIMIT 1;";

            final MetaObject[] mos = getMetaService().getMetaObject(getUser(), pruefungQuery, getConnectionContext());
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
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final BerechtigungspruefungHandler INSTANCE = new BerechtigungspruefungHandler();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
