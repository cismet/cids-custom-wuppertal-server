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
import Sirius.server.newuser.User;

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

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Body {

        //~ Enum constants -----------------------------------------------------

        FIND_GRENZNIEDERSCHRIFT, GET_GRENZNIEDERSCHRIFT_FILENAME, GET_GRENZNIEDERSCHRIFT_LINK_FILENAME,
        FIND_VERMESSUNGSRISS, GET_VERMESSUNGSRISS_FILENAME, GET_VERMESSUNGSRISS_LINK_FILENAME, FIND_BUCHWERK,
        GET_BUCHWERK_FILENAME, FIND_INSELKARTE, GET_INSELKARTE_FILENAME, FIND_GEWANNE, GET_GEWANNE_FILENAME,
        FIND_GEBAEUDEBESCHREIBUNG, GET_GEBAEUDEBESCHREIBUNG_FILENAME
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Param {

        //~ Enum constants -----------------------------------------------------

        SCHLUESSEL, GEMARKUNG, FLUR, BLATT, STEUERBEZIRK, BEZEICHNER, HISTORISCH, LINK, VERSION, KMQUADRAT, ORDNER,
        NUMMER
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
        String ordner = null;
        Integer nummer = null;
        if (params != null) {
            for (final ServerActionParameter sap : params) {
                if (sap.getKey().equals(Param.SCHLUESSEL.toString())) {
                    schluessel = (String)sap.getValue();
                } else if (sap.getKey().equals(Param.GEMARKUNG.toString())) {
                    gemarkung = (Integer)sap.getValue();
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
                } else if (sap.getKey().equals(Param.ORDNER.toString())) {
                    ordner = (String)sap.getValue();
                } else if (sap.getKey().equals(Param.NUMMER.toString())) {
                    nummer = (Integer)sap.getValue();
                }
            }
        }
        if (request != null) {
            switch (request) {
                case FIND_VERMESSUNGSRISS: {
                    return finder.findVermessungsrissPicture(schluessel, gemarkung, flur, blatt);
                }
                case GET_VERMESSUNGSRISS_FILENAME: {
                    return finder.getVermessungsrissFilename(schluessel, gemarkung, flur, blatt);
                }
                case GET_VERMESSUNGSRISS_LINK_FILENAME: {
                    return finder.getVermessungsrissLinkFilename(link);
                }
                case FIND_GRENZNIEDERSCHRIFT: {
                    return finder.findGrenzniederschriftPicture(schluessel, gemarkung, flur, blatt);
                }
                case GET_GRENZNIEDERSCHRIFT_FILENAME: {
                    return finder.getGrenzniederschriftFilename(schluessel, gemarkung, flur, blatt);
                }
                case GET_GRENZNIEDERSCHRIFT_LINK_FILENAME: {
                    return finder.getGrenzniederschriftLinkFilename(link);
                }
                case FIND_BUCHWERK: {
                    return finder.findBuchwerkPicture(
                            schluessel,
                            gemarkungBean,
                            steuerbezirk,
                            bezeichner,
                            historisch);
                }
                case GET_BUCHWERK_FILENAME: {
                    return finder.getBuchwerkFilename(
                            schluessel,
                            gemarkungBean,
                            steuerbezirk,
                            bezeichner,
                            historisch);
                }
                case FIND_GEWANNE: {
                    final boolean liste = gemarkungBean != null;
                    return finder.findGewannenPicture(
                            gemarkungBean,
                            kmquadrat,
                            liste);
                }
                case GET_GEWANNE_FILENAME: {
                    final boolean liste = gemarkungBean != null;
                    return finder.getGewannenFilename(
                            gemarkungBean,
                            kmquadrat,
                            liste);
                }
                case FIND_INSELKARTE: {
                    return finder.findInselkartePicture(
                            schluessel,
                            gemarkungBean,
                            flur,
                            blatt,
                            version);
                }
                case GET_INSELKARTE_FILENAME: {
                    return finder.getInselkarteFilename(
                            schluessel,
                            gemarkungBean,
                            flur,
                            blatt,
                            version);
                }
                case FIND_GEBAEUDEBESCHREIBUNG: {
                    return finder.findGebaeudebeschreibungPicture(ordner, nummer);
                }
                case GET_GEBAEUDEBESCHREIBUNG_FILENAME: {
                    return finder.getGebaeudebeschreibungFilename(ordner, nummer);
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
