package com.juvenxu.portableconfig.traverser;

import com.juvenxu.portableconfig.AbstractTraverser;
import com.juvenxu.portableconfig.ContentFilter;
import com.juvenxu.portableconfig.model.ConfigFile;
import com.juvenxu.portableconfig.model.PortableConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.component.annotations.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * @author juven
 */
@Component(role = AbstractTraverser.class, hint = "jar")
public class JarTraverser extends AbstractTraverser
{

  @Override
  public void traverse(PortableConfig portableConfig, File jar) throws IOException
  {
    List<Integer> checklength=new ArrayList<Integer>();
    JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jar));
    File tmpJar = File.createTempFile(Long.toString(System.nanoTime()), ".jar");
    getLogger().info("Tmp file: " + tmpJar.getAbsolutePath());
    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpJar));

    byte[] buffer = new byte[1024];

    getLogger().info("=== Starting to find the to be replaced file");
    int configfilefoundCount = 0; // how many config file were found in this jar/war?
    while (true)
    {
      JarEntry jarEntry = jarInputStream.getNextJarEntry();
      if (jarEntry == null)
      {
        break;
      }

      getLogger().debug(jarEntry.getName());

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();



      while (true)
      {
        int length = jarInputStream.read(buffer, 0, buffer.length);

        if (length <= 0)
        {
          break;
        }

        byteArrayOutputStream.write(buffer, 0, length);

      }

      boolean filtered = false;


      int length=0;
      for (ConfigFile configFile : portableConfig.getConfigFiles()) {
        if (!configFile.getPath().equals(jarEntry.getName())) {
          length++;
          continue;
        }
        if (configFile.getPath().equals(jarEntry.getName())) {
          checklength.add(length);
          length++;;
        }

        configfilefoundCount ++;
        getLogger().info("Found Config file : " + configFile.getPath());

        if (!hasContentFilter(configFile.getType())) {
          continue;
        }

        getLogger().info(String.format("Replacing: %s!%s", jar.getName(), jarEntry.getName()));

        JarEntry filteredJarEntry = new JarEntry(configFile.getPath());
        jarOutputStream.putNextEntry(filteredJarEntry);

        ContentFilter contentFilter = getContentFilter(configFile.getType());

        contentFilter.filter(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), jarOutputStream, configFile.getReplaces());

        for(int i=0;i<contentFilter.getReplacedEntry().size();i++) {
          getLogger().info(String.format(contentFilter.getReplacedEntry().get(i)));
        }


        filtered = true;
      }





      if (!filtered)
      {
        jarOutputStream.putNextEntry(jarEntry);
        byteArrayOutputStream.writeTo(jarOutputStream);
      }
    }
    List<String> checklist=new ArrayList<String>();
    for (ConfigFile configFile : portableConfig.getConfigFiles()) {
      checklist.add(configFile.getPath());
    }
    for(Integer j:checklength){
      checklist.remove((int)j);
    }
  if(checklist.size()>0){
    for (String path : checklist) {
                 System.err.println("未找到修改数据："+path);
    }
    }


    //compare the found file and config file number
    if(portableConfig.getConfigFiles().size() !=configfilefoundCount ){
      getLogger().warn("Defined config files :"  +  portableConfig.getConfigFiles().size() + " found file : " +configfilefoundCount   );

    }
    else
      getLogger().info("Defined config files :"  +  portableConfig.getConfigFiles().size() + " found file : " +configfilefoundCount   );

    IOUtils.closeQuietly(jarInputStream);
    IOUtils.closeQuietly(jarOutputStream);

    getLogger().info("Replacing: " + jar.getAbsolutePath() + " with: " + tmpJar.getAbsolutePath());
    FileUtils.copyFile(tmpJar, jar);
    FileUtils.forceDeleteOnExit(tmpJar);
  }
}
