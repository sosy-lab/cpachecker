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
package common.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Preconditions;

/**
 * Immutable wrapper around a {@link Properties} instance, providing some
 * useful access helper methods.
 */
public class Configuration {

  private static final long serialVersionUID = -5910186668866464153L;
  
  /** Delimiters to create string arrays */
  private static final String DELIMS = "[;, ]+";

  private final Properties properties;
  
  private final String prefix;
  
  /**
   * Constructor for creating a Configuration with values set from a file.
   * Also allows for passing an optional map of settings overriding those from
   * the file.
   * @param fileName The complete path to the configuration file.
   * @param pOverrides A set of option values
   * @throws IOException If the file cannot be read.
   */
  public Configuration(String fileName, Map<String, String> pOverrides) throws IOException {
    properties = new Properties();
    prefix = "";
    
    loadFile(fileName);
    
    if (pOverrides != null) {
      properties.putAll(pOverrides);
    }
  }

  /**
   * Constructor for creating a Configuration with values set from a given map.
   * @param pValues The values this configuration should represent.
   */
  public Configuration(Map<String, String> pValues) {
    Preconditions.checkNotNull(pValues);
    properties = new Properties();
    prefix = "";

    properties.putAll(pValues);
  }
  
  /**
   * Constructor for creating Configuration from a given configuration.
   * Allows to pass a prefix. Options with the prefix will override those with
   * the same key but without the prefix in the new configuration.
   * @param pConfig An old configuration.
   * @param pPrefix A prefix for overriding configuration options.
   */
  public Configuration(Configuration pConfig, String pPrefix) {
    Preconditions.checkNotNull(pConfig);
    Preconditions.checkNotNull(pPrefix);
    
    properties = pConfig.properties;
    prefix = pPrefix.isEmpty() ? "" : pPrefix + ".";
  }
  
  private String getDefaultConfigFileName() {
    // TODO use resources for this?
    URL binDir = getClass().getProtectionDomain().getCodeSource().getLocation();
    
    File defaultFile = new File("..", "default.properties");
    defaultFile = new File(binDir.getPath(), defaultFile.getPath());
    return defaultFile.getPath();
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

    properties.load(new FileInputStream(fileName));
  }

  /**
   * @see Properties#getProperty(String)
   */
  public String getProperty(String key) {
    String result = properties.getProperty(prefix + key);
    
    if (result == null && !prefix.isEmpty()) {
      result = properties.getProperty(key);
    }
    return result;
  }

  /**
   * @see Properties#getProperty(String, String)
   */
  public String getProperty(String key, String defaultValue) {
    String result = getProperty(key);
    if (result == null) {
      result = defaultValue;
    }
    return result;
  }
  
  /**
   * If there are a number of properties for a given key, this method will split them
   * using {@link Configuration#DELIMS} and return the array of properties
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