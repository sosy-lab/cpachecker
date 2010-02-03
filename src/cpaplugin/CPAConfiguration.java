/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpaplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;


/**
 * CPA Checker properties file. Processes the properties file and save them as strings.
 * If -config is not among program arguments, reads properties file from a default
 * location. If properties are modified via command line arguments, they are processed
 * and related properties keys are modified by this class.
 * @author erkan
 *
 */
public class CPAConfiguration extends Properties {

  private static final long serialVersionUID = -5910186668866464153L;
  /** Delimiters to create string arrays */
  private static final String DELIMS = "[;, ]+";

  public CPAConfiguration(String fileName) throws IOException {
    loadFile(fileName);
  }

  private String getDefaultConfigFileName() {
    // TODO use resources for this?
    URL binDir = getClass().getProtectionDomain().getCodeSource().getLocation();
    String binDirString = binDir.getPath();
    int index = binDirString.lastIndexOf(File.separatorChar);
    binDirString = binDirString.substring(0, index);
    return binDirString + ".." + File.separatorChar + "default.properties";
  }

  /**
   * Load the file as property file see {@link Properties}
   * @param fileName name of the property file
   * @return true if file is loaded successfully
   */
  private void loadFile(String fileName) throws IOException {
    if (fileName == null || fileName.isEmpty()) {
      fileName = getDefaultConfigFileName();
    }

    load(new FileInputStream(fileName));

    //section for setting default values
    //booleans are automatically set to false if no value is given in the config file
    if (this.getProperty("analysis.traversal") == null) {
      this.setProperty("analysis.traversal", "dfs");
    }

  }

  /**
   * If there are a number of properties for a given key, this method will split them
   * using {@link CPAConfiguration#DELIMS} and return the array of properties
   * @param key the key for the property
   * @return array of properties
   */
  public String[] getPropertiesArray(String key){
    String s = getProperty(key);
    return (s != null) ? s.split(DELIMS) : null;
  }


  /**
   * A shortcut for properties which has only true, false value
   * @param key the key for the property
   * @return the boolean value of the property, if the key is not present in
   * the properties file false
   */
  public boolean getBooleanValue(String key){
    return Boolean.valueOf(getProperty(key));
  }
}