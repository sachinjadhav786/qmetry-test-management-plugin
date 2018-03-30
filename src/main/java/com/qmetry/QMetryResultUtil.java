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

public class QMetryResultUtil
{	
	public File prepareResultFile(String filePath, AbstractBuild build, String pluginName, BuildListener listener) throws QMetryException {
		try {
			if(Computer.currentComputer() instanceof SlaveComputer) {
				listener.getLogger().println(pluginName + " : build taking place on slave machine");
				FilePath slaveMachineWorkspace = build.getProject().getWorkspace();
				if(!slaveMachineWorkspace.exists()) {
					throw new QMetryException("Failed to access slave machine workspace directory");
				}
				listener.getLogger().println(pluginName + " : slave machine workspace at '"+slaveMachineWorkspace.toURI().toString()+"'");
				if(!(build.getProject() instanceof FreeStyleProject)) {
					throw new QMetryException("Not a Freestyle project. Failed to fetch workspace directory on master machine");
				}
				FilePath masterMachineWorkspace  = null;
				if(build.getProject().getCustomWorkspace() != null && build.getProject().getCustomWorkspace().length()>0 ) {
					masterMachineWorkspace = new FilePath(new File(build.getProject().getCustomWorkspace()));
				} 
				else {
					masterMachineWorkspace = Jenkins.getInstance().getWorkspaceFor((FreeStyleProject)build.getProject());
				}
				if(masterMachineWorkspace==null) {
					throw new QMetryException("Failed to access master machine workspace directory");
				} else if(!masterMachineWorkspace.exists()) {
					//create directory if not yet exists
					masterMachineWorkspace.mkdirs();
				}
				listener.getLogger().println(pluginName + " : master machine workspace at '"+masterMachineWorkspace.toURI().toString()+"'");
				if(filePath.startsWith("/")) {
					filePath = filePath.substring(1, filePath.length());
				}
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
			else if(Computer.currentComputer() instanceof MasterComputer) {
				listener.getLogger().println(pluginName + " : build taking place on master machine");
				File masterWorkspace = new File(build.getProject().getWorkspace().toURI());
				File resultFile = new File(masterWorkspace, filePath);
				if(!resultFile.exists()) {
					throw new QMetryException("Result file(s) not found : '"+filePath+"'");
				}
				return resultFile;
			}
			else throw new QMetryException("Machine instance neither a master nor a slave");
		}
		catch(QMetryException e) {
			throw new QMetryException(e.getMessage());
		} 
		catch (NullPointerException e) {
			throw new QMetryException("failed to read result file(s) at location '"+filePath+"'");
		}
		catch(IOException e) {
			throw new QMetryException("failed to read result file(s) at location '"+filePath+"'");
		}
		catch(InterruptedException e) {
			throw new QMetryException("failed to read result file(s) at location '"+filePath+"'");
		}
	}

	public void uploadResultFilesToQMetry(AbstractBuild build,
											String pluginName,
											BuildListener listener,
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
			File resultFile = prepareResultFile(resultFilePath, build, pluginName, listener);
			String extension = null;
			if(automationFramework.equals("CUCUMBER") || automationFramework.equals("QAS"))
			{
				extension = ".json";
			}
			else
			{
				extension = ".xml";
			}
			
			QMetryConnection conn = new QMetryConnection(url, key);
			
			if(automationFramework.equals("QAS"))
			{
				File dirs[] = resultFile.listFiles(new FilenameFilter() {
					public boolean accept(File directory, String fileName) {
						return (directory.isDirectory() && fileName.length()==20);
					}
				});
				
				if(dirs==null) {
					throw new QMetryException("Could not find result file(s) at given path!");
				}
				
				Long last_mod = Long.valueOf(0);
				File latest_dir = null;
				
				for(File adir : dirs)
				{				
					if (adir.isDirectory() && adir.lastModified() > last_mod) 
					{
						latest_dir = adir;
						last_mod = adir.lastModified();
					}
				}
				ZipUtils zipUtils = new ZipUtils(extension);
				if(latest_dir == null)
					throw new QMetryException("Results' directory of type QAS not found in given directory '"+resultFile.getAbsolutePath()+"'");
				zipUtils.zipDirectory(latest_dir, "qmetry_result.zip", pluginName, listener);
				File zipArchive = new File(latest_dir, "qmetry_result.zip");
				if(zipArchive==null || !zipArchive.exists())
					throw new QMetryException("Failed to create zip archive for QAS results at directory '"+latest_dir.getAbsolutePath()+"'");
				conn.uploadFileToTestSuite(zipArchive.getAbsolutePath(), testSuiteName, automationFramework, buildName, platformName, project, release, cycle, pluginName, listener);
			}
			else if (resultFile.isDirectory()) 
			{
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
		} catch (NullPointerException e) {
			throw new QMetryException("Failed to upload result file(s) to server");
		} catch (QMetryException e) {
			throw new QMetryException(e.getMessage());
		}
	}
}