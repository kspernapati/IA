package com.emc.ecd.spt.restfulsamples;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestfullIA implements Runnable {
    final static Logger logger = LoggerFactory.getLogger(RestfullIA.class);

    /**
     * Restful Java Class sample for InfoArchive 4.
     * 
     * @version 1.0
     * @author EMEA SPT
     * @version 1.1
     * @author Derek Zasiewski
     * @comments: Updated to work with IA4 GA Candidate
     */

    public static volatile String accessToken = null;
    public static volatile Client client = null;

    public static volatile String serverIp = null;

    public static volatile String clientPort = null;
    public static volatile String restAPIPort = null;
    public static volatile String userName = null;
    public static volatile String password = null;
    public static volatile String appName = null;
    public static volatile String sipDirPath = null;

    public static String getApplicationApiURI(String appsURI, String applicationName) throws Exception {
        logger.info("Looking for application URL ...");
        WebTarget target = client.target(appsURI).path("");

        Invocation.Builder invocationBuilder = target.request(new String[] {MediaType.APPLICATION_JSON_TYPE.toString()});
        invocationBuilder.header("Authorization", "Bearer " + accessToken);

        Response response = invocationBuilder.get();

        final JSONObject obj = new JSONObject(response.readEntity(String.class));

        final JSONObject data = (JSONObject)obj.get("_embedded");

        JSONObject links = null;

        final JSONArray apps = data.getJSONArray("applications");
        final int n = apps.length();
        for (int i = 0; i < n; ++i) {
            final JSONObject app = apps.getJSONObject(i);
            if (app.getString("name").equals(applicationName)) {
                links = app.getJSONObject("_links");
                break;

            }
        }

        return links.getJSONObject("http://identifiers.emc.com/aips").get("href").toString();
    }

    public static String receiveAip(String aipURL, File input) throws IOException, JSONException {
        WebTarget target = client.target(aipURL).path("").register(MultiPartFeature.class);
        Invocation.Builder invocationBuilder = target.request(new String[] {MediaType.APPLICATION_JSON_TYPE.toString()});
        invocationBuilder.header("Authorization", "Bearer " + accessToken);

        //File input = new File("C:/dev/infoarchive/tools/applications/sapmifPC/sips/eas_archive_documentum_32-SAPMIF-2016-Q1-2016-09-09-BSb_1.zip");
        final FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
        formDataMultiPart.getBodyParts().add(new FileDataBodyPart("sip", input));
        FormDataMultiPart multipart = (FormDataMultiPart)formDataMultiPart.field("format", "eas_sip_zip");
        logger.info("Receive started ..." + aipURL);
        Response response = invocationBuilder.post(Entity.entity(multipart, MediaType.MULTIPART_FORM_DATA_TYPE));
        final JSONObject obj = new JSONObject(response.readEntity(String.class));

        final JSONObject data = (JSONObject)obj.get("_links");

        formDataMultiPart.close();
        multipart.close();
        return data.getJSONObject("http://identifiers.emc.com/ingest").get("href").toString();
    }

    public static ArrayList<String> getAipURL(String appAIPURL) throws IOException, JSONException {
        WebTarget target = client.target(appAIPURL).path("");

        Invocation.Builder invocationBuilder = target.request(new String[] {"application/hal+json"});
        invocationBuilder.header("Authorization", "Bearer " + accessToken);
        Response response = invocationBuilder.get();

        final JSONObject obj = new JSONObject(response.readEntity(String.class));

        ArrayList<String> links = new ArrayList<String>();

        final JSONObject data = (JSONObject)obj.get("_embedded");
        final JSONArray apps = data.getJSONArray("aips");
        final int n = apps.length();
        for (int i = 0; i < n; ++i) {
            final JSONObject app = apps.getJSONObject(i);
            links.add(app.getJSONObject("_links").getJSONObject("self").get("href").toString());
        }

        return links;
    }

    public static String getInvalidateURLs(String aipURL) throws IOException, JSONException {
        logger.info("Action..." + aipURL);
        WebTarget target = client.target(aipURL).path("");
        Invocation.Builder invocationBuilder = target.request(new String[] {"application/hal+json"});
        invocationBuilder.header("Authorization", "Bearer " + accessToken);
        Response response = invocationBuilder.get();

        final JSONObject obj = new JSONObject(response.readEntity(String.class));

        final JSONObject data = (JSONObject)obj.get("_links");
        if (!(obj.get("stateCode").toString().equals("INV_WPROC"))) {
            return data.getJSONObject("http://identifiers.emc.com/invalid").get("href").toString();
        }
        logger.info("NoAction..." + aipURL);
        return "NoAction";
    }

    public static void InvalidateAIP(String aipURL) throws IOException, JSONException {
        logger.info("Invalidate AIP..." + aipURL);
        WebTarget target = client.target(aipURL).path("");
        Invocation.Builder invocationBuilder = target.request(new String[] {"application/hal+json"});
        invocationBuilder.header("Authorization", "Bearer " + accessToken);
        Response response = invocationBuilder.put(Entity.json("Invalidate"));

        final JSONObject obj = new JSONObject(response.readEntity(String.class));
    }

    public static void ingest(String ingestURL) throws IOException, JSONException {
        logger.info("Ingest started URL .." + ingestURL);
        WebTarget target = client.target(ingestURL).path("");

        Invocation.Builder invocationBuilder = target.request(new String[] {"application/hal+json"});
        invocationBuilder.header("Authorization", "Bearer " + accessToken);

        Response response = invocationBuilder.put(Entity.json("Ingestion"));
        final JSONObject obj = new JSONObject(response.readEntity(String.class));

        final String data = ((JSONObject)obj.get("_links")).getJSONObject("self").get("href").toString();
        logger.info("AIP Successfully Ingested" + data);
    }

    public static String getTenantApplicationURI() throws Exception {
        logger.info("Looking for an InfoArchive tenant ...");

        WebTarget target = client.target("http://" + serverIp + ":" + restAPIPort + "/systemdata/tenants").path("");
        WebTarget targetQry = target.queryParam("grant_type", new Object[] {"password"});

        Invocation.Builder invocationBuilder = targetQry.request(new String[] {MediaType.APPLICATION_JSON_TYPE.toString()});
        invocationBuilder.header("Authorization", "Bearer " + accessToken);

        Response response = invocationBuilder.get();
        final JSONObject obj = new JSONObject(response.readEntity(String.class));
        final JSONObject data = (JSONObject)obj.get("_embedded");

        JSONObject links = null;
        final JSONArray tenants = data.getJSONArray("tenants");
        final int n = tenants.length();
        for (int i = 0; i < n; ++i) {
            final JSONObject tenant = tenants.getJSONObject(i);
            log("Tenant selected is '" + tenant.getString("name") + "'");
            links = tenant.getJSONObject("_links");
        }

        logger.info("Looking for applications URL ...");
        return links.getJSONObject("http://identifiers.emc.com/applications").get("href").toString();
    }

    public static String getAccessToken() throws Exception {
        logger.info("Getting access token ... ");

        WebTarget target = client.target("http://" + serverIp + ":" + clientPort + "/login").path("");
        //oauth/token
        //client_secret param depends on the configuration of spring
        WebTarget targetQry = target.queryParam("grant_type", new Object[] {"password"}).queryParam("username", new Object[] {userName})
            .queryParam("password", new Object[] {password})
            //                                                                                                          .queryParam("client_id", new Object[] { "infoarchive.iawa" })
            //                                                                                                          .queryParam( "client_secret", new Object[] { "secret" })
            .queryParam("scope", new Object[] {"search compliance administration"});

        Invocation.Builder invocationBuilder = targetQry.request(new String[] {MediaType.APPLICATION_JSON_TYPE.toString()});
        invocationBuilder.header("Authorization", "Basic aW5mb2FyY2hpdmUuaWF3YTpzZWNyZXQ=");

        Response response = invocationBuilder.post(null);
        final JSONObject obj = new JSONObject(response.readEntity(String.class));

        logger.info("Access token is: " + obj.getString("access_token"));
        logger.info("Refresh token is: " + obj.getString("refresh_token"));
        logger.info("JTI identifier is: " + obj.getString("jti"));

        return obj.getString("access_token");
    }

    public static void getInstanceInformation() throws Exception {
        logger.info("Retrieving InfoArchive instance product information ...");

        WebTarget target = client.target("http://" + serverIp + ":" + restAPIPort + "/product-info").path("");
        Invocation.Builder invocationBuilder = target.request(new String[] {MediaType.APPLICATION_JSON_TYPE.toString()});
        invocationBuilder.header("Authorization", "Bearer " + accessToken);

        Response response = invocationBuilder.get();
        final JSONObject obj = new JSONObject(response.readEntity(String.class));
        final JSONObject props = (JSONObject)obj.get("buildProperties");

        logger.info("InfoArchive " + props.getString("ia.server.version.label") + " has been detected ... ");
        logger.info("Extracting additional instance information ... ");

        Iterator<?> iterator = props.keys();
        while (iterator.hasNext()) {
            String obj2 = iterator.next().toString();
            if (!(props.get(obj2) instanceof JSONArray)) {
                if (!(props.get(obj2) instanceof JSONObject)) {
                    logger.info(" - " + obj2 + ": " + props.getString(obj2));
                }
            }
        }
    }

    public static void log(String text) {
        System.out.println("(" + System.currentTimeMillis() + "m.) => " + text);
    }

    public void run(){
        try {
            //Get config paramaters to connect to InfoArchive
            ResourceBundle inputStream = ResourceBundle.getBundle("config");
            serverIp = inputStream.getString("serverip");
            clientPort = inputStream.getString("client_port");
            restAPIPort = inputStream.getString("restapiproxy_port");
            userName = inputStream.getString("username");
            password = inputStream.getString("password");
            appName = inputStream.getString("appname");
            sipDirPath = inputStream.getString("sipDir");
            String renameFile = inputStream.getString("renameFiles");
            String match = inputStream.getString("match");
            String sub = inputStream.getString("substitution");
            String str;
            
            logger.info("Welcome to the Restful Java Class for InfoArchive 4");
            logger.info("Creating Jersey client instance ...");

            File sipPath = new File(sipDirPath);
          
            Collection<File> fileColl = FileUtils.listFiles(sipPath, FileFilterUtils.suffixFileFilter("zip"), null);
            ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue( fileColl);
            queue.addAll(fileColl);
           
            logger.info("Queue size......"+queue.size());
            logger.info("Total files to ingest..." + fileColl.size());
            if (renameFile.equalsIgnoreCase("true")) {
                logger.info("Renaming started...");
                renameFiles(fileColl, match, sub);
            }

            Iterator<File> fileItr = queue.iterator();
            int i = 0;
            while (fileItr.hasNext()) {
                client = ClientBuilder.newClient();

                //Retrieve access token from oauth authentication engine for the user indicated in the properties file
                //accessToken = getAccessToken();
                accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjM2MDU2NDM3NDYsInVzZXJfbmFtZSI6InN1ZUBpYWN1c3RvbWVyLmNvbSIsImF1dGhvcml0aWVzIjpbIkdST1VQX1NVUEVSIiwiUk9MRV9BRE1JTklTVFJBVE9SIiwiUk9MRV9CVVNJTkVTU19PV05FUiIsIlJPTEVfREVWRUxPUEVSIiwiUk9MRV9FTkRfVVNFUiIsIlJPTEVfSVRfT1dORVIiLCJST0xFX1JFVEVOVElPTl9NQU5BR0VSIl0sImp0aSI6IjZiMTM0YzVlLThkZmItNDdmNC1iYWExLTQ1ZDBiNmFhM2U4YiIsImNsaWVudF9pZCI6ImluZm9hcmNoaXZlLmlhd2EiLCJzY29wZSI6WyJhZG1pbmlzdHJhdGlvbiIsImNvbXBsaWFuY2UiLCJzZWFyY2giXX0.A6INoAQpQQrWYAosPlQz9DL9xV-jPwXt3Z6XfkmoPus";
                
                //Retrieve IA instance runtime information 
                //getInstanceInformation();

                /**
                 * Get existing tenants and the URL pointer to the first Tenant
                 * found http://identifiers.emc.com/applications
                 */
                String applicationsURL = getTenantApplicationURI();
                log("Applications URL for Tenant is " + applicationsURL);

                String apiURL = getApplicationApiURI(applicationsURL, appName);
                logger.info("API URL for Application " + appName + " is " + apiURL);
                //getInvalidateURLs(aipInvalidateURL.get(1).toString());

                File file =  fileItr.next();
                logger.info("Iteration values are --->"+file);
                logger.info("File processing..." + ++i + "..." + file.getName());
                String ingestURL = receiveAip(apiURL, file);
                
                ingest(ingestURL);
                
                Thread.currentThread().sleep(10000);

                client.close();
                logger.info("Connection closed...");
            }

            logger.info("Job is done.....");

            /**
             * ArrayList<String> aipInvalidateURL = getAipURL(apiURL); for (int
             * i = 0; i < aipInvalidateURL.size(); i++) { log(">>>>>>>>>>" + i);
             * String invalidateURL =
             * getInvalidateURLs(aipInvalidateURL.get(i).toString()); if
             * (!(invalidateURL.equalsIgnoreCase("NoAction")))
             * InvalidateAIP(invalidateURL); } //String ingestURL =
             * receiveAip(apiURL); //System.out.println(ingestURL);
             * //ingest(ingestURL);
             **/
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    public static void renameFiles(Collection fileColl, String match, String sub) {
        Iterator<File> fileItr = fileColl.iterator();
        while (fileItr.hasNext()) {
            File file = fileItr.next();
            logger.info("File name changed.." + file.renameTo(new File((file.getAbsolutePath().replaceAll(match, sub)))));
        }
    }

}
