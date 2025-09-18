package com.qmetry;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
//import hudson.model.BuildListener;
//import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;

public class QTMReportPublisher extends Recorder implements SimpleBuildStep {
    
    private final boolean disableaction;
    private final String qtmUrl;
    private final Secret qtmAutomationApiKey;
    private final String proxyUrl;
    private final String automationFramework;
    private final String automationHierarchy;
    private final String testResultFilePath;
    private final String buildName;
    private final String testSuiteName;
    private final String testSName;
    private final String tsFolderPath;
    private final String platformName;
    private final String project;
    private final String release;
    private final String cycle;
    private final String testcaseFields;
    private final String testsuiteFields;
    private final String skipWarning;
    private final String isMatchingRequired;
    
    @DataBoundConstructor
    public QTMReportPublisher(final String qtmUrl, final String qtmAutomationApiKey, final String proxyUrl, final String automationFramework, final String automationHierarchy,
                              final String testResultFilePath, final String buildName, final String testSuiteName, final String testSName, final String tsFolderPath, final String platformName,
                              final String project, final String release, final String cycle, final boolean disableaction, final String testcaseFields, final String testsuiteFields, final String skipWarning, final String isMatchingRequired) {
        
        this.disableaction = disableaction;
        this.qtmUrl = qtmUrl;
        this.qtmAutomationApiKey = Secret.fromString(qtmAutomationApiKey);
        this.proxyUrl = proxyUrl;
        this.automationFramework = automationFramework;
        this.automationHierarchy = automationHierarchy;
        this.testResultFilePath = testResultFilePath;
        this.buildName = buildName;
        this.testSuiteName = testSuiteName;
        this.testSName = testSName;
        this.tsFolderPath = tsFolderPath;
        this.platformName = platformName;
        this.project = project;
        this.cycle = cycle;
        this.release = release;
        this.testcaseFields = testcaseFields;
        this.testsuiteFields = testsuiteFields;
        this.skipWarning = skipWarning;
        this.isMatchingRequired = isMatchingRequired;
    }
    
    public boolean isDisableaction() {
        return this.disableaction;
    }
    
    public String getQtmUrl() {
        return this.qtmUrl;
    }
    
    public Secret getQtmAutomationApiKey() {
        return this.qtmAutomationApiKey;
    }

    public String getProxyUrl() {
        return this.proxyUrl;
    }
    
    public String getAutomationFramework() {
        return this.automationFramework;
    }
    
    public String getAutomationHierarchy() {
        return this.automationHierarchy;
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
    
    public String getTestSName() {
        return this.testSName;
    }
    
    public String getTsFolderPath() {
        return tsFolderPath;
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

    public String getTestcaseFields() {
	return testcaseFields;
    }

    public String getTestsuiteFields() {
	return testsuiteFields;
    }
    
    public String getSkipWarning() {
	return skipWarning;
    }

    public String getIsMatchingRequired() {
	return isMatchingRequired;
    }    

    @Override
    //public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener)
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws AbortException
    {
        String pluginName = "QMetry Test Management Plugin";
        int buildnumber = run.number;
        if(disableaction == false)
        {
            QMetryResultUtil resultUtil = new QMetryResultUtil();
            try {
                EnvVars env = null;
                try {
                    env = run.getEnvironment(listener);
                }      catch(Exception e) {
                    listener.getLogger().println("Error retrieving environment variables: " + e.getMessage());
                    // env = new EnvVars();
                }
                
                listener.getLogger().println("-------------------------------------------------------------------------------");
                listener.getLogger().println("                           QMetry Test Management                              ");
                listener.getLogger().println("-------------------------------------------------------------------------------");
                String qtmUrl_chkd = StringUtils.trimToEmpty(getQtmUrl());
                
                String qtmAutomationApiKey_chkd = StringUtils.trimToEmpty(Secret.toString(getQtmAutomationApiKey()));

                String proxyUrl_chkd = StringUtils.trimToEmpty(getProxyUrl());
                
                String automationFramework_chkd = StringUtils.trimToEmpty(getAutomationFramework());
                
                String automationHierarchy_chkd = StringUtils.trimToEmpty(getAutomationHierarchy());
                
                String testResultFilePath_chkd = StringUtils.trimToEmpty(getTestResultFilePath()).replace("\\","/");
                
                String buildName_chkd = StringUtils.trimToEmpty(getBuildName());
                
                String platformName_chkd = StringUtils.trimToEmpty(getPlatformName());
                
                String testSuiteName_chkd = StringUtils.trimToEmpty(getTestSuiteName());
                
                String testSName_chkd = StringUtils.trimToEmpty(getTestSName());
                
                String tsFolderPath_chkd = StringUtils.trimToEmpty(getTsFolderPath());
                
                String release_chkd = StringUtils.trimToEmpty(getRelease());
                
                String cycle_chkd = StringUtils.trimToEmpty(getCycle());
                
                String project_chkd = StringUtils.trimToEmpty(getProject());
                
                String testCaseField_chkd = StringUtils.trimToEmpty(getTestcaseFields());
                
                String testSuiteField_chkd = StringUtils.trimToEmpty(getTestsuiteFields());
                
                String skipWarning_chkd = StringUtils.trimToEmpty(getSkipWarning());

                String isMatchingRequired_chkd = StringUtils.trimToEmpty(getIsMatchingRequired());
                
                if(env != null)
                {
                    qtmUrl_chkd = env.expand(qtmUrl_chkd);
                    qtmAutomationApiKey_chkd = env.expand(qtmAutomationApiKey_chkd);
                    proxyUrl_chkd = env.expand(proxyUrl_chkd);
                    automationFramework_chkd = env.expand(automationFramework_chkd);
                    automationHierarchy_chkd = env.expand(automationHierarchy_chkd);
                    testResultFilePath_chkd = env.expand(testResultFilePath_chkd);
                    buildName_chkd= env.expand(buildName_chkd);
                    platformName_chkd= env.expand(platformName_chkd);
                    testSuiteName_chkd= env.expand(testSuiteName_chkd);
                    testSName_chkd= env.expand(testSName_chkd);
                    tsFolderPath_chkd= env.expand(tsFolderPath_chkd);
                    release_chkd= env.expand(release_chkd);
                    cycle_chkd= env.expand(cycle_chkd);
                    project_chkd= env.expand(project_chkd);
                    testCaseField_chkd = env.expand(testCaseField_chkd);
                    testSuiteField_chkd =  env.expand(testSuiteField_chkd);
                    skipWarning_chkd = env.expand(skipWarning_chkd);
                    isMatchingRequired_chkd = env.expand(isMatchingRequired_chkd);
                }
                
                String displayName = pluginName + " : Starting Post Build Action";
                
                if (StringUtils.isNotEmpty(project_chkd)) {
                    displayName += " : " + project_chkd;
                } else {
                    throw new QMetryException("Target project name cannot be empty!");
                }
                
                //String repeated = new String(new char[displayName.length()]).replace("\0", "-");
                //listener.getLogger().println("\n" + repeated + "\n" + displayName + "\n" + repeated);
                listener.getLogger().println(displayName);
                if(StringUtils.isEmpty(automationFramework_chkd) ||
                   !(automationFramework_chkd.equals("CUCUMBER")
                     || automationFramework_chkd.equals("TESTNG")
                     || automationFramework_chkd.equals("JUNIT")
                     || automationFramework_chkd.equals("QAS")
                     || automationFramework_chkd.equals("HPUFT")
                     || automationFramework_chkd.equals("ROBOT")))
                {
                    throw new QMetryException("Please enter a valid automation framework [CUCUMBER/JUNIT/TESTNG/QAS/HPUFT/ROBOT]");
                }
                else
                {
                    if(automationFramework_chkd.equals("JUNIT"))
                    {
                        if(StringUtils.isNotEmpty(automationHierarchy_chkd))
                        {
                            if(!(automationHierarchy_chkd.equals("1") || automationHierarchy_chkd.equals("2") || automationHierarchy_chkd.equals("3")))
                            {
                                throw new QMetryException("Please provide valid automationHierarchy value for framework " + automationFramework_chkd);
                            }
                        }
                    }
                    else if(automationFramework_chkd.equals("TESTNG"))
                    {
                        if(StringUtils.isNotEmpty(automationHierarchy_chkd))
                        {
                            if(!(automationHierarchy_chkd.equals("1") || automationHierarchy_chkd.equals("2") || automationHierarchy_chkd.equals("3")))
                            {
                                throw new QMetryException("Please provide valid automationHierarchy value for framework " + automationFramework_chkd);
                            }
                        }
                    }
                    else
                    {
                        if(StringUtils.isNotEmpty(automationHierarchy_chkd))
                        {
                            listener.getLogger().println(pluginName + " : Skipping automationHierarchy becuase it is not supported for framework: " + automationFramework_chkd);
                        }
                    }
                }
                if(StringUtils.isNotEmpty(skipWarning_chkd))
                {
                    if(!(skipWarning_chkd.equals("0") || skipWarning_chkd.equals("1")))
                    {
                	throw new QMetryException("Please provide valid skipWarning value");
                    }
                }
                if(StringUtils.isNotEmpty(isMatchingRequired_chkd))
                {
                    if(!(isMatchingRequired_chkd.equals("false") || isMatchingRequired_chkd.equals("true")))
                    {
                	throw new QMetryException("Please provide valid isMatchingRequired value");
                    }
                } 
                if(StringUtils.isEmpty(qtmUrl_chkd)) {
                    throw new QMetryException("URL to qmetry instance cannot be empty");
                }
                if(StringUtils.isEmpty(qtmAutomationApiKey_chkd)) {
                    throw new QMetryException("Automation API key cannot be empty");
                }
                if(StringUtils.isEmpty(testResultFilePath_chkd)) {
                    throw new QMetryException("Please enter a valid path to your test result file(s) path/directory");
                }
                if(StringUtils.isNotEmpty(cycle_chkd) && StringUtils.isEmpty(release_chkd)) {
                    throw new QMetryException("Please provide target release for cycle '"+cycle_chkd+"' in project '"+project_chkd+"'");
                }
                if(StringUtils.isNotEmpty(buildName_chkd) && (StringUtils.isEmpty(release_chkd) || StringUtils.isEmpty(cycle_chkd))) {
                    throw new QMetryException("Please provide target release and cycle for build '"+buildName_chkd+"' in project '"+project_chkd+"'");
                }
                
                resultUtil.uploadResultFilesToQMetry(/*build*/run,
                                                     pluginName,
                                                     listener,
                                                     workspace,
                                                     qtmUrl_chkd,
                                                     qtmAutomationApiKey_chkd,
                                                     proxyUrl_chkd,
                                                     testResultFilePath_chkd,
                                                     testSuiteName_chkd,
                                                     testSName_chkd,
                                                     tsFolderPath_chkd,
                                                     automationFramework_chkd,
                                                     automationHierarchy_chkd,
                                                     buildName_chkd,
                                                     platformName_chkd,
                                                     project_chkd,
                                                     release_chkd,
                                                     cycle_chkd,
                                                     buildnumber,
                                                     testCaseField_chkd,
                                                     testSuiteField_chkd,
                                                     skipWarning_chkd,
                                                     isMatchingRequired_chkd);
            }
            catch (QMetryException e)
            {
                e.printStackTrace();
                listener.getLogger().println(pluginName + " : ERROR : " + e.getMessage());
                listener.getLogger().println(pluginName + " : Failed to upload test result file(s) to server. Please send these logs to qtmprofessional@qmetrysupport.atlassian.net for more information");
                listener.getLogger().println("-------------------------------------------------------------------------------");
                //return false;
                throw new AbortException();
            }
            catch(IOException e)
            {
                e.printStackTrace();
                listener.getLogger().println(pluginName + " : ERROR : " + e.getMessage());
                listener.getLogger().println(pluginName + " : Failed to upload test result file(s) to server. Please send these logs to qtmprofessional@qmetrysupport.atlassian.net for more information");
                listener.getLogger().println("-------------------------------------------------------------------------------");
                throw new AbortException();
            }
            catch(Exception e) {
                e.printStackTrace();
                listener.getLogger().println(pluginName + " : ERROR : " + e.getMessage());
                listener.getLogger().println(pluginName + " : Failed to upload test result file(s) to server. Please send these logs to qtmprofessional@qmetrysupport.atlassian.net for more information");
                listener.getLogger().println("-------------------------------------------------------------------------------");
                //return false;
                throw new AbortException();
            }
            finally
            {
                if(resultUtil.isOnSlave())
                {
                    if(resultUtil.getQtmFile() != null)
                    {
                        try
                        {
                            FileUtils.cleanDirectory(resultUtil.getQtmFile());
                        }
                        catch(IOException e)
                        {
                            listener.getLogger().println(pluginName + " : Copying task failed");
                        }
                        catch(IllegalArgumentException e)
                        {
                            listener.getLogger().println(pluginName + " : Copying task failed");
                        }
                    }
                }
            }
            
            listener.getLogger().println(pluginName + " : Successfully finished Post Build Action!");
            listener.getLogger().println("-------------------------------------------------------------------------------");
            //return true;
        }
        else
        {
            listener.getLogger().println(pluginName + ": Action 'Publish Build Result(s) to QMetry Test Management' is disabled");
        }
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
            items.add("Robot", "ROBOT");
            return items;
        }

        public ListBoxModel doFillIsMatchingRequiredItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("1 - Create a new Test Case or Test Case Version if no version matches with the one being uploaded","true");
            items.add("0 - Reuse already linked Test Case version OR Auto link the existing latest version of Test Case","false");
            return items;
        }

        public ListBoxModel doFillSkipWarningItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("0 - Test Case Import will be failed if the Test Case summary is more than 255 characters","0");
            items.add("1 - Test Case will be imported by truncating the Test Case summary to 255 characters","1");
            return items;
        }

        public ListBoxModel doFillAutomationHierarchyItems(@QueryParameter String automationFramework ) {
            ListBoxModel items = new ListBoxModel();
            if (automationFramework.equals("TESTNG"))
            {
                items.add("1 - Use result file's 'class' tag as TestCase and 'test-method' tag as TestStep","1");
                items.add("2 - Use result file's 'test-method' tag as TestCase","2");
                items.add("3 - Use result file's 'test' tag as TestCase and 'test-method' tag as TestStep", "3");
            }
            else if(automationFramework.equals("JUNIT"))
            {
                items.add("1 - Use result file's 'testcase' tag as TestStep and 'testsuite' tag as TestCase","1");
                items.add("2 - Create Single Testsuite and link all TestCases to that Testsuite ('testcase' tag will be treated as TestCase)","2");
                items.add("3 - Create Multiple Testsuites and then link their respective testCases in corresponding Testsuites ('testcase' tag will be treated as TestCase)","3");
            }
            return items;
        }
        
        public FormValidation doCheckQtmUrl(@QueryParameter String qtmUrl) throws IOException, ServletException {
            if (qtmUrl.length() <1) {
                return FormValidation.error("Please enter valid QTM API URL!");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckQtmAutomationApiKey(@QueryParameter String qtmAutomationApiKey)
        throws IOException, ServletException {
            if (qtmAutomationApiKey.length() < 1 ) {
                return FormValidation.error("Please enter valid QTM Automation API Key!");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckTestResultFilePath(@QueryParameter String testResultFilePath)
        throws IOException, ServletException {
            if (testResultFilePath == null || testResultFilePath.length() < 1) {
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
            if (project == null || project.length()< 1) {
                return FormValidation.error("Please provide project ID, Key or Name!");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckCycle(@QueryParameter String project, @QueryParameter String release, @QueryParameter String cycle)
        throws IOException, ServletException {
            if(cycle !=null && cycle.length()>0) {
                if (project == null || project.length()<1) {
                    return FormValidation.error("Please provide project ID, Key or Name!");
                } else if(release==null || release.length()<1) {
                    return FormValidation.error("Please provide release ID or Name!");
                }
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckRelease(@QueryParameter String project, @QueryParameter String release)
        throws IOException, ServletException {
            if (release!=null && release.length() > 0 && (project == null || project.length()<1)) {
                return FormValidation.error("Please provide project ID, Key or Name!");
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckBuildName(@QueryParameter String release, @QueryParameter String cycle, @QueryParameter String buildName) throws IOException, ServletException
        {
            if(buildName!=null && buildName.length()>0)
            {
                if(release==null || release.length()<1) {
                    return FormValidation.error("Please provide release ID or Name!");
                }
                else if(cycle==null || cycle.length()<1)
                {
                    return FormValidation.error("Please provide cycle ID or Name!");
                }
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