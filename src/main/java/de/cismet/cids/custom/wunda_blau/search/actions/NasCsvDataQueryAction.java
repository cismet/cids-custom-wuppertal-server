/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.newuser.User;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.vividsolutions.jts.geom.GeometryCollection;

import java.io.InputStream;
import java.io.StringReader;

import java.net.URL;

import java.util.HashMap;

import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;
import de.cismet.cids.custom.utils.nas.NasProduct;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionHelper;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UploadableInputStream;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.commons.security.AccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class NasCsvDataQueryAction implements UserAwareServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            NasCsvDataQueryAction.class);
    public static final String TASKNAME = "nasCsvDataQuery";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum METHOD_TYPE {

        //~ Enum constants -----------------------------------------------------

        CREATE, GET
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        TEMPLATE, GEOMETRY_COLLECTION, METHOD
    }

    //~ Instance fields --------------------------------------------------------

    private User user;

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        NasProduct nasProduct = null;
        GeometryCollection geoms = null;
        METHOD_TYPE method = null;

        for (final ServerActionParameter sap : params) {
            if (sap.getKey().equals(PARAMETER_TYPE.TEMPLATE.toString())) {
                nasProduct = (NasProduct)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.GEOMETRY_COLLECTION.toString())) {
                geoms = (GeometryCollection)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.METHOD.toString())) {
                method = (METHOD_TYPE)sap.getValue();
            }
        }

        if ((method == METHOD_TYPE.CREATE) && (nasProduct != null)) {
            try {
                final NasGetObjekteServerAction action = new NasGetObjekteServerAction();
                final ServerActionParameter[] parameter = new ServerActionParameter[2];

                parameter[0] = new ServerActionParameter(NasGetObjekteServerAction.Parameter.SEARCH_TYPE.name(),
                        NasGetObjekteServerAction.NasSearchType.FLURSTUECKE);
                parameter[1] = new ServerActionParameter(NasGetObjekteServerAction.Parameter.GEOMETRY.name(), geoms);

                final Object landparcels = action.execute(null, parameter);

                if (landparcels instanceof String) {
                    final URL url = new URL(nasProduct.getServer());
                    String para = nasProduct.getTemplateContent().replace("%landparcels%", (String)landparcels);
                    para += ServerAlkisProducts.getInstance().getIdentification();
                    final HashMap<String, String> requestHeaders = new HashMap<String, String>();
                    requestHeaders.put("content-type", "application/x-www-form-urlencoded");

                    final InputStream is =
                        new SimpleHttpAccessHandler().doRequest(
                            url,
                            new StringReader(para),
                            "application/x-www-form-urlencoded",
                            AccessHandler.ACCESS_METHODS.POST_REQUEST,
                            requestHeaders,
                            null);
                    final ObjectMapper mapper = new ObjectMapper(new JsonFactory());
                    final JsonNode node = mapper.readTree(is);

                    final JsonNode id = node.get("id");

                    if (id != null) {
                        final String orderId = id.asText();
                        final URL getUrl = ServerAlkisProducts.getInstance()
                                    .getCsvGetResultURL(nasProduct.getServer(), orderId);
                        final InputStream resultIs = new SimpleHttpAccessHandler().doRequest(getUrl);
                        return ServerActionHelper.asyncByteArrayHelper(new UploadableInputStream(resultIs), orderId);
                    } else {
                        throw new Exception("no order id retrieved");
                    }
                }
            } catch (Exception e) {
                LOG.error("Error during NAS Request " + nasProduct.key, e);
            }
        }

        return null;
    }

    @Override
    public String getTaskName() {
        return TASKNAME;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }
}
