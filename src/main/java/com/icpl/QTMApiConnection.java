package com.icpl;

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

    private JSONObject projectInformationJson;

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
        // TODO validate connection using API
        return true;
    }

	/*
    public JSONObject getProjectInformationJson() throws QTMException {
        if (this.projectInformationJson == null) {
            this.projectInformationJson = generateProjectInformationJson();
        }
        return this.projectInformationJson;
    }

    public String getAutomationApiKey() throws QTMException {
        String key = null;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(getUrl() + "/rest/admin/project/getinfo");
            httpGet.addHeader("apikey", getKey());
            httpGet.addHeader("scope", "default");
            httpGet.addHeader("Accept", "application/json");
            response = httpClient.execute(httpGet);
            String respEntityStr = EntityUtils.toString(response.getEntity());
            if (!respEntityStr.contains("success")) {
                throw new QTMException();
            }
            JSONParser parser = new JSONParser();
            JSONObject automationJson = (JSONObject) parser.parse(respEntityStr);
            key = automationJson.get("automationAPIKey").toString();
        } catch (Exception e) {
            System.out.println("QTMJenkinsPlugin : ERROR : " + e);
            throw new QTMException("Could not generate Automation Key!");
        } finally {
            try {
                httpClient.close();
                response.close();
            } catch (Exception e) {
            }
        }
        return key;
    }

    public Map<String, String> getGeneralInfo() throws QTMException {
        Map<String, String> projectData = null;
        try {
            JSONObject projectsInfoJson = getProjectInformationJson();
            JSONObject rootFolders = (JSONObject) projectsInfoJson.get("rootFolders");
            JSONObject rootFoldersTS = (JSONObject) rootFolders.get("TS");

            projectData = new HashMap<String, String>();
            projectData.put("testSuiteParentFolderId", rootFoldersTS.get("id").toString());
            projectData.put("projectId", projectsInfoJson.get("currentProjectId").toString());
            projectData.put("releaseId", projectsInfoJson.get("currentReleaseId").toString());
            projectData.put("buildId", projectsInfoJson.get("currentBuildId").toString());

            JSONObject views = (JSONObject) projectsInfoJson.get("views");
            JSONObject views_ts = (JSONObject) views.get("TS");
            JSONObject views_ts_platform = (JSONObject) views_ts.get("TsPlatform");
            projectData.put("defaultPlatformId", views_ts_platform.get("id").toString());

            JSONObject projectPlatforms = (JSONObject) projectsInfoJson.get("projectPlatforms");
            Iterator iterator = projectPlatforms.keySet().iterator();
            for (; iterator.hasNext();) {
                String pid = (String) iterator.next();
                projectData.put("qtmplatform:" + projectPlatforms.get(pid), pid);
            }

            JSONObject currentUser = (JSONObject) projectsInfoJson.get("currentUser");
            projectData.put("currentUserId", currentUser.get("id").toString());
        } catch (Exception e) {
            System.out.println("QTMJenkinsPlugin : ERROR : " + e);
            throw new QTMException("Could not fetch project information!");
        }
        return projectData;
    }

    public String createNewPlatform(String platformName, String scope) throws QTMException {
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = null;
        String platformId = null;
        try {
            httpClient = HttpClients.createDefault();
            HttpPost post = new HttpPost(getUrl() + "/rest/admin/platform");
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-Type", "application/json");
            post.setHeader("scope", scope);
            post.setHeader("apiKey", getKey());

            JSONObject reqJson = new JSONObject();
            reqJson.put("name", platformName);
            reqJson.put("platformID", 0);
            reqJson.put("platformType", "single");

            post.setEntity(new StringEntity(reqJson.toJSONString()));
            response = httpClient.execute(post);
            String respEntity = EntityUtils.toString(response.getEntity());
            if (!respEntity.contains("success")) {
                throw new QTMException();
            }
            JSONParser parser = new JSONParser();
            JSONObject respJson = (JSONObject) parser.parse(respEntity);
            platformId = respJson.get("platformID").toString();
        } catch (Exception e) {
            System.out.println("QTMJenkinsPlugin : ERROR : " + e);
            throw new QTMException("Could not create new platform '" + platformName + "'!");
        } finally {
            try {
                httpClient.close();
                response.close();
            } catch (Exception e) {
            }
        }
        return platformId;
    }

    public String createNewTestSuite(String testSuiteName, String testSuiteDescription, String ownerId,
            String parentFolderId, String automationFramework, String buildFramework, String projectDirectory,
            String filePath, String projectId, String releaseId, String buildId) throws QTMException {
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = null;
        String scope = projectId + ":" + releaseId + ":" + buildId;
        String testSuiteId = null;
        try {
            httpClient = HttpClients.createDefault();
            HttpPost post = new HttpPost(getUrl() + "/rest/testsuites");
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-Type", "application/json");
            post.setHeader("scope", scope);
            post.setHeader("apiKey", getKey());

            JSONObject reqJson = new JSONObject();
            reqJson.put("name", testSuiteName);
            reqJson.put("description", testSuiteDescription);
            reqJson.put("owner", ownerId);
            reqJson.put("parentFolderId", parentFolderId);
            reqJson.put("isAutomatedFlag", "true");
            reqJson.put("frameWork", automationFramework);
            if (!buildFramework.equals("none")) {
                reqJson.put("buildFramework", buildFramework);
            }
            reqJson.put("projectHomeDirectory", projectDirectory);
            reqJson.put("projectPath", filePath);

            post.setEntity(new StringEntity(reqJson.toJSONString()));
            response = httpClient.execute(post);

            String respEntity = EntityUtils.toString(response.getEntity());
            if (!respEntity.contains("success")) {
                throw new QTMException();
            }
            JSONParser parser = new JSONParser();
            JSONObject respJson = (JSONObject) parser.parse(respEntity);
            JSONArray data = (JSONArray) respJson.get("data");
            respJson = (JSONObject) data.get(0);
            testSuiteId = respJson.get("id").toString();
        } catch (Exception e) {
            System.out.println("QTMJenkinsPlugin : ERROR : " + e);
            throw new QTMException("Could not create new test suite '" + testSuiteName + "'!");
        } finally {
            try {
                httpClient.close();
                response.close();
            } catch (Exception e) {
            }
        }
        return testSuiteId;
    }

    public boolean mapTestSuiteReleaseCycle(String testSuiteId, String releaseId, String buildId, String scope)
            throws QTMException {
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = null;
        HttpPost post = null;
        try {
            httpClient = HttpClients.createDefault();
            post = new HttpPost(getUrl() + "/rest/testsuites/mapReleaseCycle");
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-Type", "application/json");
            post.setHeader("scope", scope);
            post.setHeader("apiKey", getKey());

            JSONObject reqJson = new JSONObject();
            reqJson.put("tsId", testSuiteId);
            reqJson.put("buildID", buildId);
            reqJson.put("releaseId", releaseId);

            JSONArray dataarray = new JSONArray();
            dataarray.add(reqJson);

            JSONObject finalJson = new JSONObject();
            finalJson.put("data", dataarray);

            post.setEntity(new StringEntity(finalJson.toJSONString()));
            response = httpClient.execute(post);
            String respEntity = EntityUtils.toString(response.getEntity());
            if (!respEntity.contains("success")) {
                throw new QTMException();
            }

            return true;
        } catch (Exception e) {
            System.out.println("QTMJenkinsPlugin : ERROR : " + e);
            throw new QTMException("Could not map test suite '" + testSuiteId + "' to release & cycle!");
        } finally {
            try {
                httpClient.close();
                response.close();
            } catch (Exception e) {
            }
        }
    }
	*/

    public String uploadFileToTestSuite(String filePath, String testSuiteName, String automationFramework,
            String buildName, String platformName)
            throws InvalidCredentialsException, ProtocolException, IOException, QTMException {
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
            if (!(response.getStatusLine().getStatusCode() == 200)) 
			{
                throw new QTMException("Error uploading file!");
            }
            return respEntityStr;
        } catch (Exception e) {
            System.out.println("QTMJenkinsPlugin : ERROR : " + e);
            throw new QTMException(
                    "Could not upload file '" + filePath + "' to test suite '" + testSuiteName + "'!");
        } finally {
            try {
                httpClient.close();
                response.close();
            } catch (Exception e) {
            }
        }
    }

	/*
    public Map<Long, String> getProjectList() throws QTMException {
        Map<Long, String> projectList = null;
        try {
            JSONObject projectsInfoJson = getProjectInformationJson();
            JSONArray projects = (JSONArray) projectsInfoJson.get("projects");
            projectList = new HashMap<Long, String>();
            JSONObject project = null;
            Iterator itr = projects.iterator();
            while (itr.hasNext()) {
                project = (JSONObject) itr.next();
                if (!(boolean) project.get("isArchive")) {
                    projectList.put((Long) project.get("projectID"), (String) project.get("name"));
                }
            }
        } catch (Exception e) {
            System.out.println("QTMJenkinsPlugin : ERROR : " + e);
            throw new QTMException("Could not fetch project list!");
        }
        return projectList;
    }

    public Map<Long, String> getReleaseList(Long projectId) throws QTMException {
        Map<Long, String> releaseList = null;
        try {
            JSONObject projectsInfoJson = getProjectInformationJson();
            JSONArray projects = (JSONArray) projectsInfoJson.get("projects");
            releaseList = new HashMap<Long, String>();
            JSONObject project = null;
            Iterator projectsI = projects.iterator();
            while (projectsI.hasNext()) {
                project = (JSONObject) projectsI.next();
                if ((Long) project.get("projectID") == projectId) {
                    JSONArray releases = (JSONArray) project.get("releases");
                    JSONObject release = null;
                    Iterator releaseI = releases.iterator();
                    while (releaseI.hasNext()) {
                        release = (JSONObject) releaseI.next();
                        if (!(boolean) release.get("isArchive")) {
                            releaseList.put((Long) release.get("releaseID"), (String) release.get("name"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("QTMJenkinsPlugin : ERROR : " + e);
            throw new QTMException("Could not fetch project list!");
        }
        return releaseList;
    }

    public Map<Long, String> getBuildList(Long projectId, Long releaseId) throws QTMException {
        Map<Long, String> buildList = null;
        try {
            JSONObject projectsInfoJson = getProjectInformationJson();
            JSONArray projects = (JSONArray) projectsInfoJson.get("projects");
            buildList = new HashMap<Long, String>();
            JSONObject project = null;
            Iterator projectsI = projects.iterator();
            while (projectsI.hasNext()) {
                project = (JSONObject) projectsI.next();
                if ((Long) project.get("projectID") == projectId) {
                    JSONArray releases = (JSONArray) project.get("releases");
                    JSONObject release = null;
                    Iterator releaseI = releases.iterator();
                    while (releaseI.hasNext()) {
                        release = (JSONObject) releaseI.next();
                        if ((Long) release.get("releaseID") == releaseId) {
                            JSONArray builds = (JSONArray) release.get("builds");
                            JSONObject build = null;
                            Iterator buildI = builds.iterator();
                            while (buildI.hasNext()) {
                                build = (JSONObject) buildI.next();
                                if (!(boolean) build.get("isArchive")) {
                                    buildList.put((Long) build.get("buildID"), (String) build.get("name"));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("QTMJenkinsPlugin : ERROR : " + e);
            throw new QTMException("Could not fetch build list!");
        }
        return buildList;
    }

    public Map<Long, String> generateTestSuiteList(Long projectId) throws QTMException {
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = null;
        HttpPost post = null;
        Map<Long, String> testSuiteList = null;
        try {
            //generate a scope for given projectId
            JSONObject projectsInfoJson = getProjectInformationJson();
            JSONArray projects = (JSONArray) projectsInfoJson.get("projects");
            String scope = null;
            JSONObject project = null;
            Iterator projectsI = projects.iterator();

            while (projectsI.hasNext()) {
                project = (JSONObject) projectsI.next();
                if ((Long) project.get("projectID") == projectId) {
                    scope = projectId.toString();
                    JSONArray releases = (JSONArray) project.get("releases");
                    JSONObject release = (JSONObject) releases.get(0);
                    scope += ":" + release.get("releaseID").toString();
                    JSONArray builds = (JSONArray) release.get("builds");
                    JSONObject build = (JSONObject) builds.get(0);
                    scope += ":" + build.get("buildID").toString();
                }
            }
            if (scope == null) {
                throw new QTMException("Could not fetch test suite list!");
            }

            //get test suite root folder id to get all test suites
            JSONObject rootFolders = (JSONObject) projectsInfoJson.get("rootFolders");
            JSONObject rootFoldersTS = (JSONObject) rootFolders.get("TS");

            String testSuiteParentFolderId = rootFoldersTS.get("id").toString();

            //Make the api call
            httpClient = HttpClients.createDefault();
            post = new HttpPost(getUrl() + "/rest/testsuites/list");
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-Type", "application/json");
            post.setHeader("scope", scope);
            post.setHeader("apiKey", getKey());

            JSONObject reqJson = new JSONObject();
            reqJson.put("tsFolderId", testSuiteParentFolderId);
            reqJson.put("buildID", "cycle");

            post.setEntity(new StringEntity(reqJson.toJSONString()));
            response = httpClient.execute(post);
            String respEntity = EntityUtils.toString(response.getEntity());
            JSONParser parser = new JSONParser();
            JSONObject testSuites = (JSONObject) parser.parse(respEntity);
            JSONArray data = (JSONArray) testSuites.get("data");
            JSONObject suite = null;
            testSuiteList = new HashMap<Long, String>();
            Iterator dataI = data.iterator();
            while (dataI.hasNext()) {
                suite = (JSONObject) dataI.next();
                testSuiteList.put((Long) suite.get("tsID"), (String) suite.get("name"));
            }
        } catch (Exception e) {
            System.out.println("QTMJenkinsPlugin : ERROR : " + e);
            throw new QTMException("Could not fetch test suite list!");
        } finally {
            try {
                httpClient.close();
                response.close();
            } catch (Exception e) {
            }
        }
        return testSuiteList;
    }

    public JSONObject generateProjectInformationJson() throws QTMException {
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = null;
        JSONObject projectsInfoJson = null;
        try {
            HttpGet httpGet = new HttpGet(getUrl() + "/rest/admin/project/getinfo");
            httpGet.addHeader("apikey", getKey());
            httpGet.addHeader("scope", "default");
            httpGet.addHeader("Accept", "application/json");

            httpClient = HttpClients.createDefault();
            response = httpClient.execute(httpGet);
            String respEntityStr = EntityUtils.toString(response.getEntity());
            JSONParser parser = new JSONParser();
            projectsInfoJson = (JSONObject) parser.parse(respEntityStr);

        } catch (Exception e) {
            System.out.println("QTMJenkinsPlugin : ERROR : " + e);
            throw new QTMException("Could not fetch project information!");
        } finally {
            try {
                httpClient.close();
                response.close();
            } catch (Exception e) {
            }
        }
        return projectsInfoJson;
    } 
	*/
}