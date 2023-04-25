/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import de.cismet.cids.custom.wunda_blau.search.server.BaumChildLightweightSearch;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.SearchException;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextProvider;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
public class BaumMeldungReportScriptlet extends JRDefaultScriptlet implements ConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    protected static final Logger LOG = Logger.getLogger(BaumMeldungReportScriptlet.class);
    private static final String CHILD_TOSTRING_TEMPLATE = "%s";
    private static final String TABLE_GEBIET = "baum_gebiet";
    private static final String TABLE_MELDUNG = "baum_meldung";
    private static final String TABLE_SCHADEN = "baum_schaden";
    private static final String TABLE_OT = "baum_ortstermin";
    private static final String TABLE_OT_TEIL = "baum_teilnehmer";
    private static final String TABLE_ARRAY = "baum_meldung_ansprechpartner";
    private static final String TABLE_OT_AP = "baum_ortstermin_ansprechpartner";
    private static final String TABLE_AP_TEL = "baum_telefon";
    private static final String TABLE_S_KRONE = "baum_schaden_krone";
    private static final String TABLE_S_STAMM = "baum_schaden_stamm";
    private static final String TABLE_S_WURZEL = "baum_schaden_wurzel";
    private static final String TABLE_S_MASS = "baum_schaden_massnahme";
    private static final String TABLE_FEST = "baum_festsetzung";
    private static final String TABLE_ERSATZ = "baum_ersatz";
    private static final String TABLE_ERSATZBAUM = "baum_ersatzbaum";
    private static final String TABLE_KONTROLLE = "baum_kontrolle";
    private static final String[] CHILD_TOSTRING_FIELDS = { "id" };
    private static final String FIELD__REFERENCE_MELDUNG = "baum_meldung_reference";
    private static final String FIELD__MELDUNG = "fk_meldung";
    private static final String FIELD__OT = "fk_ortstermin";
    private static final String FIELD__REFERENCE_OT = "baum_ortstermin_reference";
    private static final String FIELD__REFERENCE_S = "baum_schaden_reference";
    private static final String FIELD__GEBIET = "fk_gebiet";
    private static final String FIELD__AP = "fk_ansprechpartner";
    private static final String FIELD__NAME = "name";
    private static final String FIELD__KRONE = "fk_krone";
    private static final String FIELD__STAMM = "fk_stamm";
    private static final String FIELD__WURZEL = "fk_wurzel";
    private static final String FIELD__MASS = "fk_massnahme";
    private static final String FIELD__SCHADEN = "fk_schaden";
    private static final String FIELD__ERSATZ = "fk_ersatz";
    private static final String FIELD__GEOM = "fk_geom";
    private static final String FIELD__GEOM_GEO_FIELD = "fk_geom.geo_field";

    private static final int DEFAULT_MAP_DPI = 300;
    private static final int DEFAULT_BUFFER = 50;
    private static final int DEFAULT_MAP_WIDTH = 300;
    private static final int DEFAULT_MAP_HEIGHT = 200;
    private static final String DEFAULT__SRS = "EPSG:25832";
    private static final double DEFAULT__HOME_X1 = 6.7d;
    private static final double DEFAULT__HOME_Y1 = 49.1d;
    private static final double DEFAULT__HOME_X2 = 7.1d;
    private static final double DEFAULT__HOME_Y2 = 49.33d;
    private static final String DEFAULT_COLOR = "#888888";
    private static final String BAUM_MAP_IMAGE_FACTORY = "de.cismet.cids.custom.reports.wunda_blau.BaumMapImageFactory";

    //~ Instance fields --------------------------------------------------------

    private final BaumChildLightweightSearch searchChild = new BaumChildLightweightSearch(
            CHILD_TOSTRING_TEMPLATE,
            CHILD_TOSTRING_FIELDS,
            TABLE_MELDUNG,
            FIELD__GEBIET);

    private final MetaService metaService;
    private final Sirius.server.newuser.User user;

    private final Map<Integer, MetaObjectNode> beansSchadenMap = new HashMap<>();
    private final Map<Integer, MetaObjectNode> beansErsatzMap = new HashMap<>();
    private final Map<Integer, MetaObjectNode> beansFestMap = new HashMap<>();
    private final Map<Integer, MetaObjectNode> beansBaumMap = new HashMap<>();

    private final ConnectionContext connectionContext;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaumMeldungReportScriptlet object.
     *
     * @param  metaService        DOCUMENT ME!
     * @param  user               DOCUMENT ME!
     * @param  connectionContext  DOCUMENT ME!
     */
    public BaumMeldungReportScriptlet(final MetaService metaService,
            final Sirius.server.newuser.User user,
            final ConnectionContext connectionContext) {
        this.metaService = metaService;
        this.user = user;
        this.connectionContext = connectionContext;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   idGebiet  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getChildrenFrom(final int idGebiet) throws SearchException {
        searchChild.setParentId(idGebiet);
        searchChild.setFkField(FIELD__GEBIET);
        searchChild.setTable(TABLE_MELDUNG);
        searchChild.setRepresentationFields(CHILD_TOSTRING_FIELDS);
        final Collection<MetaObjectNode> mons;
        final Map map = new HashMap();
        map.put("WUNDA_BLAU", metaService);
        searchChild.setActiveLocalServers(map);
        mons = searchChild.performServerSearch();
        final List<CidsBean> beansMeldung = new ArrayList<>();
        if (!mons.isEmpty()) {
            mons.forEach((mon) -> {
                try {
                    final MetaObject mo = metaService.getMetaObject(
                            user,
                            mon.getObjectId(),
                            mon.getClassId(),
                            getConnectionContext());
                    beansMeldung.add(mo.getBean());
                } catch (RemoteException e) {
                    LOG.error("Error while retrieving meta object", e);
                }
            });
        }

        final JRBeanCollectionDataSource meldungenDataSource = new JRBeanCollectionDataSource(beansMeldung);

        return meldungenDataSource;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idGebiet  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getMeldungen(final int idGebiet) throws SearchException {
        searchChild.setParentId(idGebiet);
        searchChild.setFkField(FIELD__GEBIET);
        searchChild.setTable(TABLE_MELDUNG);

        return getChildren();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idMeldung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getSchaeden(final int idMeldung) throws SearchException {
        searchChild.setParentId(idMeldung);
        searchChild.setFkField(FIELD__MELDUNG);
        searchChild.setTable(TABLE_SCHADEN);

        return getChildren();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idSchaden  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getFestsetzungen(final int idSchaden) throws SearchException {
        searchChild.setParentId(idSchaden);
        searchChild.setFkField(FIELD__SCHADEN);
        searchChild.setTable(TABLE_FEST);

        return getChildren();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idSchaden  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getErsatzpflanzungen(final int idSchaden) throws SearchException {
        searchChild.setParentId(idSchaden);
        searchChild.setFkField(FIELD__SCHADEN);
        searchChild.setTable(TABLE_ERSATZ);

        return getChildren();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idErsatz  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getErsatzbaeume(final int idErsatz) throws SearchException {
        searchChild.setParentId(idErsatz);
        searchChild.setFkField(FIELD__ERSATZ);
        searchChild.setTable(TABLE_ERSATZBAUM);

        return getChildren();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idErsatz  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getKontrollen(final int idErsatz) throws SearchException {
        searchChild.setParentId(idErsatz);
        searchChild.setFkField(FIELD__ERSATZ);
        searchChild.setTable(TABLE_KONTROLLE);

        return getChildren();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idMeldung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getAps(final int idMeldung) throws SearchException {
        searchChild.setParentId(idMeldung);
        searchChild.setFkField(FIELD__REFERENCE_MELDUNG);
        searchChild.setTable(TABLE_ARRAY);
        searchChild.setRepresentationFields(CHILD_TOSTRING_FIELDS);
        final Collection<MetaObjectNode> mons;
        final Map map = new HashMap();
        map.put("WUNDA_BLAU", metaService);
        searchChild.setActiveLocalServers(map);
        mons = searchChild.performServerSearch();
        final List<CidsBean> beansAps = new ArrayList<>();
        if (!mons.isEmpty()) {
            mons.forEach((mon) -> {
                try {
                    final MetaObject mo = metaService.getMetaObject(
                            user,
                            mon.getObjectId(),
                            mon.getClassId(),
                            getConnectionContext());
                    final CidsBean arrayBean = mo.getBean();
                    final CidsBean apBean = (CidsBean)arrayBean.getProperty(FIELD__AP);
                    beansAps.add(apBean);
                } catch (RemoteException e) {
                    LOG.error("Error while retrieving meta object", e);
                }
            });
        }

        final JRBeanCollectionDataSource apsDataSource = new JRBeanCollectionDataSource(beansAps);

        return apsDataSource;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fk  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public List<CidsBean> getChildrenBeans(final String fk) {
        final Collection<MetaObjectNode> mons;
        final Map map = new HashMap();
        map.put("WUNDA_BLAU", metaService);
        searchChild.setActiveLocalServers(map);
        final List<CidsBean> beansChildren = new ArrayList<>();
        try {
            mons = searchChild.performServerSearch();
            if (!mons.isEmpty()) {
                mons.forEach((mon) -> {
                    try {
                        final MetaObject mo = metaService.getMetaObject(
                                user,
                                mon.getObjectId(),
                                mon.getClassId(),
                                getConnectionContext());
                        final CidsBean arrayBean = mo.getBean();
                        final CidsBean childBean = (CidsBean)arrayBean.getProperty(fk);
                        beansChildren.add(childBean);
                    } catch (RemoteException e) {
                        LOG.error("Error while retrieving meta object", e);
                    }
                });
            }
        } catch (SearchException ex) {
            Exceptions.printStackTrace(ex);
        }

        return beansChildren;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fk  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getChildrenOverArray(final String fk) throws SearchException {
        final List<CidsBean> beansChildren = getChildrenBeans(fk);
        final JRBeanCollectionDataSource childrenDataSource = new JRBeanCollectionDataSource(beansChildren);

        return childrenDataSource;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   id  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getOtAps(final int id) throws SearchException {
        searchChild.setParentId(id);
        searchChild.setFkField(FIELD__REFERENCE_OT);
        searchChild.setTable(TABLE_OT_AP);
        searchChild.setRepresentationFields(CHILD_TOSTRING_FIELDS);

        return getChildrenOverArray(FIELD__AP);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idAp  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getApTels(final int idAp) throws SearchException {
        searchChild.setParentId(idAp);
        searchChild.setFkField(FIELD__AP);
        searchChild.setTable(TABLE_AP_TEL);
        return getChildren();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idMeldung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getOts(final int idMeldung) throws SearchException {
        searchChild.setParentId(idMeldung);
        searchChild.setFkField(FIELD__MELDUNG);
        searchChild.setTable(TABLE_OT);
        return getChildren();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idOt  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getOtTeils(final int idOt) throws SearchException {
        searchChild.setParentId(idOt);
        searchChild.setFkField(FIELD__OT);
        searchChild.setTable(TABLE_OT_TEIL);
        return getChildren();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    public JRDataSource getChildren() throws SearchException {
        searchChild.setRepresentationFields(CHILD_TOSTRING_FIELDS);
        final Collection<MetaObjectNode> mons;
        final Map map = new HashMap();
        map.put("WUNDA_BLAU", metaService);
        searchChild.setActiveLocalServers(map);
        mons = searchChild.performServerSearch();
        final List<CidsBean> beansChild = new ArrayList<>();
        if (!mons.isEmpty()) {
            for (final MetaObjectNode mon : mons) {
                try {
                    final MetaObject mo = metaService.getMetaObject(
                            user,
                            mon.getObjectId(),
                            mon.getClassId(),
                            getConnectionContext());
                    final CidsBean bean = mo.getBean();
                    beansChild.add(bean);
                    switch (mo.getMetaClass().getName()) {
                        case TABLE_SCHADEN: {
                            beansSchadenMap.put(mon.getObjectId(), mon);
                            break;
                        }
                        case TABLE_ERSATZ: {
                            beansErsatzMap.put(mon.getObjectId(), mon);
                            break;
                        }
                        case TABLE_FEST: {
                            beansFestMap.put(mon.getObjectId(), mon);
                            break;
                        }
                        case TABLE_ERSATZBAUM: {
                            beansBaumMap.put(mon.getObjectId(), mon);
                            break;
                        }
                    }
                } catch (RemoteException e) {
                    LOG.error("Error while retrieving meta object", e);
                }
            }
        }

        final JRBeanCollectionDataSource childDataSource = new JRBeanCollectionDataSource(beansChild);

        return childDataSource;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   id  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public MetaObjectNode getSchadenBean(final Integer id) {
        return beansSchadenMap.get(id);
    }
    /**
     * DOCUMENT ME!
     *
     * @param   id  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public MetaObjectNode getErsatzBean(final Integer id) {
        return beansErsatzMap.get(id);
    }
    /**
     * DOCUMENT ME!
     *
     * @param   id  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public MetaObjectNode getFestBean(final Integer id) {
        return beansFestMap.get(id);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   id  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public MetaObjectNode getBaumBean(final Integer id) {
        return beansBaumMap.get(id);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idSchaden  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String concatenatedKrone(final int idSchaden) {
        searchChild.setParentId(idSchaden);
        searchChild.setFkField(FIELD__REFERENCE_S);
        searchChild.setTable(TABLE_S_KRONE);
        return getConcatenatedArray(FIELD__KRONE);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idSchaden  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String concatenatedStamm(final int idSchaden) {
        searchChild.setParentId(idSchaden);
        searchChild.setFkField(FIELD__REFERENCE_S);
        searchChild.setTable(TABLE_S_STAMM);
        return getConcatenatedArray(FIELD__STAMM);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idSchaden  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String concatenatedWurzel(final int idSchaden) {
        searchChild.setParentId(idSchaden);
        searchChild.setFkField(FIELD__REFERENCE_S);
        searchChild.setTable(TABLE_S_WURZEL);
        return getConcatenatedArray(FIELD__WURZEL);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   idSchaden  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String concatenatedMassnahme(final int idSchaden) {
        searchChild.setParentId(idSchaden);
        searchChild.setFkField(FIELD__REFERENCE_S);
        searchChild.setTable(TABLE_S_MASS);
        return (getConcatenatedArray(FIELD__MASS).equals("") ? "keine" : getConcatenatedArray(FIELD__MASS));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fk  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getConcatenatedArray(final String fk) {
        final List<CidsBean> beansChildren = getChildrenBeans(fk);
        final List<String> listChildren = new ArrayList<>();
        String children = "";
        if (beansChildren != null) {
            beansChildren.forEach((childBean) -> { listChildren.add(childBean.getProperty(FIELD__NAME).toString()); });
            children = listChildren.isEmpty() ? "" : (String.join(", ", listChildren));
        }
        return children;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   mons    DOCUMENT ME!
     * @param   buffer  DOCUMENT ME!
     * @param   width   DOCUMENT ME!
     * @param   height  DOCUMENT ME!
     * @param   dpi     DOCUMENT ME!
     * @param   url     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public BufferedImage generateMap(
            final Collection<BaumMapImageFactoryConfiguration.ObjectIdentifier> mons,
            final double buffer,
            final Integer width,
            final Integer height,
            final Integer dpi,
            final String url) throws Exception {
        final BaumProperties properties = BaumProperties.getInstance();
        final BaumMapImageFactoryConfiguration config = new BaumMapImageFactoryConfiguration();
        config.setMons(mons);
        config.setWidth(width);
        config.setHeight(height);
        config.setBuffer(buffer);
        config.setMapDpi(dpi);
        config.setMapUrl(url);
        config.setSrs((properties != null) ? properties.getMapSrs() : DEFAULT__SRS);
        config.setBbX1(DEFAULT__HOME_X1);
        config.setBbY1(DEFAULT__HOME_Y1);
        config.setBbX2(DEFAULT__HOME_X2);
        config.setBbY2(DEFAULT__HOME_Y2);

        final Map<String, String> colorMap = new HashMap<>();
        colorMap.put(
            TABLE_GEBIET,
            ((properties != null) && (properties.getGebietColor() != null)) ? properties.getGebietColor()
                                                                            : DEFAULT_COLOR);
        colorMap.put(
            TABLE_SCHADEN,
            ((properties != null) && (properties.getSchadenColor() != null)) ? properties.getSchadenColor()
                                                                             : DEFAULT_COLOR);
        colorMap.put(
            TABLE_ERSATZ,
            ((properties != null) && (properties.getErsatzColor() != null)) ? properties.getErsatzColor()
                                                                            : DEFAULT_COLOR);
        colorMap.put(
            TABLE_ERSATZBAUM,
            ((properties != null) && (properties.getBaumColor() != null)) ? properties.getBaumColor() : DEFAULT_COLOR);
        colorMap.put(
            TABLE_FEST,
            ((properties != null) && (properties.getFestColor() != null)) ? properties.getFestColor() : DEFAULT_COLOR);
        config.setColorMap(colorMap);

        final byte[] bytes = ByteArrayFactoryHandler.getInstance()
                    .execute(
                        BAUM_MAP_IMAGE_FACTORY,
                        new ObjectMapper().writeValueAsString(config),
                        getUser(),
                        getConnectionContext());
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   gebietMon  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public BufferedImage generateGebietMap(final MetaObjectNode gebietMon) throws Exception {
        final BaumProperties properties = BaumProperties.getInstance();
        final Collection<BaumMapImageFactoryConfiguration.ObjectIdentifier> mons = Arrays.asList(
                new BaumMapImageFactoryConfiguration.ObjectIdentifier(
                    gebietMon.getObjectId(),
                    gebietMon.getClassId()));
        final Integer width = ((properties != null) ? properties.getGebietMapWidth() : DEFAULT_MAP_WIDTH);
        final Integer height = ((properties != null) ? properties.getGebietMapHeight() : DEFAULT_MAP_HEIGHT);
        final double buffer = ((properties != null) ? properties.getGebietMapBuffer() : DEFAULT_BUFFER);
        final Integer dpi = ((properties != null) ? properties.getGebietMapDpi() : DEFAULT_MAP_DPI);
        final String url = ((properties != null) ? properties.getUrlDefault() : null);
        return generateMap(mons, buffer, width, height, dpi, url);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   schadenMon  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public BufferedImage generateSchadenMap(final MetaObjectNode schadenMon) throws Exception {
        final BaumProperties properties = BaumProperties.getInstance();
        final Collection<BaumMapImageFactoryConfiguration.ObjectIdentifier> mons = Arrays.asList(
                new BaumMapImageFactoryConfiguration.ObjectIdentifier(
                    schadenMon.getObjectId(),
                    schadenMon.getClassId()));
        final Integer width = ((properties != null) ? properties.getSchadenMapWidth() : DEFAULT_MAP_WIDTH);
        final Integer height = ((properties != null) ? properties.getSchadenMapHeight() : DEFAULT_MAP_HEIGHT);
        final double buffer = ((properties != null) ? properties.getSchadenMapBuffer() : DEFAULT_BUFFER);
        final Integer dpi = ((properties != null) ? properties.getSchadenMapDpi() : DEFAULT_MAP_DPI);
        final String url = ((properties != null) ? properties.getUrlSchaden() : null);
        return generateMap(mons, buffer, width, height, dpi, url);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   ersatzMon  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public boolean isErsatzGeom(final MetaObjectNode ersatzMon) throws Exception {
        try {
            final MetaObject mo = metaService.getMetaObject(
                    user,
                    ersatzMon.getObjectId(),
                    ersatzMon.getClassId(),
                    getConnectionContext());
            if ((mo.getBean() != null) && (mo.getBean().getProperty(FIELD__GEOM) != null)
                        && (mo.getBean().getProperty(FIELD__GEOM_GEO_FIELD) != null)) {
                return true;
            }
        } catch (RemoteException e) {
            LOG.error("Error while retrieving meta object", e);
        }
        return false;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   ersatzMon  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public BufferedImage generateErsatzMap(final MetaObjectNode ersatzMon) throws Exception {
        final BaumProperties properties = BaumProperties.getInstance();
        final MetaObject mo;
        try {
            mo = metaService.getMetaObject(
                    user,
                    ersatzMon.getObjectId(),
                    ersatzMon.getClassId(),
                    getConnectionContext());
            if ((mo.getBean() == null) || (mo.getBean().getProperty(FIELD__GEOM_GEO_FIELD) == null)) {
                return null;
            }
            final int ersatzId = mo.getBean().getPrimaryKeyValue();
            getErsatzbaeume(ersatzId);
            final Collection<BaumMapImageFactoryConfiguration.ObjectIdentifier> mons = new ArrayList<>();
            mons.add(new BaumMapImageFactoryConfiguration.ObjectIdentifier(
                    ersatzMon.getObjectId(),
                    ersatzMon.getClassId()));
            MetaObject moBaum;
            for (final MetaObjectNode monBaum : beansBaumMap.values()) {
                moBaum = metaService.getMetaObject(
                        user,
                        monBaum.getObjectId(),
                        monBaum.getClassId(),
                        getConnectionContext());
                if (((CidsBean)moBaum.getBean().getProperty(FIELD__ERSATZ)).getPrimaryKeyValue() == ersatzId) {
                    mons.add(new BaumMapImageFactoryConfiguration.ObjectIdentifier(
                            monBaum.getObjectId(),
                            monBaum.getClassId()));
                }
            }
            final Integer width = ((properties != null) ? properties.getErsatzMapWidth() : DEFAULT_MAP_WIDTH);
            final Integer height = ((properties != null) ? properties.getErsatzMapHeight() : DEFAULT_MAP_HEIGHT);
            final double buffer = ((properties != null) ? properties.getErsatzMapBuffer() : DEFAULT_BUFFER);
            final Integer dpi = ((properties != null) ? properties.getErsatzMapDpi() : DEFAULT_MAP_DPI);
            final String url = ((properties != null) ? properties.getUrlDefault() : null);
            return generateMap(mons, buffer, width, height, dpi, url);
        } catch (RemoteException e) {
            LOG.error("Error while retrieving meta object", e);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   festMon  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public BufferedImage generateFestMap(final MetaObjectNode festMon) throws Exception {
        final BaumProperties properties = BaumProperties.getInstance();
        final Collection<BaumMapImageFactoryConfiguration.ObjectIdentifier> mons = Arrays.asList(
                new BaumMapImageFactoryConfiguration.ObjectIdentifier(
                    festMon.getObjectId(),
                    festMon.getClassId()));
        final Integer width = ((properties != null) ? properties.getFestMapWidth() : DEFAULT_MAP_WIDTH);
        final Integer height = ((properties != null) ? properties.getFestMapHeight() : DEFAULT_MAP_HEIGHT);
        final double buffer = ((properties != null) ? properties.getFestMapBuffer() : DEFAULT_BUFFER);
        final Integer dpi = ((properties != null) ? properties.getFestMapDpi() : DEFAULT_MAP_DPI);
        final String url = ((properties != null) ? properties.getUrlFestsetzung() : null);
        return generateMap(mons, buffer, width, height, dpi, url);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public MetaService getMetaService() {
        return metaService;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public User getUser() {
        return user;
    }
}
