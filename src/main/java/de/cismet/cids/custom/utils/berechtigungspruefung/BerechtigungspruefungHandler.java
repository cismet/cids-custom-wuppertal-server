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
import org.apache.commons.lang.RandomStringUtils;

import java.io.File;
import java.io.IOException;

import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungDownloadInfo;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.messages.CidsServerMessageManagerImpl;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class BerechtigungspruefungHandler {

    //~ Static fields/initializers ---------------------------------------------

    public static final String UPLOAD_PATH = "/tmp/";

    public static final String MESSAGE_CATEGORY_ANFRAGE = "berechtigungspruefungAnfrage";
    public static final String MESSAGE_CATEGORY_FREIGABE = "berechtigungspruefungFreigabe";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static BerechtigungspruefungHandler INSTANCE;
    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungHandler.class);

    //~ Instance fields --------------------------------------------------------

    private MetaService metaService;

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
    public void sendMessagesForAllOpenPruefungen(final User user) {
        final Collection<CidsBean> allOpenPruefungen = loadOpenPruefungBeans(user);
        if (allOpenPruefungen != null) {
            for (final CidsBean openPruefungen : allOpenPruefungen) {
                sendPruefungMessage((String)openPruefungen.getProperty("schluessel"));
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey  DOCUMENT ME!
     * @param  user     DOCUMENT ME!
     */
    public void sendMessagesForAllOpenDownloads(final String userKey, final User user) {
        final Collection<CidsBean> allOpenDownloads = loadOpenDownloadBeans(userKey, user);
        sendDownloadMessage(userKey, allOpenDownloads);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  user  DOCUMENT ME!
     */
    public void sendMessagesForAllOpenDownloads(final User user) {
        final Collection<CidsBean> allOpenDownloads = loadOpenDownloadBeans(user);
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
                sendDownloadMessage(userKey, dm.get(userKey));
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
     * @param  schluessel  openPruefung DOCUMENT ME!
     */
    private void sendPruefungMessage(final String schluessel) {
        CidsServerMessageManagerImpl.getInstance().publishMessage(MESSAGE_CATEGORY_ANFRAGE, schluessel);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey        DOCUMENT ME!
     * @param  pruefungBeans  DOCUMENT ME!
     */
    public void sendDownloadMessage(final String userKey, final Collection<CidsBean> pruefungBeans) {
        if (pruefungBeans != null) {
            try {
                final Map<String, BerechtigungspruefungInfo> pruefungInfoMap =
                    new HashMap<String, BerechtigungspruefungInfo>();

                for (final CidsBean pruefungBean : pruefungBeans) {
                    final String schluessel = (String)pruefungBean.getProperty("schluessel");

                    final BerechtigungspruefungDownloadInfo downloadInfo = extractDownloadInfo((String)
                            pruefungBean.getProperty("downloadinfo_json"));

                    final BerechtigungspruefungInfo berechtigungspruefungInfo;
                    if (downloadInfo instanceof BerechtigungspruefungBescheinigungDownloadInfo) {
                        berechtigungspruefungInfo =
                            new BerechtigungspruefungInfo<BerechtigungspruefungBescheinigungDownloadInfo>((String)
                                pruefungBean.getProperty("pruefkommentar"),
                                (Boolean)pruefungBean.getProperty("pruefstatus"),
                                (BerechtigungspruefungBescheinigungDownloadInfo)downloadInfo);
                    } else {
                        berechtigungspruefungInfo = new BerechtigungspruefungInfo((String)pruefungBean.getProperty(
                                    "pruefkommentar"),
                                (Boolean)pruefungBean.getProperty("pruefstatus"),
                                downloadInfo);
                    }
                    pruefungInfoMap.put(
                        schluessel,
                        berechtigungspruefungInfo);
                }
                CidsServerMessageManagerImpl.getInstance()
                        .publishMessage(
                            BerechtigungspruefungHandler.MESSAGE_CATEGORY_FREIGABE,
                            MAPPER.writeValueAsString(pruefungInfoMap),
                            new HashSet(Arrays.asList(userKey)),
                            true);
            } catch (final Exception ex) {
                LOG.error("error while producing or sending message", ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user                DOCUMENT ME!
     * @param   downloadInfo        produktbezeichnung DOCUMENT ME!
     * @param   berechtigungsgrund  DOCUMENT ME!
     * @param   begruendung         DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public String addNewPruefung(final User user,
            final BerechtigungspruefungDownloadInfo downloadInfo,
            final String berechtigungsgrund,
            final String begruendung) throws Exception {
        return addNewPruefung(
                user,
                downloadInfo,
                berechtigungsgrund,
                begruendung,
                null,
                null);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user        DOCUMENT ME!
     * @param   schluessel  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void closePruefung(final User user, final String schluessel) throws Exception {
        final CidsBean pruefungBean = loadPruefungBean(user, schluessel);
        pruefungBean.setProperty("abholung_timestamp", new Timestamp(new Date().getTime()));
        pruefungBean.setProperty("abgeholt", true);

        DomainServerImpl.getServerInstance().updateMetaObject(user, pruefungBean.getMetaObject());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user                DOCUMENT ME!
     * @param   downloadInfo        produktbezeichnung DOCUMENT ME!
     * @param   berechtigungsgrund  DOCUMENT ME!
     * @param   begruendung         DOCUMENT ME!
     * @param   dateiname           DOCUMENT ME!
     * @param   data                DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public String addNewPruefung(final User user,
            final BerechtigungspruefungDownloadInfo downloadInfo,
            final String berechtigungsgrund,
            final String begruendung,
            final String dateiname,
            final byte[] data) throws Exception {
        final String userKey = (String)user.getKey();
        String pruefungKey;
        do {
            pruefungKey = RandomStringUtils.randomAlphanumeric(8);
        } while (loadPruefungBean(user, pruefungKey) != null);

        if ((data != null) && (dateiname != null)) {
            final File file = new File(UPLOAD_PATH + pruefungKey);
            try {
                FileUtils.writeByteArrayToFile(file, data);
            } catch (final IOException ex) {
                throw new Exception("Datei konnte nicht geschrieben werden.", ex);
            }
        }

        final CidsBean newPruefungBean = CidsBean.createNewCidsBeanFromTableName("WUNDA_BLAU", "berechtigungspruefung");
        newPruefungBean.setProperty("dateiname", dateiname);
        newPruefungBean.setProperty("schluessel", pruefungKey);
        newPruefungBean.setProperty("anfrage_timestamp", new Timestamp(new Date().getTime()));
        newPruefungBean.setProperty("berechtigungsgrund", berechtigungsgrund);
        newPruefungBean.setProperty("begruendung", begruendung);
        newPruefungBean.setProperty("benutzer", userKey);
        newPruefungBean.setProperty("abgeholt", false);
        newPruefungBean.setProperty("pruefstatus", null);
        newPruefungBean.setProperty("downloadinfo_json", new ObjectMapper().writeValueAsString(downloadInfo));

        DomainServerImpl.getServerInstance().insertMetaObject(user, newPruefungBean.getMetaObject());

        sendPruefungMessage(pruefungKey);

        return pruefungKey;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<CidsBean> loadOpenPruefungBeans(final User user) {
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

            mos = metaService.getMetaObject(user, pruefungQuery);
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
    public List<CidsBean> loadOpenDownloadBeans(final String userKey, final User user) {
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
                        + "AND " + mcBerechtigungspruefung.getTableName() + ".abgeholt IS NOT TRUE;";

            mos = metaService.getMetaObject(user, pruefungQuery);
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
    public List<CidsBean> loadOpenDownloadBeans(final User user) {
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
                        + "AND " + mcBerechtigungspruefung.getTableName() + ".abgeholt IS NOT TRUE;";

            mos = metaService.getMetaObject(user, pruefungQuery);
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
    public CidsBean loadPruefungBean(final User user, final String schluessel) {
        try {
            final MetaClass mcBerechtigungspruefung = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "berechtigungspruefung");

            final String pruefungQuery = "SELECT DISTINCT " + mcBerechtigungspruefung.getID() + ", "
                        + mcBerechtigungspruefung.getTableName() + "." + mcBerechtigungspruefung.getPrimaryKey() + " "
                        + "FROM " + mcBerechtigungspruefung.getTableName() + " "
                        + "WHERE " + mcBerechtigungspruefung.getTableName() + ".schluessel LIKE '" + schluessel + "' "
                        + "LIMIT 1;";

            final MetaObject[] mos = metaService.getMetaObject(user, pruefungQuery);
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
     * @param   downloadInfo_json  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static BerechtigungspruefungDownloadInfo extractDownloadInfo(final String downloadInfo_json)
            throws Exception {
        final BerechtigungspruefungDownloadInfo berechtigungspruefungDownloadInfo = (BerechtigungspruefungDownloadInfo)
            MAPPER.readValue(downloadInfo_json, BerechtigungspruefungDownloadInfo.class);

        // TODO weitere produkte haben eigene downloadinfo klassen
        if (BerechtigungspruefungBescheinigungDownloadInfo.PRODUKT_TYP.equals(
                        berechtigungspruefungDownloadInfo.getProduktTyp())) {
            return MAPPER.readValue(downloadInfo_json, BerechtigungspruefungBescheinigungDownloadInfo.class);
        } else {
            throw new Exception("unbekannter Download-Typ");
        }
    }
}
