package com.qmetry;

import hudson.Extension;
import hudson.Launcher;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import hudson.util.FormValidation;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.File;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class QTMReportPublisher extends Recorder {

    private final String qtmUrl;
    private final String qtmAutomationApiKey;
    private final String automationFramework;
    private final String testResultFilePath;
    private final String buildName;
    private final String testSuiteName;
    private final String platformName;

    @DataBoundConstructor
    public QTMReportPublisher(final String qtmUrl, final String qtmAutomationApiKey, final String automationFramework,
            final String testResultFilePath, final String buildName, final String testSuiteName, final String platformName) {
        this.qtmUrl = qtmUrl;
        this.qtmAutomationApiKey = qtmAutomationApiKey;
        this.automationFramework = automationFramework;
        this.testResultFilePath = testResultFilePath;
        this.buildName = buildName;
        this.testSuiteName = testSuiteName;
        this.platformName = platformName;
    }

    public String getQtmUrl() {
        return this.qtmUrl;
    }

    public String getQtmAutomationApiKey() {
        return this.qtmAutomationApiKey;
    }

    public String getAutomationFramework() {
        return this.automationFramework;
    }

    public String getTestResultFilePath() {
        return this.testResultFilePath;
    }

    public String getBuildName() {
        return this.buildName;
    }

    public String getTestSuiteName() {
        return this.testSuiteName;
    }

    public String getPlatformName() {
        return this.platformName;
    }

    public String getParsedQtmUrl() {
        if (!getQtmUrl().endsWith("/")) {
            return getQtmUrl() + "/";
        } else {
            return getQtmUrl();
        }
    }

    @Override
	public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener)
	{
		String pluginName = "QMetry Test Management Plugin";
        try
		{
            String displayName = pluginName + " : Starting Post Build Action";
            if (this.testSuiteName!=null && !this.testSuiteName.isEmpty()) 
			{
                displayName += " : " + this.testSuiteName;
            }
            String repeated = new String(new char[displayName.length()]).replace("\0", "-");
            listener.getLogger().println("\n" + repeated + "\n" + displayName + "\n" + repeated);
			if(getQtmUrl() == null || getQtmUrl().isEmpty())
			{
				throw new QMetryException("Please enter a valid URL to QMetry Test Management in configuration form");
			}
			if(getQtmAutomationApiKey() == null || getQtmAutomationApiKey().isEmpty())
			{
				throw new QMetryException("Please enter a valid QMetry Test Management automation API Key in configuration form");
			}
			if(getAutomationFramework() == null || getAutomationFramework().isEmpty() 
				|| !(getAutomationFramework().equals("CUCUMBER") 
						|| getAutomationFramework().equals("TESTNG")
						|| getAutomationFramework().equals("JUNIT")
						|| getAutomationFramework().equals("QAS")
						|| getAutomationFramework().equals("HPUFT")))
			{
				throw new QMetryException("Please enter a valid automation framework in configuration form [CUCUMBER/JUNIT/TESTNG/QAS/HPUFT]");
			}
			if(getTestResultFilePath() == null || getTestResultFilePath().isEmpty())
			{
				throw new QMetryException("Please enter a valid path to your test result file(s) path/directory in configuration form");
			}
            String compfilepath = build.getWorkspace().toString() + "/" + getTestResultFilePath().trim();
            String buildName = getBuildName();
            String platformName = getPlatformName();
            String testSuiteName = getTestSuiteName();
            if (testSuiteName == null)
			{
				testSuiteName = "";
			}
			else
			{
				testSuiteName = testSuiteName.trim();
				if(!testSuiteName.isEmpty()) {
					listener.getLogger().println(pluginName + " : Using Test Suite '"+testSuiteName+"'");
				}
			}
            if (buildName == null)
            {
				buildName = "";
			}
			else
			{
				buildName = buildName.trim();
				if(!buildName.isEmpty()) {
					listener.getLogger().println(pluginName + " : Using Cycle '"+buildName+"'");
				}
			}
            if (platformName == null)
			{
                platformName = "";
			}
			else
			{
				platformName = platformName.trim();
				if(!platformName.isEmpty()) {
					listener.getLogger().println(pluginName + " : Using Platform '"+platformName+"'");
				}
			}
			
            File filePath = new File(compfilepath);
			if(filePath==null || !filePath.exists()) throw new QMetryException("Failed to read result file(s) at location '"+compfilepath+"'");
			compfilepath = filePath.getAbsolutePath();
            QTMApiConnection conn = new QTMApiConnection(getQtmUrl().trim(), getQtmAutomationApiKey().trim());
            synchronized (conn) 
			{
                // Upload Result Files
                if (filePath.isDirectory()) 
				{
                    listener.getLogger().println(pluginName + " : Reading result files from Directory '"+compfilepath+"'");
                    File[] listOfFiles = filePath.listFiles();

                    for (int i = 0; i < listOfFiles.length; i++) 
					{
                        if (listOfFiles[i].isFile() && (listOfFiles[i].getName().endsWith(".xml") || listOfFiles[i].getName().endsWith(".json"))) 
						{
                            listener.getLogger().println("\nQTMJenkinsPlugin : Result File Found '" + listOfFiles[i].getName() + "'");
                            try 
							{
                                listener.getLogger().println(pluginName + " : Uploading result file...");
                                String response = conn.uploadFileToTestSuite(listOfFiles[i].getAbsolutePath(), testSuiteName,
                                        getAutomationFramework(), buildName, platformName);
                                listener.getLogger().println(pluginName + " : Response : " + response);
                                listener.getLogger().println(pluginName + " : Result file successfully uploaded!");
                            } 
							catch (QMetryException e) 
							{
                                listener.getLogger().println(pluginName + " : Failed to upload Result file!");
                            }
                        }
                    }
                }
				else  if(filePath.isFile())
				{
                    listener.getLogger().println(pluginName + " : Reading result file '"+compfilepath+"'");
                    listener.getLogger().println(pluginName + " : Uploading result file...");
                    String response = conn.uploadFileToTestSuite(compfilepath, testSuiteName, getAutomationFramework(), buildName, platformName);
					listener.getLogger().println(pluginName + " : Response : " + response);
                    listener.getLogger().println(pluginName + " : Result file successfully uploaded!");
                }
				else
				{
					throw new QMetryException("Failed to read result file(s) at location '"+compfilepath+"'");
				}
            } // connection synchronized block
        }
		catch (Exception e) 
		{
            listener.getLogger().println(pluginName + " : ERROR : " + e.toString());
			return false;
        }
        listener.getLogger().println("\n" + pluginName + " : Successfully finished Post Build Action!");
        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public ListBoxModel doFillAutomationFrameworkItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("Cucumber", "CUCUMBER");
            items.add("Junit", "JUNIT");
            items.add("TestNG", "TESTNG");
            items.add("QAS", "QAS");
            items.add("HP-UFT", "HPUFT");
            return items;
        }

        public FormValidation doCheckQtmUrl(@QueryParameter String qtmUrl) throws IOException, ServletException {
            if (qtmUrl.length() == 0 || qtmUrl.length() < 4
                    || (!qtmUrl.startsWith("https://") && !qtmUrl.startsWith("http://"))) {
                return FormValidation.error("Please enter valid QTM API URL!");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckQtmAutomationApiKey(@QueryParameter String qtmAutomationApiKey)
                throws IOException, ServletException {
            if (!(qtmAutomationApiKey.length()>10)) {
                return FormValidation.error("Please enter valid QTM Automation API Key!");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTestResultFilePath(@QueryParameter String testResultFilePath)
                throws IOException, ServletException {
            if (testResultFilePath == null) {
                return FormValidation.error("Please provide a file path(or directory for multiple files)");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckAutomationFramework(@QueryParameter String automationFramework)
                throws IOException, ServletException {
            if (automationFramework == null) {
                return FormValidation.error("Please select an Automation Framework!");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Publish Build Result(s) to QMetry Test Management";
        }
    }
}