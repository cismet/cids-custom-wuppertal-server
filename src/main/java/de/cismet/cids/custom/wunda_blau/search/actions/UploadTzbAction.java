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

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.newuser.User;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

import java.awt.Image;
import java.awt.image.BufferedImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.sql.Timestamp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.cids.utils.serverresources.JsonServerResource;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;
import de.cismet.cids.utils.serverresources.TextServerResource;

import de.cismet.commons.security.WebDavClient;
import de.cismet.commons.security.WebDavHelper;

import de.cismet.connectioncontext.AbstractConnectionContext;
import de.cismet.connectioncontext.ConnectionContext;

import de.cismet.netutil.ProxyHandler;

import de.cismet.tools.PasswordEncrypter;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class UploadTzbAction implements ServerAction, UserAwareServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(UploadTzbAction.class);
    private static final String DOMAIN = "WUNDA_BLAU";
    private static final ConnectionContext CC = ConnectionContext.create(
            AbstractConnectionContext.Category.ACTION,
            "UploadTzbAction");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final String ACTION_QUERY = "select %1s, a.id from tzb_action a where key = '%2s'";
    private static final String TREE_QUERY = "select %1s, t.id from tzb_tree t where id = %2s";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ParameterType {

        //~ Enum constants -----------------------------------------------------

        key, status, payload, created_at, action_time, description, status_reason, fk_tree, url
    }

    //~ Instance fields --------------------------------------------------------

    protected final Map<String, Object> paramsHashMap = new HashMap<>();
    private User usr = null;

    //~ Methods ----------------------------------------------------------------

    @Override
    public User getUser() {
        return usr;
    }

    @Override
    public void setUser(final User user) {
        this.usr = user;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        paramsHashMap.clear();
        for (final ServerActionParameter param : params) {
            final String key = param.getKey().toLowerCase();
            final Object value = param.getValue();
            if ((value instanceof String) || (value == null)) {
                final String singleValue = (String)value;
                paramsHashMap.put(key, singleValue);
            } else if (value instanceof Object) {
                paramsHashMap.put(key, value);
            }
        }

        try {
            final CidsBean actionBean = CidsBean.createNewCidsBeanFromTableName(
                    DOMAIN,
                    "tzb_tree_action",
                    CC);
            final MetaObject o = DomainServerImpl.getServerInstance().getMetaObject(getUser(), 1000, 877, CC);
//            o.getBean().getProperty(ParameterType.action_time.toString());
            final Object actionTimeAsObject = paramsHashMap.get(ParameterType.action_time.toString());
            final Object createdTimeAsObject = paramsHashMap.get(ParameterType.created_at.toString());
            final Object actionAsObject = paramsHashMap.get(ParameterType.key.toString());
            final Object treeAsObject = paramsHashMap.get(ParameterType.fk_tree.toString());
            Timestamp actionTime = null;
            Timestamp createdTime = null;

            if (createdTimeAsObject instanceof String) {
                createdTime = new Timestamp(sdf.parse((String)createdTimeAsObject).getTime());
            }

            if (actionTimeAsObject instanceof String) {
                actionTime = new Timestamp(sdf.parse((String)actionTimeAsObject).getTime());
            }

            CidsBean fkActionBean = null;
            CidsBean fkTreeBean = null;

            if (actionAsObject instanceof String) {
                final MetaClass mcTzbAction = DomainServerImpl.getServerInstance()
                            .getClassByTableName(getUser(), "tzb_action", CC);
                final MetaObject[] actions = DomainServerImpl.getServerInstance()
                            .getMetaObject(
                                getUser(),
                                String.format(ACTION_QUERY, mcTzbAction.getID(), actionAsObject),
                                CC);

                if ((actions != null) && (actions.length == 1)) {
                    fkActionBean = actions[0].getBean();
                } else {
                    LOG.error("Cannot find action '" + actionAsObject + "'");
                }
            }

            if (treeAsObject instanceof String) {
                final MetaClass mcTzbTree = DomainServerImpl.getServerInstance()
                            .getClassByTableName(getUser(), "tzb_tree", CC);
                final MetaObject[] trees = DomainServerImpl.getServerInstance()
                            .getMetaObject(getUser(), String.format(ACTION_QUERY, mcTzbTree.getID(), treeAsObject), CC);

                if ((trees != null) && (trees.length == 1)) {
                    fkTreeBean = trees[0].getBean();
                } else {
                    LOG.error("Cannot find action '" + treeAsObject + "'");
                }
            }

            actionBean.setProperty(ParameterType.action_time.toString(), actionTime);
            actionBean.setProperty(ParameterType.created_at.toString(), createdTime);
            actionBean.setProperty(ParameterType.status.toString(), paramsHashMap.get(ParameterType.status.toString()));
            actionBean.setProperty(ParameterType.status_reason.toString(),
                paramsHashMap.get(ParameterType.status_reason.toString()));
            actionBean.setProperty(ParameterType.fk_tree.toString(), fkTreeBean);
            actionBean.setProperty("payload", "{\"test\": \"Ein Test\"}");
            actionBean.setProperty("fk_action", fkActionBean);
            final Map<String, Object> payload = (Map<String, Object>)paramsHashMap.get(ParameterType.payload
                            .toString());

            final Object picture = payload.get("pic");

            if (picture instanceof String) {
                final String imageData = (String)picture;

                if (imageData != null) {
                    try {
                        final String documentUrl = writeImage(imageData);

                        payload.put("pic", documentUrl);
                    } catch (final Exception ex) {
                        LOG.fatal(ex, ex);
                    }
                }

                actionBean.setProperty(ParameterType.payload.toString(),
                    (new ObjectMapper()).writeValueAsString(payload));
            }

            final MetaObject mo = DomainServerImpl.getServerInstance()
                        .insertMetaObject(getUser(), actionBean.getMetaObject(), CC);
            return mo.getBean();
        } catch (Exception e) {
            LOG.error("Cannot create tzb_tree_action cide bean^");
        }
        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  key    DOCUMENT ME!
     * @param  value  DOCUMENT ME!
     */
    protected void addParam(final String key, final Object value) {
        paramsHashMap.put(key, value);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   key    DOCUMENT ME!
     * @param   clazz  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected Object getParam(final String key, final Class clazz) {
        final Collection values = getListParam(key, clazz);
        if ((values == null) || values.isEmpty()) {
            return null;
        } else {
            return values.iterator().next();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   key    DOCUMENT ME!
     * @param   clazz  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  UnsupportedOperationException  DOCUMENT ME!
     */
    protected Collection getListParam(final String key, final Class clazz) {
        final Collection objects = new ArrayList();
        if (paramsHashMap.containsKey(key.toLowerCase())) {
            for (final Object val : (List)paramsHashMap.get(key.toLowerCase())) {
                Object object = null;

                if (val instanceof String) {
                    final String value = (String)val;

                    if (Date.class.equals(clazz)) {
                        final long timestamp = Long.parseLong(value);
                        object = new Date(timestamp);
                    } else if (java.sql.Date.class.equals(clazz)) {
                        final long timestamp = Long.parseLong(value);
                        object = new java.sql.Date(timestamp);
                    } else if (Timestamp.class.equals(clazz)) {
                        final long timestamp = Long.parseLong(value);
                        object = new Timestamp(timestamp);
                    } else if (Integer.class.equals(clazz)) {
                        object = Integer.parseInt(value);
                    } else if (Float.class.equals(clazz)) {
                        object = Float.parseFloat(value);
                    } else if (Long.class.equals(clazz)) {
                        object = Long.parseLong(value);
                    } else if (Double.class.equals(clazz)) {
                        object = Double.parseDouble(value);
                    } else if (Boolean.class.equals(clazz)) {
                        if ("ja".equals(value.toLowerCase())) {
                            object = true;
                        } else if ("nein".equals(value.toLowerCase())) {
                            object = false;
                        } else {
                            throw new UnsupportedOperationException("wrong boolean value");
                        }
                    } else if (String.class.equals(clazz)) {
                        object = value;
                    } else {
                        throw new UnsupportedOperationException("this class is not supported");
                    }
                } else {
                    if (val == null) {
                        object = null;
                    } else if (ArrayList.class.equals(clazz)) {
                        object = val;
                    }
                }

                objects.add(object);
            }
        } else {
            return objects;
        }
        return objects;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   imageData  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static String writeImage(
            final String imageData) throws Exception {
        final FileOutputStream fos = null;
        try {
            final String ending = "jpg";
            final UploadConfig config = ServerResourcesLoader.getInstance()
                        .loadJson((JsonServerResource)WundaBlauServerResources.TZB_WEBDAV.getValue(),
                            UploadConfig.class);

            final String webDavRoot = config.getUrl();
            final String webDavPath = (config.getPath().endsWith("/") ? config.getPath() : (config.getPath() + "/"));
            final String webFileName = createFileName(ending);

            final File tempFile = uploadToWebDav(
                    webDavRoot,
                    config.getUser(),
                    config.getPasswd(),
                    imageData,
                    null,
                    webDavPath
                            + webFileName,
                    ending);

            if (ending.equals("jpg") || ending.equals("png")) {
                final byte[] bytes = createThumbnail(tempFile, ending);

                uploadToWebDav(
                    webDavRoot,
                    config.getUser(),
                    config.getPasswd(),
                    imageData,
                    bytes,
                    webDavPath
                            + webFileName
                            + ".thumbnail."
                            + ending,
                    ending);
            }

            return webDavRoot + webDavPath + webFileName;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                LOG.fatal(ex, ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   ending  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String createFileName(final String ending) {
        final Random rand = new Random();
        return "IMG-" + rand.nextInt() + "_" + System.currentTimeMillis() + "." + ending;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   webDavRoot    DOCUMENT ME!
     * @param   user          DOCUMENT ME!
     * @param   passwd        DOCUMENT ME!
     * @param   imageData     DOCUMENT ME!
     * @param   imageAsBytes  DOCUMENT ME!
     * @param   webFileName   DOCUMENT ME!
     * @param   ending        DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private static File uploadToWebDav(final String webDavRoot,
            final String user,
            final String passwd,
            final String imageData,
            final byte[] imageAsBytes,
            final String webFileName,
            final String ending) throws Exception {
        final File tempFile = File.createTempFile("file", "." + ending);
        FileOutputStream fos = null;
        WebDavClient webDavClient = null;
        byte[] imageDataAsByteA = imageAsBytes;

        if (imageDataAsByteA == null) {
            imageDataAsByteA = convertFileDataToBytes(imageData);
        }

        try {
            fos = new FileOutputStream(tempFile);
            fos.write(imageDataAsByteA);
            fos.close();

            if (webDavClient == null) {
                String pass = passwd;

                if ((pass != null) && pass.startsWith(PasswordEncrypter.CRYPT_PREFIX)) {
                    pass = PasswordEncrypter.decryptString(passwd);
                }

                webDavClient = new WebDavClient(ProxyHandler.getInstance().getProxy(), user, pass);
            }

            final int httpStatusCode = WebDavHelper.uploadFileToWebDAV(
                    webFileName,
                    tempFile,
                    webDavRoot,
                    webDavClient,
                    null);

            if ((int)(httpStatusCode / 100) != 2) {
                throw new Exception("Cannot upload image. Status code = " + httpStatusCode);
            }
        } finally {
            if (fos != null) {
                fos.close();
            }
        }

        return tempFile;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   tempFile  DOCUMENT ME!
     * @param   ending    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private static byte[] createThumbnail(final File tempFile, final String ending) throws Exception {
        final Image img = ImageIO.read(tempFile);
        final int height = img.getHeight(null);
        final int width = img.getWidth(null);
        final int longestSide = Math.max(width, height);
        double scale = 1;

        // set longest side to 600 if it is longer
        if (longestSide > 600) {
            scale = 600.0 / longestSide;
        }

        final BufferedImage imgThumb = new BufferedImage((int)(width * scale),
                (int)(height * scale),
                BufferedImage.TYPE_INT_RGB);

        imgThumb.createGraphics()
                .drawImage(img.getScaledInstance((int)(width * scale), (int)(height * scale), Image.SCALE_SMOOTH),
                    0,
                    0,
                    null);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(imgThumb, ending, os);

        return os.toByteArray();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   imageData  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static byte[] convertFileDataToBytes(final String imageData) {
        String base64String;

        if (imageData.indexOf("base64,") != -1) {
            base64String = imageData.substring(imageData.indexOf("base64,") + "base64,".length());
        } else {
            base64String = imageData;
        }

        final Base64.Decoder decoder = Base64.getDecoder();

        return decoder.decode(base64String);
    }

    @Override
    public String getTaskName() {
        return "uploadTzbTreeAction";
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    private static class UploadConfig {

        //~ Instance fields ----------------------------------------------------

        String url;
        String path;
        String user;
        String passwd;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new UploadConfig object.
         */
        public UploadConfig() {
        }
    }
}
