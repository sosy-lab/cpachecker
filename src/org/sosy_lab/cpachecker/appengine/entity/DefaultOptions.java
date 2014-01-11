/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.appengine.entity;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;

/**
 * Represents the options that may be set by a client.
 * Setting options not defined by this class might cause the application
 * to crash or not behave as intended.
 *
 * The class also provides static methods to retrieve the following:
 * - default options: All allowed options and their default value
 * - immutable options: Options that will always precede any other options
 * - specifications: A list of available specifications
 * - configurations: A list of available configurations
 */
public class DefaultOptions {

  private static final Map<String, String> allowedOptions = new HashMap<>();
  private Map<String, String> usedOptions = new HashMap<>();

  public DefaultOptions() {
    allowedOptions.put("analysis.machineModel", "Linux32");
//    allowedOptions.put("parser.usePreprocessor", "false");
    allowedOptions.put("output.disable", "false");
    allowedOptions.put("statistics.export", "true");
    allowedOptions.put("log.usedOptions.export", "false");
    allowedOptions.put("log.level", "OFF");
    allowedOptions.put("log.truncateSize", "10000");
  }

  /**
   * Sets an option to be used if it is allowed
   * and if the given value differs from the default value.
   *
   * @param key The option to set
   * @param value The value to set
   * @return True, if the option will be used, false otherwise.
   */
  public boolean setOption(String key, String value) {
    if (!allowedOptions.containsKey(key)) {
      return false;
    }

    if (!allowedOptions.get(key).equals(value)) {
      usedOptions.put(key, value);
      return true;
    }

    return false;
  }

  /**
   * Indicates whether setting the given option is allowed.
   *
   * @param option The option to check
   * @return True, if the option may be set, false otherwise.
   */
  public boolean isAllowed(String option) {
    return allowedOptions.containsKey(option);
  }

  /**
   * Returns the default value for a given key.
   *
   * @param option The option for which the default will be retrieved
   * @return The default value. null if the option does not exist.
   */
  public String getDefaultValue(String option) {
    return allowedOptions.get(option);
  }

  public Map<String, String> getUsedOptions() {
    return usedOptions;
  }

  public String getUsedOption(String option) {
    return usedOptions.get(option);
  }

  public Map<String, String> getAllowedOptions() {
    return allowedOptions;
  }

  /**
   * Returns the allowed options and their default values.
   */
  public static Map<String, String> getDefaultOptions() {
    return allowedOptions;
  }

  public static String getDefault(String key) {
    return allowedOptions.get(key);
  }

  /**
   * Returns options that will always be set and cannot be changed.
   *
   * @return The immutable options
   * @throws IOException If the options cannot be retrieved.
   */
  public static Map<String, String> getImmutableOptions() throws IOException {
    Map<String, String> options = new HashMap<>();
    Properties properties = new Properties();
    properties.load(Paths.get("WEB-INF", "default-options.properties").asByteSource().openStream());

    for (String key : properties.stringPropertyNames()) {
      options.put(key, properties.getProperty(key));
    }

    return options;
  }

  /**
   * Returns an array of all available specification files.
   *
   * @return The available specification files.
   */
  public static File[] getSpecifications() {
    Path specificationDir = Paths.get("WEB-INF/specifications");
    return specificationDir.toFile().listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File pDir, String pName) {
        // exclude directories from the list
        return pName.endsWith(".spc");
      }
    });
  }

  /**
   * Returns an array of all available configuration files.
   *
   * @return The available configuration files.
   */
  public static File[] getConfigurations() {
    Path configurationDir = Paths.get("WEB-INF/configurations");
    return configurationDir.toFile().listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File pDir, String pName) {
        // exclude directories from the list
        return pName.endsWith(".properties");
      }
    });
  }
}
