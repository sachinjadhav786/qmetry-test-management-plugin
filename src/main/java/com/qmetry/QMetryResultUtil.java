package com.qmetry;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.InterruptedException;
import java.lang.NullPointerException;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.FilePath;
import hudson.model.FreeStyleProject;
import jenkins.model.Jenkins;
import hudson.model.Computer;
import hudson.model.Hudson.MasterComputer;
import hudson.slaves.SlaveComputer;

import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.TopLevelItem;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import java.util.List;	

public class QMetryResultUtil
{	
	public File prepareResultFile(String filePath, /*AbstractBuild build*/Run<?, ?> run, String pluginName, /*BuildListener*/TaskListener listener, FilePath workspace, String automationFramework) throws QMetryException {
		try {
			if(filePath.startsWith("/")) {
					filePath = filePath.substring(1, filePath.length());
			}
			if(workspace.toComputer().getNode().toComputer() instanceof SlaveComputer) {
				listener.getLogger().println(pluginName + " : build taking place on slave machine");
				FilePath slaveMachineWorkspace = workspace;
				if(!slaveMachineWorkspace.exists()) {
					throw new QMetryException("Failed to access slave machine workspace directory");
				}
				listener.getLogger().println(pluginName + " : slave machine workspace at '"+slaveMachineWorkspace.toURI().toString()+"'");
				
				//for last modified folder in QAS
				FilePath f = null;
				if(automationFramework.equals("QAS"))
				{
					listener.getLogger().println(pluginName + " : Getting latest test-result folder for QAS...");
					f = lastFileModified(slaveMachineWorkspace,filePath);
					filePath = filePath + "/" + f.getName();
					listener.getLogger().println(pluginName + " : Latest test-result folder : " + f.toString());
				}
				else
				{
					f = new FilePath(slaveMachineWorkspace, filePath);
					if(!f.exists())
					{
						throw new QMetryException("Can not find given file");
					}
				}
				
				FilePath masterMachineWorkspace = null;
				//for free style job
				if(run.getParent() instanceof AbstractProject)
				{
					AbstractProject project = (AbstractProject)run.getParent();
					if(project.getCustomWorkspace() != null && project.getCustomWorkspace().length()>0 ) 
					{
						masterMachineWorkspace = new FilePath(new File(project.getCustomWorkspace()));
					} 
					else 
					{
						masterMachineWorkspace = Jenkins.getInstance().getWorkspaceFor((TopLevelItem)project);
					}
				}
				//for pipeline job
				else if(run.getParent() instanceof WorkflowJob)
				{
					//listener.getLogger().println("[DEBUG] : instance of WorkFlowJob");
					WorkflowJob project = (WorkflowJob)run.getParent();
					masterMachineWorkspace = Jenkins.getInstance().getWorkspaceFor((TopLevelItem)project);
				}
			
				if(masterMachineWorkspace==null) {
					throw new QMetryException("Failed to access master machine workspace directory");
				} else if(!masterMachineWorkspace.exists()) {
					//create directory if not yet exists
					masterMachineWorkspace.mkdirs();
				}
				listener.getLogger().println(pluginName + " : master machine workspace at '"+masterMachineWorkspace.toURI().toString()+"'");
				
				if(!filePath.endsWith("/") && !filePath.endsWith("json") && !filePath.endsWith("xml")) {
					filePath = filePath + "/";
				}
				
				int fileCount = slaveMachineWorkspace.copyRecursiveTo(filePath, masterMachineWorkspace);
				if(fileCount<1) {
					throw new QMetryException("Failed to copy result file(s) from slave machine!");
				}
				listener.getLogger().println(pluginName + " : "+fileCount+" result file(s) copied from slave to master machine");
				File finalResultFile = new File(masterMachineWorkspace.toURI());
				finalResultFile = new File(finalResultFile, filePath);
				if(!finalResultFile.exists()) {
					throw new QMetryException("Failed to read result file(s) on master machine");
				}
				return finalResultFile;
			}
			else if(workspace.toComputer().getNode().toComputer() instanceof MasterComputer)
			{
				listener.getLogger().println(pluginName + " : build taking place on master machine");
				//File masterWorkspace = new File(build.getProject().getWorkspace().toURI());
				File masterWorkspace = new File(workspace.toString());
				
				//File resultFile = new File(masterWorkspace, filePath);
				
				FilePath resultFilePath = null;
				if(automationFramework.equals("QAS"))
				{
					//Getting latest testresult files for QAS
					listener.getLogger().println("QMetry for JIRA : Getting latest test-result folder for QAS...");
					resultFilePath = lastFileModified(new FilePath(masterWorkspace),filePath);
					listener.getLogger().println("QMetry for JIRA : Latest test-result folder : " + resultFilePath.toString());
					//listener.getLogger().println("[DEBUG]: final path : "+resultFilePath.toString());
				}
				else
				{
					resultFilePath = new FilePath(new File(masterWorkspace, filePath));
					if(!resultFilePath.exists())
					{
						throw new QMetryException("Can not find given file");
					}
					//listener.getLogger().println("[DEBUG]: final path : "+resultFilePath.toString());
				}
				
				File resultFile = new File(resultFilePath.toString());
				if(!resultFile.exists()) {
					throw new QMetryException("Result file(s) not found : '"+filePath+"'");
				}
				return resultFile;
			}
			else throw new QMetryException("Machine instance neither a master nor a slave");
		}
		/*catch(QMetryException e) {
			throw new QMetryException(e.getMessage());
		} */
		catch (NullPointerException e) {
			listener.getLogger().println(pluginName+" : ERROR : "+e.toString());
			throw new QMetryException("failed to read result file(s) at location '"+filePath+"'");
		}
		catch(IOException e) {
			listener.getLogger().println(pluginName+" : ERROR : "+e.toString());
			throw new QMetryException("failed to read result file(s) at location '"+filePath+"'");
		}
		catch(InterruptedException e) {
			listener.getLogger().println(pluginName+" : ERROR : "+e.toString());
			throw new QMetryException("failed to read result file(s) at location '"+filePath+"'");
		}
	}

	public void uploadResultFilesToQMetry(/*AbstractBuild build*/Run<?, ?> run,
											String pluginName,
											/*BuildListener*/TaskListener listener,
											FilePath workspace,
											String url,
											String key,
											String resultFilePath,
											String testSuiteName,
											String automationFramework,
											String buildName,
											String platformName,
											String project,
											String release,
											String cycle) throws QMetryException
	{
		try 
		{
			File resultFile = prepareResultFile(resultFilePath, /*build*/run, pluginName, listener,workspace, automationFramework);
						
			QMetryConnection conn = new QMetryConnection(url, key);
			
			if(automationFramework.equals("QAS"))
			{
				listener.getLogger().println(pluginName + " : Reading result files from path '"+resultFile.getAbsolutePath()+"'");
				String filepath = null;
				if (resultFile.isDirectory()){
					filepath = CreateZip.createZip(resultFile.getAbsolutePath(),automationFramework);
					listener.getLogger().println(pluginName + " : Zip file path '"+filepath+"'");
				}else if(resultFile.isFile()){
					String fileExtensionJson=getExtensionOfFile(resultFile);
					String extension = "zip";
					if(extension.equalsIgnoreCase(fileExtensionJson)){
						filepath = resultFile.getAbsolutePath();
					}else 
						listener.getLogger().println(pluginName + " : Upload .Zip file or configure directory to upload " + automationFramework +" results");
				}
					
				if(filepath == null)
					throw new QMetryException("Results' directory of type "+automationFramework+" not found in given directory '"+resultFile.getAbsolutePath()+"'");
				// zipUtils.zipDirectory(latest_dir, "qmetry_result.zip", pluginName, listener);
				// File zipArchive = new File(latest_dir, "qmetry_result.zip");
				// if(zipArchive==null || !zipArchive.exists())
				// 	throw new QMetryException("Failed to create zip archive for QAS results at directory '"+latest_dir.getAbsolutePath()+"'");
				conn.uploadFileToTestSuite(filepath, testSuiteName, automationFramework, buildName, platformName, project, release, cycle, pluginName, listener);
			}
			else if (resultFile.isDirectory()) 
			{
				String extension = null;
				if(automationFramework.equals("CUCUMBER") || automationFramework.equals("QAS"))
				{
					extension = ".json";
				}
				else
				{
					extension = ".xml";
				}
				listener.getLogger().println(pluginName + " : Reading result files from Directory '"+resultFile.getAbsolutePath()+"'");
				File[] listOfFiles = resultFile.listFiles();
				if(listOfFiles == null)
					throw new QMetryException(pluginName + " : No result file(s) found in directory '"+resultFile.getAbsolutePath()+"'");
				for (File file : listOfFiles) 
				{
					if (file.isFile() && (file.getName().endsWith(extension)))
					{
						listener.getLogger().println(pluginName + " : Result File Found '" + file.getName() + "'");
						try 
						{
							listener.getLogger().println(pluginName + " : Uploading result file...");
							conn.uploadFileToTestSuite(file.getAbsolutePath(), testSuiteName,
																			automationFramework, buildName, platformName, project, release, cycle, pluginName, listener);
							listener.getLogger().println(pluginName + " : Result file successfully uploaded!");
						} 
						catch (QMetryException e) 
						{
							listener.getLogger().println(pluginName + " : Failed to upload Result file!");
						}
					}
				}
			}
			else  if(resultFile.isFile())
			{
				listener.getLogger().println(pluginName + " : Reading result file(s) at location '"+resultFile.getAbsolutePath()+"'");
				conn.uploadFileToTestSuite(resultFile.getAbsolutePath(), testSuiteName, automationFramework, buildName, platformName, project, release, cycle, pluginName, listener);
			}
			else
			{
				throw new QMetryException("Failed to read result file(s) at location '"+resultFile.getAbsolutePath()+"'");
			}
		}catch (IOException e) {
			listener.getLogger().println(pluginName+" : ERROR : "+e.toString());
			throw new QMetryException("Failed to upload result file(s) to server");
		}  catch (NullPointerException e) {
			listener.getLogger().println(pluginName+" : ERROR : "+e.toString());
			throw new QMetryException("Failed to upload result file(s) to server");
		} /*catch (QMetryException e) {
			listener.getLogger().println(pluginName+" : ERROR : "+e.toString());
			throw new QMetryException(e.getMessage());
		}*/
	}

	private static String getExtensionOfFile(File file)
	{
		String fileExtension="";
		// Get file Name first
		String fileName=file.getName();
		
		// If fileName do not contain "." or starts with "." then it is not a valid file
		if(fileName.contains(".") && fileName.lastIndexOf(".")!= 0)
		{
			fileExtension=fileName.substring(fileName.lastIndexOf(".")+1);
		}
		
		return fileExtension;
	}
	
	public static FilePath lastFileModified(FilePath base, String path) throws IOException,InterruptedException,QMetryException
	{
		FilePath slaveDir = new FilePath(base,path); 
		if(!slaveDir.exists())
		{
			throw new QMetryException("Can not find given file");
		}
		List<FilePath> files = slaveDir.listDirectories();
		long lastMod = Long.MIN_VALUE;
		FilePath choice = null;
		if(files!=null)
		{
			for (FilePath file : files) 
			{
				if(file.isDirectory() && !(file.getName()).equals("surefire"))
				{
					if (file.lastModified() > lastMod) 
					{
						choice = file;
						lastMod = file.lastModified();
					}
				}
			}
		}
		return choice;
	}
}