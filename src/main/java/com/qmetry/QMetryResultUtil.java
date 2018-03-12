package com.qmetry;

import java.io.File;
import java.io.FilenameFilter;
import hudson.model.BuildListener;

public class QMetryResultUtil
{	
	public void uploadResultFilesToQMetry(String pluginName,
											BuildListener listener,
											String url,
											String key,
											File resultFile,
											String testSuiteName,
											String automationFramework,
											String buildName,
											String platformName,
											String project,
											String release,
											String cycle) throws QMetryException
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
		
		QMetryConnection conn = new QMetryConnection(url, key);
		
		if(automationFramework.equals("QAS"))
		{
			File dirs[] = resultFile.listFiles(new FilenameFilter() {
				public boolean accept(File directory, String fileName) {
					return (directory.isDirectory() && fileName.length()==20);
				}
			});
			
			Long last_mod = new Long(0);
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
			listener.getLogger().println(pluginName + " : Reading result file '"+resultFile.getAbsolutePath()+"'");
			listener.getLogger().println(pluginName + " : Uploading result file...");
			conn.uploadFileToTestSuite(resultFile.getAbsolutePath(), testSuiteName, automationFramework, buildName, platformName, project, release, cycle, pluginName, listener);
			listener.getLogger().println(pluginName + " : Result file successfully uploaded!");
		}
		else
		{
			throw new QMetryException("Failed to read result file(s) at location '"+resultFile.getAbsolutePath()+"'");
		}
	}
}