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
package de.cismet.cids.custom.utils.nas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.impl.MultiPartWriter;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.MediaType;

import de.cismet.cids.server.api.types.Action;
import de.cismet.cids.server.api.types.ActionResultInfo;
import de.cismet.cids.server.api.types.ActionTask;
import de.cismet.cids.server.api.types.CollectionResource;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class CidsActionClient {

    //~ Static fields/initializers ---------------------------------------------

    protected static final String ACTION_URL = "/actions";
//    private static final ApacheHttpClient client = ApacheHttpClient.create();
    private static final Logger LOG = Logger.getLogger(CidsActionClient.class);

    //~ Instance fields --------------------------------------------------------

    protected String domain;
    protected String baseURL;
    private final Client client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String RESOURCE_URL = "/%1$s.%2$s/tasks/%3$s/results/%4$s";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AbstractCidsActionClient object.
     *
     * @param  domain   DOCUMENT ME!
     * @param  baseURL  DOCUMENT ME!
     */
    public CidsActionClient(final String domain, final String baseURL) {
        this.domain = domain;
        this.baseURL = baseURL;
        final ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        clientConfig.getClasses().add(MultiPartWriter.class);
        client = Client.create(clientConfig);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Client getClient() {
        return client;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  user      DOCUMENT ME!
     * @param  password  DOCUMENT ME!
     */
    public void setBasicAuthentication(final String user, final String password) {
        client.addFilter(new HTTPBasicAuthFilter(user, password));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   <T>  DOCUMENT ME!
     * @param   c    DOCUMENT ME!
     * @param   url  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private <T extends Object> T getSimpleResource(final Class<T> c, final String url) {
        final WebResource r = client.resource(url);
        final String jsonResult = r.get(String.class);
        try {
            final T result = mapper.readValue(jsonResult, c);
            return result;
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   <E>  DOCUMENT ME!
     * @param   c    DOCUMENT ME!
     * @param   url  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private <E extends Object> List<E> getCollectionResource(final Class<E> c, final String url) {
        try {
            final String json = client.resource(url)
                        .queryParam("domain", "cids")
                        .accept(MediaType.APPLICATION_JSON)
                        .get(String.class);

            final CollectionResource resource = mapper.readValue(json, CollectionResource.class);
            final String actionJson = mapper.writeValueAsString(resource.get$collection());
            final List<E> result = mapper.readValue(actionJson, new TypeReference<List<E>>() {
                    });

            return result;
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected List<Action> getAllActions() {
        final String url = baseURL + ACTION_URL;
        return getCollectionResource(Action.class, url);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   actionKey  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected Action getAction(final String actionKey) {
        final String formattedUrl = String.format(RESOURCE_URL, domain, actionKey, "", "");
        final String url = baseURL + ACTION_URL
                    + formattedUrl.substring(0, formattedUrl.lastIndexOf("/tasks"));
        return getSimpleResource(Action.class, url);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   actionKey  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected List<ActionTask> getAllRunningTasks(final String actionKey) {
        final String formattedUrl = String.format(RESOURCE_URL, domain, actionKey, "", "");
        final String url = baseURL + ACTION_URL
                    + formattedUrl.substring(0, formattedUrl.lastIndexOf("//results"));
        return getCollectionResource(ActionTask.class, url);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   actionKey  DOCUMENT ME!
     * @param   taskKey    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected ActionTask.Status getTaskStatus(final String actionKey, final String taskKey) {
        final String formattedUrl = String.format(RESOURCE_URL, domain, actionKey, taskKey, "");
        final String url = baseURL + ACTION_URL
                    + formattedUrl.substring(0, formattedUrl.lastIndexOf("/results"));
        final ActionTask status = getSimpleResource(ActionTask.class, url);
        if (status != null) {
            return status.getStatus();
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  actionKey  DOCUMENT ME!
     * @param  taskKey    DOCUMENT ME!
     */
    protected void cancelTask(final String actionKey, final String taskKey) {
        final String formattedUrl = String.format(RESOURCE_URL, domain, actionKey, taskKey, "");
        final String url = baseURL + ACTION_URL
                    + formattedUrl.substring(0, formattedUrl.lastIndexOf("/results"));
        client.resource(url).delete();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   actionKey                 DOCUMENT ME!
     * @param   task                      DOCUMENT ME!
     * @param   f                         DOCUMENT ME!
     * @param   fileType                  DOCUMENT ME!
     * @param   requestResultingInstance  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected ActionTask createTask(final String actionKey,
            final ActionTask task,
            final File f,
            final MediaType fileType,
            final boolean requestResultingInstance) {
        try {
            final String formattedUrl = String.format(RESOURCE_URL, domain, actionKey, "", "");
            final String url = baseURL + ACTION_URL
                        + formattedUrl.substring(0, formattedUrl.lastIndexOf("//results"));
            final WebResource webResource = client.resource(url)
                        .queryParam("requestResultingInstance", Boolean.toString(requestResultingInstance));

            final FormDataMultiPart form = new FormDataMultiPart();
            form.field("taskparams", mapper.writeValueAsString(task), MediaType.APPLICATION_JSON_TYPE);
            if (f != null) {
                form.field("file", f, fileType);
            }
            LOG.error("sending post request to " + url);
            final String responseJson = webResource.type(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .post(String.class, form);

            final ActionTask resultingInstance = mapper.readValue(responseJson, ActionTask.class);

            return resultingInstance;
        } catch (JsonProcessingException ex) {
            LOG.error(ex.getMessage(), ex);
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   <T>        DOCUMENT ME!
     * @param   c          DOCUMENT ME!
     * @param   actionKey  DOCUMENT ME!
     * @param   taskKey    DOCUMENT ME!
     * @param   resultKey  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected <T extends Object> T getTaskResult(final Class<T> c,
            final String actionKey,
            final String taskKey,
            final String resultKey) {
        final String url = baseURL + ACTION_URL
                    + String.format(RESOURCE_URL, domain, actionKey, taskKey, resultKey);
        final WebResource r = client.resource(url);

        return r.get(c);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   actionKey  DOCUMENT ME!
     * @param   taskKey    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected List<ActionResultInfo> getTaskResults(final String actionKey, final String taskKey) {
        final String url = baseURL + ACTION_URL
                    + String.format(RESOURCE_URL, domain, actionKey, taskKey, "")
                    .substring(0, RESOURCE_URL.lastIndexOf("/"));

        return getCollectionResource(ActionResultInfo.class, url);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        final CidsActionClient client = new CidsActionClient("cids", "http://s102x002:8890");
        final ActionTask at = new ActionTask();
        at.setParameters(new HashMap<String, Object>());
        final File f = new File("ci_test_klein.zip");
//        final ActionTask.Status status = client.getTaskStatus("dxf", "1406735852661");
//        System.out.println("Status " + status);
        final ActionTask resultInstance = client.createTask(
                "dummydxf",
                at,
                f,
                new MediaType("application", "zip"),
                true);
        System.out.println("Resulting instance: " + resultInstance.getKey());

        ActionTask.Status status = client.getTaskStatus("dummydxf", resultInstance.getKey());
        while (status != ActionTask.Status.FINISHED) {
            try {
                Thread.sleep(1000);
                status = client.getTaskStatus("dummydxf", resultInstance.getKey());
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        final File tmpFile = client.getTaskResult(File.class,
                "dummydxf",
                resultInstance.getKey(),
                "dxfOutput");
        final File result = new File("result.dxf");
        try {
            IOUtils.copy(new FileInputStream(tmpFile), new FileOutputStream(result));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        System.out.println("Done");
    }
}
