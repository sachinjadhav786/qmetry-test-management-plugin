package com.qmetry;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.net.ProtocolException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.commons.httpclient.auth.InvalidCredentialsException;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class QTMApiConnection {

    private String url;
    private String key;

    public QTMApiConnection(String url, String key) {
        this.url = url;
        this.key = key;
    }

    public String getUrl() {
        return this.url;
    }

    public String getKey() {
        return this.key;
    }

    public boolean validateConnection() {
        // TODO validate URL,Key
        return true;
    }

    public String uploadFileToTestSuite(String filePath, String testSuiteName, String automationFramework,
            String buildName, String platformName)
            throws InvalidCredentialsException, ProtocolException, IOException, QMetryException {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("entityType", automationFramework, ContentType.TEXT_PLAIN);
			if(testSuiteName!=null && !testSuiteName.isEmpty())
				builder.addTextBody("testsuiteId", testSuiteName, ContentType.TEXT_PLAIN);
            if(buildName!=null && !buildName.isEmpty())
				builder.addTextBody("buildID", buildName, ContentType.TEXT_PLAIN);
            if(platformName!=null && !platformName.isEmpty())
				builder.addTextBody("platformID", platformName, ContentType.TEXT_PLAIN);

            File f = new File(filePath);
            builder.addPart("file", new FileBody(f));
            HttpEntity multipart = builder.build();

            HttpPost uploadFile = new HttpPost(getUrl() + "/rest/import/createandscheduletestresults/1");
            uploadFile.addHeader("accept", "application/json");
            uploadFile.addHeader("scope", "default");
            uploadFile.addHeader("apiKey", getKey());
            uploadFile.setEntity(multipart);

            httpClient = HttpClients.createDefault();
            response = httpClient.execute(uploadFile);
            String respEntityStr = EntityUtils.toString(response.getEntity());
			System.out.println("QMetry Test Management Plugin : Response : " + respEntityStr);
            if (!(response.getStatusLine().getStatusCode() == 200)) 
			{
                throw new QMetryException("Error uploading file to server!");
            }
            return respEntityStr;
        } catch (Exception e) {
            throw new QMetryException("Could not upload file '" + filePath + "'");
        } finally {
            try {
                httpClient.close();
                response.close();
            } catch (Exception e) {
            }
        }
    }
}