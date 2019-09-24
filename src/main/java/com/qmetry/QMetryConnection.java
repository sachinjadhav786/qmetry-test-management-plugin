package com.qmetry;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.config.RequestConfig;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
//import hudson.model.BuildListener;
import hudson.model.TaskListener;

public class QMetryConnection {

    private String url;
    private String key;

    public QMetryConnection(String url, String key) {
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
    
    public void uploadFileToTestSuite(String filePath, String testSuiteName, String testSName, String automationFramework, String automationHierarchy,
            String buildName, String platformName, String project, String release, String cycle, String pluginName, /*BuildListener*/TaskListener listener, int buildnumber, String proxyUrl, String testCaseField, String testSuiteField)
            throws QMetryException, IOException {
		//try
		//{
			CloseableHttpClient httpClient = null;
			CloseableHttpResponse response = null;
			
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			
			listener.getLogger().println(pluginName + " : uploading result file(s) of type '"+automationFramework+"'");
			builder.addTextBody("entityType", automationFramework, ContentType.TEXT_PLAIN);
			if(automationHierarchy!=null && !automationHierarchy.isEmpty())
			{
				if(automationFramework.equals("TESTNG") || automationFramework.equals("JUNIT"))
				{
					listener.getLogger().println(pluginName + " : automation hierarchy '" + automationHierarchy + "'");
					builder.addTextBody("automationHierarchy", automationHierarchy, ContentType.TEXT_PLAIN);
				}
			}
			
			if(testSuiteName!=null && !testSuiteName.isEmpty()) {
				listener.getLogger().println(pluginName + " : target test suite id '"+testSuiteName+"'");
				builder.addTextBody("testsuiteId", testSuiteName, ContentType.TEXT_PLAIN);
			}
			if(testSName!=null && !testSName.isEmpty())
			{
				listener.getLogger().println(pluginName + " : test suite name '"+testSName+"'");
				builder.addTextBody("testsuiteName", testSName + "_#" + buildnumber, ContentType.TEXT_PLAIN);
			}
			if(buildName!=null && !buildName.isEmpty()) {
				listener.getLogger().println(pluginName + " : using build '"+buildName+"'");
				builder.addTextBody("buildID", buildName, ContentType.TEXT_PLAIN);
			}
			if(platformName!=null && !platformName.isEmpty()) {
				listener.getLogger().println(pluginName + " : target platform '"+platformName+"'");
				builder.addTextBody("platformID", platformName, ContentType.TEXT_PLAIN);
			}
			if(project!=null && !project.isEmpty()) {
				listener.getLogger().println(pluginName + " : target project '"+project+"'");
				builder.addTextBody("projectID", project, ContentType.TEXT_PLAIN);
			}
			if(release!=null && !release.isEmpty()) {
				listener.getLogger().println(pluginName + " : using release '"+release+"'");
				builder.addTextBody("releaseID", release, ContentType.TEXT_PLAIN);
				if(cycle!=null && !cycle.isEmpty()) {
					listener.getLogger().println(pluginName + " : using cycle '"+cycle+"'");
					builder.addTextBody("cycleID", cycle, ContentType.TEXT_PLAIN);
				}
			}
			
			if(testCaseField!=null && !testCaseField.isEmpty()) {
				listener.getLogger().println(pluginName + " : target test case Fields '"+ testCaseField +"'");
				builder.addTextBody("testcase_fields", testCaseField, ContentType.TEXT_PLAIN);
			}
			
			if(testSuiteField!=null && !testSuiteField.isEmpty()) {
				listener.getLogger().println(pluginName + " : target test suite Fields '"+ testSuiteField +"'");
				builder.addTextBody("testsuite_fields", testSuiteField, ContentType.TEXT_PLAIN);
			}

			File f = new File(filePath);
			builder.addPart("file", new FileBody(f));
			HttpEntity multipart = builder.build();

			HttpPost uploadFile = new HttpPost(getUrl() + "/rest/import/createandscheduletestresults/1");
			uploadFile.addHeader("accept", "application/json");
			uploadFile.addHeader("scope", "default");
			uploadFile.addHeader("apiKey", getKey());
			uploadFile.setEntity(multipart);

			if(proxyUrl != null && !proxyUrl.isEmpty())
			{
				listener.getLogger().println(pluginName + " : Proxy Url '" + proxyUrl + "'");
				//Setting proxy
				RequestConfig config = RequestConfig.custom().setProxy(HttpHost.create(proxyUrl)).build();
				uploadFile.setConfig(config);
			}

			httpClient = HttpClients.createDefault();
			response = httpClient.execute(uploadFile);
			String responseString = EntityUtils.toString(response.getEntity());
			if (response.getStatusLine().getStatusCode() == 200) 
			{
				try
				{
					JSONObject jsonresponse = (JSONObject) new JSONParser().parse(responseString);
					if(jsonresponse.get("success").toString().equals("true"))
					{
						listener.getLogger().println(pluginName + " : Response --> " + jsonresponse.toString().replace("\\/","/"));
					}
					else
					{
						listener.getLogger().println(pluginName + " : Response : '"+responseString+"'");
						throw new QMetryException("Error uploading file to server!");
					}
				}
				catch(ParseException e)
				{
					listener.getLogger().println(pluginName + " : ERROR :: QMetryConnection in uploadFileToTestSuite : '"+responseString+"'");
					throw new QMetryException("Error uploading file to server!");
				}
			}
			else
			{
				listener.getLogger().println(pluginName + " : Response : '"+responseString+"'");
				throw new QMetryException("Error uploading file to server!");
			}
			httpClient.close();
			response.close();
	}
}