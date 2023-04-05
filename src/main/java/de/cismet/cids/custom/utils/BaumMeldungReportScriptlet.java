/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import com.vividsolutions.jts.geom.Geometry;
import de.cismet.cids.custom.wunda_blau.search.server.BaumChildLightweightSearch;
import de.cismet.cids.dynamics.CidsBean;
import de.cismet.cids.server.search.SearchException;
import de.cismet.connectioncontext.ConnectionContext;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import net.sf.jasperreports.engine.JRDefaultScriptlet;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openide.util.Exceptions;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
public class BaumMeldungReportScriptlet extends JRDefaultScriptlet implements
    MetaServiceStore{

    //~ Static fields/initializers ---------------------------------------------

    protected static final Logger LOG = Logger.getLogger(BaumMeldungReportScriptlet.class);
    /*private static final String QUERY_MELDUNGEN =
        "Select id, abgenommen, datum, bemerkung, arr_ansprechpartner\n" +
        "From baum_meldung\n" +
        "where fk_gebiet = %d";*/
    private static final String CHILD_TOSTRING_TEMPLATE = "%s";
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
    
    private final BaumChildLightweightSearch searchChild = new BaumChildLightweightSearch(
                CHILD_TOSTRING_TEMPLATE,
                CHILD_TOSTRING_FIELDS,
                TABLE_MELDUNG,
                FIELD__GEBIET);
    
    private MetaService metaService;
    private Sirius.server.newuser.User user;
    //~ Methods ----------------------------------------------------------------

    public BaumMeldungReportScriptlet(MetaService metaService, final Sirius.server.newuser.User user) {
        this.metaService = metaService;
        this.user = user;
    }
    
    
    public JRDataSource getChildrenFrom(int idGebiet) throws SearchException {
            searchChild.setParentId(idGebiet);
            searchChild.setFkField(FIELD__GEBIET);
            searchChild.setTable(TABLE_MELDUNG);
            searchChild.setRepresentationFields(CHILD_TOSTRING_FIELDS);
            final Collection<MetaObjectNode> mons;
            Map map = new HashMap();
            map.put("WUNDA_BLAU", metaService);
            searchChild.setActiveLocalServers(map);
            mons = searchChild.performServerSearch();
            final List<CidsBean> beansMeldung = new ArrayList<>();
            if (!mons.isEmpty()) {
                for (final MetaObjectNode mon : mons) {
                    try {
                        MetaObject mo = metaService.getMetaObject(user, mon.getObjectId(), mon.getClassId(), ConnectionContext.createDummy());
                        beansMeldung.add(mo.getBean());
                    } catch (RemoteException e) {
                        LOG.error("Error while retrieving meta object", e);
                    }
                }
            }
                
            final JRBeanCollectionDataSource meldungenDataSource = 
                    new JRBeanCollectionDataSource(beansMeldung);
                   
        return meldungenDataSource;
    }
    
    public JRDataSource getMeldungen(int idGebiet) throws SearchException {
        searchChild.setParentId(idGebiet);
        searchChild.setFkField(FIELD__GEBIET);
        searchChild.setTable(TABLE_MELDUNG);

        return getChildren();
    }
    
    public JRDataSource getSchaeden(int idMeldung) throws SearchException {
        searchChild.setParentId(idMeldung);
        searchChild.setFkField(FIELD__MELDUNG);
        searchChild.setTable(TABLE_SCHADEN);

        return getChildren();
    }
    
    public JRDataSource getAps(int idMeldung) throws SearchException {
            searchChild.setParentId(idMeldung);
            searchChild.setFkField(FIELD__REFERENCE_MELDUNG);
            searchChild.setTable(TABLE_ARRAY);
            searchChild.setRepresentationFields(CHILD_TOSTRING_FIELDS);
            final Collection<MetaObjectNode> mons;
            Map map = new HashMap();
            map.put("WUNDA_BLAU", metaService);
            searchChild.setActiveLocalServers(map);
            mons = searchChild.performServerSearch();
            final List<CidsBean> beansAps = new ArrayList<>();
            if (!mons.isEmpty()) {
                for (final MetaObjectNode mon : mons) {
                    try {
                        MetaObject mo = metaService.getMetaObject(user, mon.getObjectId(), mon.getClassId(), ConnectionContext.createDummy());
                        CidsBean arrayBean = mo.getBean();
                        CidsBean apBean = (CidsBean)arrayBean.getProperty("fk_ansprechpartner");
                        beansAps.add(apBean);
                    } catch (RemoteException e) {
                        LOG.error("Error while retrieving meta object", e);
                    }
                }
            }
                
            final JRBeanCollectionDataSource apsDataSource = 
                    new JRBeanCollectionDataSource(beansAps);

        return apsDataSource;
    }
    
    public List<CidsBean> getChildrenBeans (String fk){
        final Collection<MetaObjectNode> mons;
        Map map = new HashMap();
        map.put("WUNDA_BLAU", metaService);
        searchChild.setActiveLocalServers(map);
        final List<CidsBean> beansChildren = new ArrayList<>();
        try {
            mons = searchChild.performServerSearch();
            if (!mons.isEmpty()) {
                for (final MetaObjectNode mon : mons) {
                    try {
                        MetaObject mo = metaService.getMetaObject(user, mon.getObjectId(), mon.getClassId(), ConnectionContext.createDummy());
                        CidsBean arrayBean = mo.getBean();
                        CidsBean childBean = (CidsBean)arrayBean.getProperty(fk);
                        beansChildren.add(childBean);
                    } catch (RemoteException e) {
                        LOG.error("Error while retrieving meta object", e);
                    }
                }
            }
        } catch (SearchException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return beansChildren;    
    }
    
    public JRDataSource getChildrenOverArray(final String fk) throws SearchException {
        List<CidsBean> beansChildren = getChildrenBeans(fk);      
        final JRBeanCollectionDataSource childrenDataSource = 
                new JRBeanCollectionDataSource(beansChildren);

        return childrenDataSource;
    }
    
    public JRDataSource getOtAps(int id) throws SearchException {
        searchChild.setParentId(id);
        searchChild.setFkField(FIELD__REFERENCE_OT);
        searchChild.setTable(TABLE_OT_AP);
        searchChild.setRepresentationFields(CHILD_TOSTRING_FIELDS);
            
        return getChildrenOverArray(FIELD__AP);
    }
    
    public JRDataSource getApTels(int idAp) throws SearchException {
        searchChild.setParentId(idAp);
        searchChild.setFkField(FIELD__AP);
        searchChild.setTable(TABLE_AP_TEL);
        return getChildren();
    }
    
    public JRDataSource getOts(int idMeldung) throws SearchException {
        searchChild.setParentId(idMeldung);
        searchChild.setFkField(FIELD__MELDUNG);
        searchChild.setTable(TABLE_OT);
        return getChildren();
    }
    
    public JRDataSource getOtTeils(int idOt) throws SearchException {
        searchChild.setParentId(idOt);
        searchChild.setFkField(FIELD__OT);
        searchChild.setTable(TABLE_OT_TEIL);
        return getChildren();
    }
    
    public JRDataSource getChildren() throws SearchException {
            searchChild.setRepresentationFields(CHILD_TOSTRING_FIELDS);
            final Collection<MetaObjectNode> mons;
            Map map = new HashMap();
            map.put("WUNDA_BLAU", metaService);
            searchChild.setActiveLocalServers(map);
            mons = searchChild.performServerSearch();
            final List<CidsBean> beansChild = new ArrayList<>();
            if (!mons.isEmpty()) {
                for (final MetaObjectNode mon : mons) {
                    try {
                        MetaObject mo = metaService.getMetaObject(user, mon.getObjectId(), mon.getClassId(), ConnectionContext.createDummy());
                        beansChild.add(mo.getBean());
                    } catch (RemoteException e) {
                        LOG.error("Error while retrieving meta object", e);
                    }
                }
            }
                
            final JRBeanCollectionDataSource childDataSource = 
                    new JRBeanCollectionDataSource(beansChild);

        return childDataSource;
    }
    
    public String concatenatedKrone (int idSchaden){
        searchChild.setParentId(idSchaden);
        searchChild.setFkField(FIELD__REFERENCE_S);
        searchChild.setTable(TABLE_S_KRONE);
        return getConcatenatedArray(FIELD__KRONE);
    }
    
    public String concatenatedStamm (int idSchaden){
        searchChild.setParentId(idSchaden);
        searchChild.setFkField(FIELD__REFERENCE_S);
        searchChild.setTable(TABLE_S_STAMM);
        return getConcatenatedArray(FIELD__STAMM);
    }
    
    public String concatenatedWurzel (int idSchaden){
        searchChild.setParentId(idSchaden);
        searchChild.setFkField(FIELD__REFERENCE_S);
        searchChild.setTable(TABLE_S_WURZEL);
        return getConcatenatedArray(FIELD__WURZEL);
    }
    
    public String concatenatedMassnahme (int idSchaden){
        searchChild.setParentId(idSchaden);
        searchChild.setFkField(FIELD__REFERENCE_S);
        searchChild.setTable(TABLE_S_MASS); 
        return (getConcatenatedArray(FIELD__MASS).equals("") ? "keine":getConcatenatedArray(FIELD__MASS));
    }
    
    public String getConcatenatedArray(String fk){
        List<CidsBean> beansChildren = getChildrenBeans(fk);
        List <String> listChildren = new ArrayList<>();
        String children = "";
        if (beansChildren != null){
            for (final CidsBean childBean : beansChildren){
                listChildren.add(childBean.getProperty(FIELD__NAME).toString());
            }
            if (listChildren != null){
                children = 
                        listChildren.isEmpty() ? "" : (String.join(", ", listChildren));
            }
        }
        return children;
    }
  /*  
    private static BufferedImage generateMap(final CidsBean gebietBean, final boolean isDgk) {
        try {
            final String mapUrl = BaumConfProperties.getInstance().getUrlDefault();
            Geometry geom = null;
            final Collection<Feature> features = new ArrayList<>();
                final Geometry flaecheGeom = (Geometry)gebietBean.getProperty("fk_geom.geo_field");
                if (flaecheGeom != null) {
                    final StyledFeature dsf = new DefaultStyledFeature();
                    dsf.setGeometry(flaecheGeom);
                    dsf.setFillingPaint(FEATURE_COLOR_GEBIET);
                    dsf.setTransparency(0.5f);
                    features.add(dsf);
                    if (geom == null) {
                        geom = (Geometry)flaecheGeom.buffer(0).clone();
                    } else {
                        geom = geom.union((Geometry)flaecheGeom.buffer(0).clone());
                    }
                }

            final int margin = 50;
            if (geom != null) {
                final XBoundingBox boundingBox = new XBoundingBox(geom);
                boundingBox.increase(10);
                boundingBox.setX1(boundingBox.getX1() - margin);
                boundingBox.setY1(boundingBox.getY1() - margin);
                boundingBox.setX2(boundingBox.getX2() + margin);
                boundingBox.setY2(boundingBox.getY2() + margin);

                final HeadlessMapProvider mapProvider = new HeadlessMapProvider();
                mapProvider.setCenterMapOnResize(true);
                mapProvider.setBoundingBox(boundingBox);
                final SimpleWmsGetMapUrl getMapUrl = new SimpleWmsGetMapUrl(mapUrl);
                final SimpleWMS simpleWms = new SimpleWMS(getMapUrl);
                mapProvider.addLayer(simpleWms);

                for (final Feature feature : features) {
                    mapProvider.addFeature(feature);
                }

                return (BufferedImage)mapProvider.getImageAndWait(
                        72,
                        BaumConfProperties.getInstance().getGebietMapDpi(),
                        BaumConfProperties.getInstance().getGebietMapWidth(),
                        BaumConfProperties.getInstance().getGebietMapHeight());
            } else {
                return null;
            }
        } catch (IllegalArgumentException | InterruptedException | ExecutionException e) {
            LOG.error("Error while retrieving map", e);
            return null;
        }
    }
*/

    @Override
    public void setMetaService(MetaService ms) {
        metaService = ms;
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }
}
