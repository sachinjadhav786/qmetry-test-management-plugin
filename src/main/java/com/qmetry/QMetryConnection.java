package com.qmetry;

import java.io.File;


import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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

	public void uploadFileToTestSuite(String filePath, String testSuiteName, String testSName, String tsFolderPath, String automationFramework, String automationHierarchy,
									  String buildName, String platformName, String project, String release, String cycle, String pluginName, /*BuildListener*/TaskListener listener,
									  int buildnumber, String proxyUrl, String testCaseField, String testSuiteField, String skipWarning, String isMatchingRequired) throws Exception {

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
		if(tsFolderPath!=null && !tsFolderPath.isEmpty())
		{
			listener.getLogger().println(pluginName + " : test suite folder path '"+tsFolderPath+"'");
			builder.addTextBody("tsFolderPath", tsFolderPath, ContentType.TEXT_PLAIN);
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
		if(skipWarning!=null && !skipWarning.isEmpty()) {
			listener.getLogger().println(pluginName + " : skipWarning '"+ skipWarning +"'");
			builder.addTextBody("skipWarning", skipWarning, ContentType.TEXT_PLAIN);
		}
		if(isMatchingRequired!=null && !isMatchingRequired.isEmpty()) {
			listener.getLogger().println(pluginName + " : isMatchingRequired '"+ isMatchingRequired +"'");
			builder.addTextBody("is_matching_required", isMatchingRequired, ContentType.TEXT_PLAIN);
		}

		File f = new File(filePath);
		builder.addPart("file", new FileBody(f));
		HttpEntity multipart = builder.build();

		listener.getLogger().println(pluginName + " : URL '"+ getUrl() + "/rest/import/createandscheduletestresults/1" +"'");

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
				if(jsonresponse.get("success").toString().equals("true")) {

					listener.getLogger().println(pluginName + " : Response --> " + jsonresponse.toString().replace("\\/","/"));

					if(jsonresponse.toString().contains("requestId") && jsonresponse.get("requestId") != null) {
						getRequeststatus(jsonresponse.get("requestId"), httpClient, pluginName, listener);
					}
				}
				else {
					listener.getLogger().println(pluginName + " : Response : '"+responseString+"'");
					throw new QMetryException("Error uploading file to server!");
				}
			}
			catch(ParseException e) {
				listener.getLogger().println(pluginName + " : ERROR :: QMetryConnection in uploadFileToTestSuite : '"+responseString+"'");
				throw new QMetryException("Error uploading file to server!");
			}
		}
		else {
			listener.getLogger().println(pluginName + " : Response : '"+responseString+"'");
			throw new QMetryException("Error uploading file to server!");
		}
		httpClient.close();
		response.close();
	}
    
    public void getRequeststatus(Object requestId, CloseableHttpClient httpClient, String pluginName, TaskListener listener) throws Exception {

	String statusString = null;
	try {
	    HttpGet getStatus = new HttpGet(getUrl() + "/rest/admin/status/automation/" + requestId);
	    getStatus.addHeader("apiKey",getKey());
	    getStatus.addHeader("scope","default");

	    CloseableHttpResponse statusResponse = httpClient.execute(getStatus);
	    statusString = EntityUtils.toString(statusResponse.getEntity());
	    JSONObject statusObj = (JSONObject) new JSONParser().parse(statusString);

		String s = pluginName + " : Response --> " + statusObj.toString().replace("\\/", "/");

		if (statusResponse.getStatusLine().getStatusCode() != 200) {
			listener.getLogger().println(pluginName+"Couldn't get request details.");
			listener.getLogger().println(pluginName+"Status Code : "+ statusResponse.getStatusLine().getStatusCode());
	    }else if (statusObj.get("status").toString().equals("In Queue")) {
			listener.getLogger().println(s);
			requestagain(requestId, httpClient, pluginName, listener);
		}else if(statusObj.get("status").toString().equals("In Progress")) {
			getRequeststatus(requestId, httpClient, pluginName, listener);
		} else{
			listener.getLogger().println(s);
		}
		if(statusObj.get("status").toString().equals("Completed")) {
			listener.getLogger().println(pluginName+" : Test results uploaded successfully!");
	    }

	} catch(ParseException e) {
	    listener.getLogger().println(pluginName + " : ERROR :: QMetryConnection in uploadFileToTestSuite : '"+statusString+"'");
	    throw new QMetryException("Error uploading file to server!");
	}
    }

	//Request again method for 10 min 2nd API call
	public void requestagain(Object requestId, CloseableHttpClient httpClient, String pluginName, TaskListener listener) throws Exception{
		String statusString = null;
		HttpGet getStatus = new HttpGet(getUrl() + "/rest/admin/status/automation/" + requestId);
		getStatus.addHeader("apiKey",getKey());
		getStatus.addHeader("scope","default");
		//Timer function for all API 10 mins
		long start = System.currentTimeMillis(); //start time
		long end = start + 10 * 60 * 1000; // 10 mins (60*1000 = 1 min | 1*10 = 10 mins)
		boolean flag = false;
		//Loop to start timer ( Run from current time to next 10 mins in future)
		while (System.currentTimeMillis() < end) {
			CloseableHttpResponse statusResponse = httpClient.execute(getStatus);
			statusString = EntityUtils.toString(statusResponse.getEntity());
			JSONObject statusObj = (JSONObject) new JSONParser().parse(statusString);
			String s = pluginName + " : Response --> " + statusObj.toString().replace("\\/", "/");
		    //In Progress status
			if(statusObj.get("status").toString().equals("In Progress")&& flag==false) {
				listener.getLogger().println(s);
				flag = true;
			}
			// Completed or Failed status
			if(statusObj.get("status").toString().equals("Completed")||statusObj.get("status").toString().equals("Failed")){
				listener.getLogger().println(s);
				break;
			}
		}
	}
}