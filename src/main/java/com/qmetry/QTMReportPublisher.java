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
import org.apache.commons.httpclient.auth.InvalidCredentialsException;
import java.net.ProtocolException;

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
	private final String project;
	private final String release;
	private final String cycle;

    @DataBoundConstructor
    public QTMReportPublisher(final String qtmUrl, final String qtmAutomationApiKey, final String automationFramework,
            final String testResultFilePath, final String buildName, final String testSuiteName, final String platformName,
			final String project, final String release, final String cycle) {
        this.qtmUrl = qtmUrl;
        this.qtmAutomationApiKey = qtmAutomationApiKey;
        this.automationFramework = automationFramework;
        this.testResultFilePath = testResultFilePath;
        this.buildName = buildName;
        this.testSuiteName = testSuiteName;
        this.platformName = platformName;
		this.project = project;
		this.cycle = cycle;
		this.release = release;
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
	
	public String getProject() {
		return this.project;
	}
	
	public String getRelease() {
		return this.release;
	}
	
	public String getCycle() {
		return this.cycle;
	}

    @Override
	public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener)
	{
		String pluginName = "QMetry Test Management Plugin";
        try
		{
			String qtmUrl_chkd = getQtmUrl();
			String qtmAutomationApiKey_chkd = getQtmAutomationApiKey();
			String automationFramework_chkd = getAutomationFramework();
			String testResultFilePath_chkd = getTestResultFilePath();
            String buildName_chkd = getBuildName();
            String platformName_chkd = getPlatformName();
            String testSuiteName_chkd = getTestSuiteName();
			String release_chkd = getRelease();
			String cycle_chkd = getCycle();
			String project_chkd = getProject();
            String displayName = pluginName + " : Starting Post Build Action";

            if (testSuiteName_chkd!=null && !testSuiteName_chkd.isEmpty()) 
			{
                displayName += " : " + testSuiteName_chkd;
            }
            String repeated = new String(new char[displayName.length()]).replace("\0", "-");
            listener.getLogger().println("\n" + repeated + "\n" + displayName + "\n" + repeated);
			
			if(qtmUrl_chkd == null || qtmUrl_chkd.isEmpty())
			{
				throw new QMetryException("Please enter a valid URL to QMetry Test Management in configuration form");
			}
			else
			{
				qtmUrl_chkd = qtmUrl_chkd.trim();
			}
			if(qtmAutomationApiKey_chkd == null || qtmAutomationApiKey_chkd.isEmpty())
			{
				throw new QMetryException("Please enter a valid QMetry Test Management automation API Key in configuration form");
			}
			else
			{
				qtmAutomationApiKey_chkd = qtmAutomationApiKey_chkd.trim();
			}
			if(automationFramework_chkd == null || automationFramework_chkd.isEmpty() 
				|| !(automationFramework_chkd.equals("CUCUMBER") 
						|| automationFramework_chkd.equals("TESTNG")
						|| automationFramework_chkd.equals("JUNIT")
						|| automationFramework_chkd.equals("QAS")
						|| automationFramework_chkd.equals("HPUFT")))
			{
				throw new QMetryException("Please enter a valid automation framework in configuration form [CUCUMBER/JUNIT/TESTNG/QAS/HPUFT]");
			}
			else
			{
				automationFramework_chkd = automationFramework_chkd.trim();
			}
			if(testResultFilePath_chkd == null || testResultFilePath_chkd.isEmpty())
			{
				throw new QMetryException("Please enter a valid path to your test result file(s) path/directory in configuration form");
			}
			else
			{
				testResultFilePath_chkd = testResultFilePath_chkd.trim();
				testResultFilePath_chkd = testResultFilePath_chkd.replace("\\","/");
			}
            if (testSuiteName_chkd != null)
			{
				testSuiteName_chkd = testSuiteName_chkd.trim();
				if(!testSuiteName_chkd.isEmpty()) {
					listener.getLogger().println(pluginName + " : Using Test Suite '"+testSuiteName_chkd+"'");
				}
			}
            if(project_chkd != null)
			{
				project_chkd = project_chkd.trim();
				if(!project_chkd.isEmpty()) {
					listener.getLogger().println(pluginName + " : Using Project '"+project_chkd+"'");
				}
			}
            if(release_chkd != null)
			{
				release_chkd = release_chkd.trim();
				if(!release_chkd.isEmpty()) {
					listener.getLogger().println(pluginName + " : Using Release '"+release_chkd+"'");
				}
			}
            if(cycle_chkd != null)
			{
				cycle_chkd = cycle_chkd.trim();
				if(!cycle_chkd.isEmpty()) {
					listener.getLogger().println(pluginName + " : Using Cycle '"+cycle_chkd+"'");
				}
			}
            if (buildName_chkd != null)
			{
				buildName_chkd = buildName_chkd.trim();
				if(!buildName.isEmpty()) {
					listener.getLogger().println(pluginName + " : Using Drop (Build) '"+buildName_chkd+"'");
				}
			}
            if(platformName_chkd != null)
			{
				platformName_chkd = platformName_chkd.trim();
				if(!platformName_chkd.isEmpty()) {
					listener.getLogger().println(pluginName + " : Using Platform '"+platformName_chkd+"'");
				}
			}
			File wordspaceFile = new File(build.getWorkspace().toString());
            File filePath = new File(wordspaceFile, testResultFilePath_chkd);
			if(filePath==null || !filePath.exists()) throw new QMetryException("Failed to read result file(s) at location '"+wordspaceFile.getAbsolutePath()+"/"+testResultFilePath_chkd+"'");
			
			QMetryResultUtil resultUtil = new QMetryResultUtil();
			resultUtil.uploadResultFilesToQMetry(pluginName, 
												listener, 
												qtmUrl_chkd, 
												qtmAutomationApiKey_chkd, 
												filePath, 
												testSuiteName_chkd, 
												automationFramework_chkd,
												buildName_chkd,
												platformName_chkd,
												project_chkd,
												release_chkd,
												cycle_chkd);
        }
		catch(NullPointerException e) {
			e.printStackTrace();
            listener.getLogger().println(pluginName + " : Failed to upload test result file(s) to server!");
			return false;
		}
		catch (QMetryException e) 
		{
			e.printStackTrace();
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

        public FormValidation doCheckProject(@QueryParameter String project)
                throws IOException, ServletException {
			if (project == null || project.length()<2) {
				return FormValidation.error("Please provide project ID, Key or Name!");
			}
            return FormValidation.ok();
        }
		
        public FormValidation doCheckCycle(@QueryParameter String project, @QueryParameter String release, @QueryParameter String cycle)
                throws IOException, ServletException {
			if(cycle !=null && cycle.length()>0) {
				if (project == null || project.length()<2) {
					return FormValidation.error("Please provide project ID, Key or Name!");
				} else if(release==null || release.length()<2) {
					return FormValidation.error("Please provide release ID or Name!");
				}
			}
            return FormValidation.ok();
        }

        public FormValidation doCheckRelease(@QueryParameter String project, @QueryParameter String release)
                throws IOException, ServletException {
            if (release!=null && release.length() > 0 && (project == null || project.length()<2)) {
                return FormValidation.error("Please provide project ID, Key or Name!");
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