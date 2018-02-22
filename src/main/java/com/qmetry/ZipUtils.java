package com.qmetry;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import hudson.model.BuildListener;

public class ZipUtils 
{
	private static String extension;
	
	public ZipUtils(String extension)
	{
		this.extension = extension;
	}
	
	public static void zipDirectory(File sourceDir, String zipFileName, String pluginName, BuildListener listener) throws QMetryException 
	{
		try
		{
			listener.getLogger().println(pluginName + " : Creating zip archive at directory '"+sourceDir+"'");
			FileOutputStream fout = new FileOutputStream((sourceDir.getAbsolutePath() + "/" + zipFileName), false);
			ZipOutputStream zout = new ZipOutputStream(fout);
			zipSubDirectory("", sourceDir, zout, pluginName, listener);
			zout.close();
			listener.getLogger().println(pluginName + " : Zip file created successfully '"+zipFileName+"'");
		}
		catch(IOException e)
		{
			throw new QMetryException("Failed to create zip archive in directory '"+sourceDir+"'");
		}
	}

	private static void zipSubDirectory(String basePath, File dir, ZipOutputStream zout, String pluginName, BuildListener listener) throws IOException 
	{
		byte[] buffer = new byte[4096];
		File[] files = dir.listFiles();
		for (File file : files) 
		{
			if (file.isDirectory()) 
			{
				String path = basePath + file.getName() + "/";
				zout.putNextEntry(new ZipEntry(path));
				zipSubDirectory(path, file, zout, pluginName, listener);
				zout.closeEntry();
			}
			else if(file.getName().endsWith(extension))
			{
				listener.getLogger().println(pluginName + " : adding result file to zip archive : '"+file.getAbsolutePath()+"'");
				FileInputStream fin = new FileInputStream(file);
				zout.putNextEntry(new ZipEntry(basePath + file.getName()));
				int length;
				while ((length = fin.read(buffer)) > 0) 
				{
					zout.write(buffer, 0, length);
				}
				zout.closeEntry();
				fin.close();
			}
		}
	}
}
