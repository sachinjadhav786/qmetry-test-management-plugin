package com.qmetry;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.InterruptedException;

import hudson.FilePath;
import jenkins.model.Jenkins;
import hudson.model.Computer;
import hudson.slaves.SlaveComputer;
import hudson.model.Node;
import hudson.model.Hudson.MasterComputer;

import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.model.AbstractProject;
import hudson.model.TopLevelItem;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import java.util.List;	

public class QMetryResultUtil
{	

	private boolean onSlave = false;
	private File qtmFile;

	public boolean isOnSlave()
	{
		return onSlave;
	}

	public File getQtmFile()
	{
		return qtmFile;
	}

	public File prepareResultFile(String filePath, /*AbstractBuild build*/Run<?, ?> run, String pluginName, /*BuildListener*/TaskListener listener, FilePath workspace, String automationFramework) throws QMetryException, IOException, InterruptedException {
		//try {
			onSlave = false;
			qtmFile = null;

			if(filePath.startsWith("/")) {
					filePath = filePath.substring(1, filePath.length());
			}
			//listener.getLogger().println("DEBUG : filePath : " + filePath);
			if(filePath.endsWith("*.xml"))
			{
				filePath = filePath.substring(0,filePath.length()-5);
				//listener.getLogger().println("DEBUG : filePath : " + filePath);
			}
			else if(filePath.endsWith("*.json"))
			{
				filePath = filePath.substring(0,filePath.length()-6);
				//listener.getLogger().println("DEBUG : filePath : " + filePath);
			}
			if(workspace!=null)
			{
				Computer comp = workspace.toComputer();
				if(comp!=null)
				{
					Node node = comp.getNode();
					if(node!=null)
					{
						Computer comp1 = node.toComputer();
						if(comp1!=null)
						{
							if(comp1 instanceof SlaveComputer) {
								onSlave = true;
								listener.getLogger().println(pluginName + " : build taking place on slave machine");
								FilePath slaveMachineWorkspace = workspace;
								if(!slaveMachineWorkspace.exists()) {
									throw new QMetryException("Failed to access slave machine workspace directory");
								}
								
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
										throw new QMetryException("Can not find given file : " + f);
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
								masterMachineWorkspace = new FilePath(masterMachineWorkspace,"QTM");
								
								if(!filePath.endsWith("/") && !filePath.endsWith("json") && !filePath.endsWith("xml")) {
									filePath = filePath + "/";
								}
								
								int fileCount = slaveMachineWorkspace.copyRecursiveTo(filePath, masterMachineWorkspace);
								if(fileCount<1) {
									throw new QMetryException("Failed to copy result file(s) from slave machine!");
								}
								File finalResultFile = new File(masterMachineWorkspace.toURI());
								qtmFile = finalResultFile;
								finalResultFile = new File(finalResultFile, filePath);
								if(!finalResultFile.exists()) {
									throw new QMetryException("Failed to read result file(s) on master machine");
								}
								return finalResultFile;
							}
							else if(comp1 instanceof MasterComputer)
							{
								onSlave = false;
								listener.getLogger().println(pluginName + " : build taking place on master machine");
								//File masterWorkspace = new File(build.getProject().getWorkspace().toURI());
								File masterWorkspace = new File(workspace.toString());
								
								//File resultFile = new File(masterWorkspace, filePath);
								
								FilePath resultFilePath = null;
								if(automationFramework.equals("QAS"))
								{
									//Getting latest testresult files for QAS
									listener.getLogger().println(pluginName + " : Getting latest test-result folder for QAS...");
									resultFilePath = lastFileModified(new FilePath(masterWorkspace),filePath);
									listener.getLogger().println(pluginName + " : Latest test-result folder : " + resultFilePath.toString());
									//listener.getLogger().println("[DEBUG]: final path : "+resultFilePath.toString());
								}
								else
								{
									resultFilePath = new FilePath(new File(masterWorkspace, filePath));
									if(!resultFilePath.exists())
									{
										throw new QMetryException("Can not find given file : " + resultFilePath);
									}
									//listener.getLogger().println("[DEBUG]: final path : "+resultFilePath.toString());
								}
								
								File resultFile = new File(resultFilePath.toString());
								if(!resultFile.exists()) {
									throw new QMetryException("Result file(s) not found : '"+filePath+"'");
								}
								return resultFile;
							}
						}
					}
				}
			}
			throw new QMetryException("Machine instance neither a master nor a slave");
		/*}
		catch(QMetryException e) {
			throw new QMetryException(e.getMessage());
		} */
		/*catch (NullPointerException e) {
			listener.getLogger().println("[DEBUG] : " + e.getMessage());
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
		}*/
	}

	public void uploadResultFilesToQMetry(/*AbstractBuild build*/Run<?, ?> run,
											String pluginName,
											/*BuildListener*/TaskListener listener,
											FilePath workspace,
											String url,
											String key,
											String proxyUrl,
											String resultFilePath,
											String testSuiteName,
											String testSName,
											String automationFramework,
											String automationHierarchy,
											String buildName,
											String platformName,
											String project,
											String release,
											String cycle,
											int buildnumber, 
											String testCaseField, 
											String testSuiteField, 
											String skipWarning) throws QMetryException, IOException, InterruptedException
	{
		//try 
		//{
			File resultFile = prepareResultFile(resultFilePath, /*build*/run, pluginName, listener, workspace, automationFramework);
						
			QMetryConnection conn = new QMetryConnection(url, key);
			
			if(automationFramework.equals("QAS") || resultFile.isDirectory())
			{
				listener.getLogger().println(pluginName + " : Reading result files from path '"+resultFile.getAbsolutePath()+"'");
				String filepath = null;
				if (resultFile.isDirectory())
				{
					filepath = CreateZip.createZip(resultFile.getAbsolutePath(),automationFramework);
					listener.getLogger().println(pluginName + " : Zip file path '"+filepath+"'");
				}
				else if(resultFile.isFile())
				{
					String fileExtensionJson=getExtensionOfFile(resultFile);
					String extension = "zip";
					if(extension.equalsIgnoreCase(fileExtensionJson))
					{
						filepath = resultFile.getAbsolutePath();
					}
					else
					{
						listener.getLogger().println(pluginName + " : Upload .Zip file or configure directory to upload " + automationFramework +" results");
					}
				}
					
				if(filepath == null)
				{
					throw new QMetryException("Results' directory of type "+automationFramework+" not found in given directory '"+resultFile.getAbsolutePath()+"'");
				}
				conn.uploadFileToTestSuite(filepath, testSuiteName, testSName, automationFramework, automationHierarchy, buildName, platformName, project, release, cycle, pluginName, listener, buildnumber, proxyUrl, testCaseField, testSuiteField, skipWarning);
			}
			else if (resultFilePath.endsWith("*.xml") || resultFilePath.endsWith("*.json"))
			{
				FileFilter XML_FILE_FILTER = new FileFilter() {
					public boolean accept(File file) {
						return file.getName().toLowerCase().endsWith(".xml");
					}
				};
	
				FileFilter JSON_FILE_FILTER = new FileFilter() {
					public boolean accept(File file) {
						return file.getName().toLowerCase().endsWith(".json");
					}
				};

				File[] filelist = null;
				if(automationFramework.equals("JUNIT") || automationFramework.equals("TESTNG") || automationFramework.equals("HPUFT") || automationFramework.equals("ROBOT"))
				{
					if(resultFilePath.endsWith("*.json"))
					{
						throw new QMetryException("Cannot upload json files when format is : " + automationFramework);
					}
					filelist = resultFile.listFiles(XML_FILE_FILTER);
				}
				else if(automationFramework.equals("CUCUMBER"))
				{
					if(resultFilePath.endsWith("*.xml"))
					{
						throw new QMetryException("Cannot upload xml files when format is : " + automationFramework);
					}
					filelist = resultFile.listFiles(JSON_FILE_FILTER);
				}
				if(filelist == null)
				{
					throw new QMetryException("Cannot find files of proper format in directory : " + resultFile);
				}
				else
				{
					for(File f: filelist)
					{
						listener.getLogger().println(pluginName + " : Uploading file : " + f.getAbsolutePath() + "...");
						conn.uploadFileToTestSuite(f.getAbsolutePath(), testSuiteName, testSName, automationFramework, automationHierarchy, buildName, platformName, project, release, cycle, pluginName, listener, buildnumber, proxyUrl, testCaseField, testSuiteField, skipWarning);
					}
				}
			}
			else if(resultFile.isFile())
			{
				String rPath = resultFile.getAbsolutePath();
				if(rPath.endsWith(".xml") && !(automationFramework.equals("JUNIT") || automationFramework.equals("HPUFT") || automationFramework.equals("TESTNG") || automationFramework.equals("ROBOT")))
				{
					throw new QMetryException("Cannot upload xml file when format is " + automationFramework);
				}
				else if(rPath.endsWith(".json") && !automationFramework.equals("CUCUMBER"))
				{
					throw new QMetryException("Cannot upload json file when format is " + automationFramework);
				}
				listener.getLogger().println(pluginName + " : Reading result files from path '"+resultFile.getAbsolutePath()+"'");
				conn.uploadFileToTestSuite(rPath, testSuiteName, testSName, automationFramework, automationHierarchy, buildName, platformName, project, release, cycle, pluginName, listener, buildnumber, proxyUrl, testCaseField, testSuiteField, skipWarning);
			}
			else 
			{
				throw new QMetryException("Failed to read result file(s) at location '"+resultFile.getAbsolutePath()+"'");
			}
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
			throw new QMetryException("Cannot find given file : " + slaveDir);
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
		if (choice == null)
		{
			throw new QMetryException("Cannot find latest test-result files for QAS");
		}
		return choice;
	}
}