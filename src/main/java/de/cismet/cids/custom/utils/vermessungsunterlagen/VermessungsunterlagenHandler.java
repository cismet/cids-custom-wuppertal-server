/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.vermessungsunterlagen;

/**
 * *************************************************
 *
 * cismet GmbH, Saarbruecken, Germany
 *
 * ... and it just works.
 *
 ***************************************************
 */
import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.LightweightMetaObject;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

import org.openide.util.Lookup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;

import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.nas.NasProduct;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenException;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.CidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cids.utils.MetaClassCacheService;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.commons.concurrency.CismetExecutors;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextProvider;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenHandler implements ConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(VermessungsunterlagenHandler.class);

    public static final String ALLOWED_TASKS_CONFIG_ATTR = "vup.tasks_allowed";

    public static final NasProduct NAS_PRODUCT_KOMPLETT;
    public static final NasProduct NAS_PRODUCT_OHNE_EIGENTUEMER;
    public static final NasProduct NAS_PRODUCT_PUNKTE;

    static {
        final ObjectMapper mapper = new ObjectMapper();
        NasProduct productPunkte = null;
        NasProduct productOhneEigentuemer = null;
        NasProduct productKomplett = null;
        final ArrayList<NasProduct> nasProducts;
        try {
            nasProducts = mapper.readValue(ServerResourcesLoader.getInstance().loadStringReader(
                        WundaBlauServerResources.NAS_PRODUCT_DESCRIPTION_JSON.getValue()),
                    mapper.getTypeFactory().constructCollectionType(List.class, NasProduct.class));
            for (final NasProduct nasProduct : nasProducts) {
                if ("punkte".equals(nasProduct.getKey())) {
                    productPunkte = nasProduct;
                } else if ("ohne_eigentuemer".equals(nasProduct.getKey())) {
                    productOhneEigentuemer = nasProduct;
                } else if ("komplett".equals(nasProduct.getKey())) {
                    productKomplett = nasProduct;
                }
            }
        } catch (final Exception ex) {
            final String message = "could not load NasProducts";
            LOG.error(message, ex);
            throw new RuntimeException(message, ex);
        }
        NAS_PRODUCT_PUNKTE = productPunkte;
        NAS_PRODUCT_OHNE_EIGENTUEMER = productOhneEigentuemer;
        NAS_PRODUCT_KOMPLETT = productKomplett;
    }

    private static final Map<String, VermessungsunterlagenJobInfoWrapper> JOB_MAP = new ConcurrentHashMap<>();

    public static int MAX_BEAN_LOADING = 1000;

    //~ Instance fields --------------------------------------------------------

    private final MetaService metaService;
    private final User user;
    private final VermessungsunterlagenProperties vermessungsunterlagenProperties = VermessungsunterlagenProperties
                .fromServerResources();

    private final ConnectionContext connectionContext;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenHelper object.
     *
     * @param  user               DOCUMENT ME!
     * @param  metaService        DOCUMENT ME!
     * @param  connectionContext  DOCUMENT ME!
     */
    public VermessungsunterlagenHandler(final User user,
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
    public VermessungsunterlagenProperties getProperties() {
        return vermessungsunterlagenProperties;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   executeJobContent  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String createJob(final String executeJobContent) {
        return createJob(executeJobContent, false);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   executeJobContent  DOCUMENT ME!
     * @param   test               DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String createJob(final String executeJobContent, final boolean test) {
        try {
            final VermessungsunterlagenAnfrageBean anfrageBean = VermessungsunterlagenUtils.createAnfrageBean(
                    executeJobContent);
            anfrageBean.setTest(test);
            final String jobKey = anfrageBean.getZulassungsnummerVermessungsstelle() + "_"
                        + anfrageBean.getGeschaeftsbuchnummer() + "_" + generateUniqueJobKey();

            final VermessungsunterlagenJob job = new VermessungsunterlagenJob(
                    jobKey,
                    anfrageBean,
                    getProperties(),
                    getUser(),
                    getMetaService(),
                    getConnectionContext());
            try {
                persistJobCidsBean(job, executeJobContent);
                CismetExecutors.newSingleThreadExecutor().execute(job);
            } catch (final Exception ex) {
                LOG.info("error while persisting Job", ex);
                job.setStatus(VermessungsunterlagenJob.Status.ERROR);
                job.setException(new VermessungsunterlagenException(
                        "Der Datensatz konnte nicht abgespeichert werden.",
                        ex));
            }
            JOB_MAP.put(jobKey, new VermessungsunterlagenJobInfoWrapper(job));
            return jobKey;
        } catch (final Exception ex) {
            LOG.error("Unexpected error while creating job !", ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   lwmo  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public CidsBean loadCidsBean(final LightweightMetaObject lwmo) throws Exception {
        return getMetaService().getMetaObject(
                    getUser(),
                    lwmo.getObjectID(),
                    lwmo.getClassID(),
                    getConnectionContext()).getBean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   mon  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public CidsBean loadCidsBean(final MetaObjectNode mon) throws Exception {
        return getMetaService().getMetaObject(
                    getUser(),
                    mon.getObjectId(),
                    mon.getClassId(),
                    getConnectionContext()).getBean();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private String generateUniqueJobKey() throws Exception {
        String jobKey;
        do {
            jobKey = RandomStringUtils.randomAlphanumeric(8);
        } while (isJobKeyAlreadyExisting(jobKey));
        return jobKey;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   jobKey  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private boolean isJobKeyAlreadyExisting(final String jobKey) throws Exception {
        if (JOB_MAP.containsKey(jobKey)) { // exists in memory ?
            return true;
        } else {                           // exists in database ?

            final MetaClass mc_VERMESSUNGSUNTERLAGENAUFTRAG = getMetaService().getClassByTableName(
                    getUser(),
                    "vermessungsunterlagenauftrag",
                    getConnectionContext());

            final List result = getMetaService().performCustomSearch("SELECT schluessel FROM "
                            + mc_VERMESSUNGSUNTERLAGENAUFTRAG + " WHERE schluessel LIKE '" + jobKey + "'",
                    getConnectionContext());
            return !result.isEmpty();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  jobKey  DOCUMENT ME!
     */
    public void cleanup(final String jobKey) {
        final VermessungsunterlagenJobInfoWrapper infoWrapper = JOB_MAP.get(jobKey);
        if (infoWrapper != null) {
            infoWrapper.cleanup();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   jobkey  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public VermessungsunterlagenJobInfoWrapper getJobInfo(final String jobkey) {
        if (JOB_MAP.containsKey(jobkey)) {
            return JOB_MAP.get(jobkey);
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   job                anfrageBean DOCUMENT ME!
     * @param   executeJobContent  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void persistJobCidsBean(final VermessungsunterlagenJob job, final String executeJobContent)
            throws Exception {
        final VermessungsunterlagenAnfrageBean anfrageBean = job.getAnfrageBean();

        final MetaClass mc_GEOM = getMetaService().getClassByTableName(getUser(), "geom", getConnectionContext());
        final MetaClass mc_VERMESSUNGSUNTERLAGENAUFTRAG_PUNKTNUMMER = getMetaService().getClassByTableName(
                getUser(),
                "vermessungsunterlagenauftrag_punktnummer",
                getConnectionContext());
        final MetaClass mc_VERMESSUNGSUNTERLAGENAUFTRAG = getMetaService().getClassByTableName(
                getUser(),
                "vermessungsunterlagenauftrag",
                getConnectionContext());
        final MetaClass mc_VERMESSUNGSUNTERLAGENAUFTRAG_VERMESSUNGSART = getMetaService().getClassByTableName(
                getUser(),
                "vermessungsunterlagenauftrag_vermessungsart",
                getConnectionContext());
        final MetaClass mc_VERMESSUNGSUNTERLAGENAUFTRAG_FLURSTUECK = getMetaService().getClassByTableName(
                getUser(),
                "vermessungsunterlagenauftrag_flurstueck",
                getConnectionContext());

        final Polygon[] aparr = anfrageBean.getAnfragepolygonArray();
        final Geometry geometry = ((aparr != null) && (aparr.length > 0)) ? aparr[0] : null;
        final CidsBean geomBean;
        if (geometry != null) {
            geometry.setSRID(VermessungsunterlagenUtils.SRID);
            geomBean = CidsBean.createNewCidsBeanFromTableName(
                    "WUNDA_BLAU",
                    mc_GEOM.getTableName(),
                    getConnectionContext());
            geomBean.setProperty("geo_field", geometry);
        } else {
            geomBean = null;
        }

        final CidsBean jobCidsBean = CidsBean.createNewCidsBeanFromTableName(
                "WUNDA_BLAU",
                mc_VERMESSUNGSUNTERLAGENAUFTRAG.getTableName(),
                getConnectionContext());
        jobCidsBean.setProperty("portalversion", anfrageBean.getPortalVersion());

        jobCidsBean.setProperty("executejob_json", executeJobContent);
        jobCidsBean.setProperty("schluessel", job.getKey());
        jobCidsBean.setProperty("geometrie", geomBean);
        jobCidsBean.setProperty("aktenzeichen", anfrageBean.getAktenzeichenKatasteramt());
        if (anfrageBean.getAntragsflurstuecksArray() != null) {
            for (final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean flurstueckBean
                        : anfrageBean.getAntragsflurstuecksArray()) {
                final CidsBean flurstueck = CidsBean.createNewCidsBeanFromTableName(
                        "WUNDA_BLAU",
                        mc_VERMESSUNGSUNTERLAGENAUFTRAG_FLURSTUECK.getTableName(),
                        getConnectionContext());
                flurstueck.setProperty("gemarkung", flurstueckBean.getGemarkungsID());
                flurstueck.setProperty("flur", flurstueckBean.getFlurID());
                flurstueck.setProperty("flurstueck", flurstueckBean.getFlurstuecksID());
                jobCidsBean.getBeanCollectionProperty("flurstuecke").add(flurstueck);
            }
        }
        if (anfrageBean.getPunktnummernreservierungsArray() != null) {
            for (final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean pnrBean
                        : anfrageBean.getPunktnummernreservierungsArray()) {
                final CidsBean pnr = CidsBean.createNewCidsBeanFromTableName(
                        "WUNDA_BLAU",
                        mc_VERMESSUNGSUNTERLAGENAUFTRAG_PUNKTNUMMER.getTableName(),
                        getConnectionContext());
                pnr.setProperty("anzahl", pnrBean.getAnzahlPunktnummern());
                pnr.setProperty("katasteramt", pnrBean.getKatasteramtsID());
                pnr.setProperty("kilometerquadrat", pnrBean.getUtmKilometerQuadrat());
                jobCidsBean.getBeanCollectionProperty("punktnummern").add(pnr);
            }
        }
        jobCidsBean.setProperty("mit_grenzniederschriften", anfrageBean.getMitGrenzniederschriften());
        jobCidsBean.setProperty("geschaeftsbuchnummer", anfrageBean.getGeschaeftsbuchnummer());
        jobCidsBean.setProperty("auftragsnummer", anfrageBean.getKatasteramtAuftragsnummer());
        jobCidsBean.setProperty("katasteramtsid", anfrageBean.getKatasteramtsId());
        jobCidsBean.setProperty("vermessungsstelle", anfrageBean.getZulassungsnummerVermessungsstelle());
        jobCidsBean.setProperty("nur_punktnummernreservierung", anfrageBean.getNurPunktnummernreservierung());

        jobCidsBean.setProperty(
            "mit_alkisbestandsdatenmiteigentuemerinfo",
            anfrageBean.getMitAlkisBestandsdatenmitEigentuemerinfo());
        jobCidsBean.setProperty(
            "mit_alkisbestandsdatenohneeigentuemerinfo",
            anfrageBean.getMitAlkisBestandsdatenohneEigentuemerinfo());
        jobCidsBean.setProperty("mit_alkisbestandsdatennurpunkte", anfrageBean.getMitAlkisBestandsdatennurPunkte());
        jobCidsBean.setProperty("mit_punktnummernreservierung", anfrageBean.getMitPunktnummernreservierung());
        jobCidsBean.setProperty("mit_risse", anfrageBean.getMitRisse());
        jobCidsBean.setProperty("mit_apuebersichten", anfrageBean.getMitAPUebersichten());
        jobCidsBean.setProperty("mit_apkarten", anfrageBean.getMitAPKarten());
        jobCidsBean.setProperty("mit_apbeschreibungen", anfrageBean.getMitAPBeschreibungen());

        jobCidsBean.setProperty("anonym", anfrageBean.getAnonymousOrder());

        try {
            jobCidsBean.setProperty("saumap", Integer.parseInt(anfrageBean.getSaumAPSuche()));
        } catch (final Exception ex) {
            // validation will fail. Need to be catched so that the object can be persisted.
            // The validation exception will be stored in the exception_json field later on.
            // That's why the exception can be ignored here.
        }
        if (anfrageBean.getArtderVermessung() != null) {
            for (final String art : anfrageBean.getArtderVermessung()) {
                final CidsBean pnr = CidsBean.createNewCidsBeanFromTableName(
                        "WUNDA_BLAU",
                        mc_VERMESSUNGSUNTERLAGENAUFTRAG_VERMESSUNGSART.getTableName(),
                        getConnectionContext());
                pnr.setProperty("name", art);
                jobCidsBean.getBeanCollectionProperty("vermessungsarten").add(pnr);
            }
        }
        jobCidsBean.setProperty("timestamp", new Timestamp(new Date().getTime()));
        jobCidsBean.setProperty("tasks", Arrays.toString(getAllowedTasks().toArray()));
        jobCidsBean.setProperty("test", anfrageBean.isTest());

        job.setCidsBean(getMetaService().insertMetaObject(
                getUser(),
                jobCidsBean.getMetaObject(),
                getConnectionContext()).getBean());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public Collection<String> getAllowedTasks() throws Exception {
        final String rawAllowedTasks = DomainServerImpl.getServerInstance()
                    .getConfigAttr(getUser(), ALLOWED_TASKS_CONFIG_ATTR, getConnectionContext());
        final Collection<String> allowedTasks = new ArrayList<String>();
        if (rawAllowedTasks != null) {
            for (final String allowedTask : Arrays.asList(rawAllowedTasks.split("\n"))) {
                if (allowedTask != null) {
                    allowedTasks.add(allowedTask.trim());
                }
            }
        }
        return allowedTasks;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   job           DOCUMENT ME!
     * @param   zipDateiname  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public final void updateJobCidsBeanZip(final VermessungsunterlagenJob job, final String zipDateiname)
            throws Exception {
        final CidsBean jobCidsBean = job.getCidsBean();

        jobCidsBean.setProperty("zip_pfad", zipDateiname);
        jobCidsBean.setProperty("zip_timestamp", new Timestamp(new Date().getTime()));

        getMetaService().updateMetaObject(getUser(), jobCidsBean.getMetaObject(), getConnectionContext());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   job     DOCUMENT ME!
     * @param   status  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public final void updateJobCidsBeanStatus(final VermessungsunterlagenJob job, final Boolean status)
            throws Exception {
        final CidsBean jobCidsBean = job.getCidsBean();

        jobCidsBean.setProperty("status", status);
        jobCidsBean.setProperty("status_timestamp", new Timestamp(new Date().getTime()));

        getMetaService().updateMetaObject(getUser(), jobCidsBean.getMetaObject(), getConnectionContext());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   job   DOCUMENT ME!
     * @param   geom  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public final void updateJobCidsBeanFlurstueckGeom(final VermessungsunterlagenJob job, final Geometry geom)
            throws Exception {
        final CidsBean jobCidsBean = job.getCidsBean();

        final MetaClass mc_GEOM = getMetaService().getClassByTableName(getUser(), "geom", getConnectionContext());

        CidsBean geomBean;
        if (geom != null) {
            geomBean = (CidsBean)jobCidsBean.getProperty("geometrie_flurstuecke");
            if (geomBean == null) {
                geomBean = CidsBean.createNewCidsBeanFromTableName(
                        "WUNDA_BLAU",
                        mc_GEOM.getTableName(),
                        getConnectionContext());
            }
            geomBean.setProperty("geo_field", geom);
        } else {
            geomBean = null;
        }

        jobCidsBean.setProperty("geometrie_flurstuecke", geomBean);

        getMetaService().updateMetaObject(getUser(), jobCidsBean.getMetaObject(), getConnectionContext());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   job  DOCUMENT ME!
     * @param   ex   DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public final void updateJobCidsBeanException(final VermessungsunterlagenJob job, final Exception ex)
            throws Exception {
        final CidsBean jobCidsBean = job.getCidsBean();

        jobCidsBean.setProperty("exception_json", VermessungsunterlagenUtils.getExceptionJson(ex));
        jobCidsBean.setProperty("exception_timestamp", new Timestamp(new Date().getTime()));

        getMetaService().updateMetaObject(getUser(), jobCidsBean.getMetaObject(), getConnectionContext());
    }

    /**
     * DOCUMENT ME!
     */
    public void test() {
        try {
            if ((vermessungsunterlagenProperties.getAbsPathTest() != null)
                        && !vermessungsunterlagenProperties.getAbsPathTest().isEmpty()) {
                final File directory = new File(vermessungsunterlagenProperties.getAbsPathTest());
                if (directory.exists()) {
                    final File[] executeJobFiles = directory.listFiles(new FilenameFilter() {

                                @Override
                                public boolean accept(final File dir, final String name) {
                                    return name.startsWith("executeJob.") && name.endsWith(".json");
                                }
                            });

                    for (final File executeJobFile : executeJobFiles) {
                        new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    try {
                                        if (LOG.isDebugEnabled()) {
                                            LOG.debug("----");
                                            LOG.debug("Path: " + executeJobFile.getAbsolutePath());
                                        }

                                        final String executeJobContent = IOUtils.toString(
                                                new FileInputStream(executeJobFile));
                                        if (LOG.isDebugEnabled()) {
                                            LOG.debug("Content: " + executeJobContent);
                                        }

                                        final String jobkey = createJob(executeJobContent, true);
                                        LOG.info("Job created: " + jobkey);
                                    } catch (final Exception ex) {
                                        LOG.error(ex, ex);
                                    }
                                }
                            }).start();
                    }
                }
            }
        } catch (final Exception ex) {
            LOG.error(ex, ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public final MetaService getMetaService() {
        return metaService;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public final User getUser() {
        return user;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   serverSearch  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public Collection performSearch(final CidsServerSearch serverSearch) throws SearchException {
        final Map localServers = new HashMap<>();
        localServers.put("WUNDA_BLAU", getMetaService());
        serverSearch.setActiveLocalServers(localServers);
        serverSearch.setUser(getUser());

        return (Collection<MetaObjectNode>)serverSearch.performServerSearch();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   mons  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public Collection<CidsBean> loadBeans(final Collection<MetaObjectNode> mons) throws Exception {
        if (mons != null) {
            if (mons.size() > MAX_BEAN_LOADING) {
                throw new Exception("Zu viele Objekte gefunden. Bitte Fehler melden !");
            }
            final Collection<CidsBean> beans = new ArrayList<>(mons.size());
            for (final MetaObjectNode mon : mons) {
                if (mon != null) {
                    final MetaObject mo = getMetaService().getMetaObject(
                            getUser(),
                            mon.getObjectId(),
                            mon.getClassId(),
                            getConnectionContext());
                    mo.setAllClasses(
                        ((MetaClassCacheService)Lookup.getDefault().lookup(MetaClassCacheService.class)).getAllClasses(
                            mo.getDomain(),
                            getConnectionContext()));
                    beans.add(mo.getBean());
                }
            }
            return beans;
        } else {
            return null;
        }
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
