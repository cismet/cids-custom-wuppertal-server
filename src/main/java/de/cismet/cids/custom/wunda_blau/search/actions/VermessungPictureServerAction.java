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
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.MetaClassCache;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import java.rmi.RemoteException;

import java.util.HashMap;
import java.util.Map;

import de.cismet.cids.custom.utils.alkis.VermessungPictureFinder;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class VermessungPictureServerAction implements UserAwareServerAction, MetaServiceStore, ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    public static String TASK_NAME = "VermessungPicture";

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            VermessungPictureServerAction.class);

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Body {

        //~ Enum constants -----------------------------------------------------

        FIND, BASENAME, LINK
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Type {

        //~ Enum constants -----------------------------------------------------

        VERMESSUNGSRISS, GRENZNIEDERSCHRIFT, BUCHWERK, INSELKARTE, GEWANNE, GEBAEUDEBESCHREIBUNG
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Param {

        //~ Enum constants -----------------------------------------------------

        TYPE, SCHLUESSEL, GEMARKUNG, FLUR, BLATT, STEUERBEZIRK, BEZEICHNER, HISTORISCH, LINK, VERSION, KMQUADRAT, MON
    }

    //~ Instance fields --------------------------------------------------------

    private User user;
    private MetaService metaService;

    private final Map<Integer, CidsBean> gemarkungMap = new HashMap<>();

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }

    @Override
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final VermessungPictureFinder finder = new VermessungPictureFinder(
                getUser(),
                getMetaService(),
                getConnectionContext());
        final Body request;
        if (body instanceof String) {
            request = Body.valueOf((String)body);
        } else if (body instanceof Body) {
            request = (Body)body;
        } else {
            request = null;
        }

        Type type = null;
        MetaObjectNode mon = null;
        String schluessel = null;
        Integer gemarkung = null;
        CidsBean gemarkungBean = null;
        String flur = null;
        String blatt = null;
        Integer steuerbezirk = null;
        String bezeichner = null;
        Boolean historisch = null;
        String link = null;
        String version = null;
        Integer kmquadrat = null;
        if (params != null) {
            for (final ServerActionParameter sap : params) {
                if (sap.getKey().equals(Param.SCHLUESSEL.toString())) {
                    schluessel = (String)sap.getValue();
                } else if (sap.getKey().equals(Param.TYPE.toString())) {
                    type = (Type)sap.getValue();
                } else if (sap.getKey().equals(Param.GEMARKUNG.toString())) {
                    gemarkung = (Integer)sap.getValue();
                } else if (sap.getKey().equals(Param.FLUR.toString())) {
                    flur = (String)sap.getValue();
                } else if (sap.getKey().equals(Param.BLATT.toString())) {
                    blatt = (String)sap.getValue();
                } else if (sap.getKey().equals(Param.STEUERBEZIRK.toString())) {
                    steuerbezirk = (Integer)sap.getValue();
                } else if (sap.getKey().equals(Param.BEZEICHNER.toString())) {
                    bezeichner = (String)sap.getValue();
                } else if (sap.getKey().equals(Param.HISTORISCH.toString())) {
                    historisch = (Boolean)sap.getValue();
                } else if (sap.getKey().equals(Param.LINK.toString())) {
                    link = (String)sap.getValue();
                } else if (sap.getKey().equals(Param.VERSION.toString())) {
                    version = (String)sap.getValue();
                } else if (sap.getKey().equals(Param.KMQUADRAT.toString())) {
                    kmquadrat = (Integer)sap.getValue();
                } else if (sap.getKey().equals(Param.MON.toString())) {
                    mon = (MetaObjectNode)sap.getValue();
                }
            }
        }
        if (gemarkung != null) {
            if (gemarkungMap.containsKey(gemarkung)) {
                gemarkungBean = gemarkungMap.get(gemarkung);
            } else {
                try {
                    final MetaClass mc = MetaClassCache.getInstance()
                                .getMetaClass("WUNDA_BLAU", "vermessung_gemarkung");
                    final MetaObject mo = getMetaService().getMetaObject(
                            getUser(),
                            gemarkung,
                            mc.getId(),
                            getConnectionContext());
                    gemarkungBean = mo.getBean();
                    gemarkungMap.put(gemarkung, gemarkungBean);
                } catch (final Exception ex) {
                }
            }
        }
        final CidsBean bean;
        if (mon != null) {
            MetaObject mo = null;
            try {
                mo = getMetaService().getMetaObject(
                        getUser(),
                        mon.getObjectId(),
                        mon.getClassId(),
                        getConnectionContext());
            } catch (RemoteException ex) {
                LOG.error(ex, ex);
            }
            bean = (mo != null) ? mo.getBean() : null;
        } else {
            bean = null;
        }

        if ((request != null) && (type != null)) {
            switch (request) {
                case FIND: {
                    switch (type) {
                        case VERMESSUNGSRISS: {
                            return finder.findVermessungsrissPicture(schluessel, gemarkung, flur, blatt);
                        }
                        case GRENZNIEDERSCHRIFT: {
                            return finder.findGrenzniederschriftPicture(schluessel, gemarkung, flur, blatt);
                        }
                        case BUCHWERK: {
                            return finder.findBuchwerkPicture(
                                    schluessel,
                                    gemarkungBean,
                                    steuerbezirk,
                                    bezeichner,
                                    historisch);
                        }
                        case INSELKARTE: {
                            return finder.findInselkartePicture(
                                    schluessel,
                                    gemarkungBean,
                                    flur,
                                    blatt,
                                    version);
                        }
                        case GEWANNE: {
                            return finder.findGewannenPicture(
                                    gemarkungBean,
                                    kmquadrat,
                                    gemarkungBean
                                            != null);
                        }
                        case GEBAEUDEBESCHREIBUNG: {
                            if (bean != null) {
                                final String ordner = (String)bean.getProperty("ordner");
                                final String nummer = (String)bean.getProperty("nummer");
                                return finder.findGebaeudebeschreibungenPicture(ordner, nummer);
                            } else {
                                return null;
                            }
                        }
                    }
                }
                case BASENAME: {
                    switch (type) {
                        case VERMESSUNGSRISS: {
                            return finder.getVermessungsrissFilename(schluessel, gemarkung, flur, blatt);
                        }
                        case GRENZNIEDERSCHRIFT: {
                            return finder.getGrenzniederschriftFilename(schluessel, gemarkung, flur, blatt);
                        }
                        case BUCHWERK: {
                            return finder.getBuchwerkFilename(
                                    schluessel,
                                    gemarkungBean,
                                    steuerbezirk,
                                    bezeichner,
                                    historisch);
                        }
                        case INSELKARTE: {
                            return finder.getInselkarteFilename(
                                    schluessel,
                                    gemarkungBean,
                                    flur,
                                    blatt,
                                    version);
                        }
                        case GEWANNE: {
                            return finder.getGewannenFilename(
                                    gemarkungBean,
                                    kmquadrat,
                                    gemarkungBean
                                            != null);
                        }
                        case GEBAEUDEBESCHREIBUNG: {
                            if (bean != null) {
                                final String ordner = (String)bean.getProperty("ordner");
                                final String nummer = (String)bean.getProperty("nummer");
                                return finder.getGebaeudebeschreibungenFilename(ordner, nummer);
                            } else {
                                return null;
                            }
                        }
                    }
                }
                case LINK: {
                    switch (type) {
                        case VERMESSUNGSRISS: {
                            return finder.getVermessungsrissLinkFilename(link);
                        }
                        case GRENZNIEDERSCHRIFT: {
                            return finder.getGrenzniederschriftLinkFilename(link);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
