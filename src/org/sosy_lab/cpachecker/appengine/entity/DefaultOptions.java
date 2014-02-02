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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

  private static Map<String, String> allowedOptions = new HashMap<>();
  private static List<String> unsupportedConfigurations = new ArrayList<>();
  private Map<String, String> usedOptions = new HashMap<>();

  static {
    allowedOptions.put("analysis.machineModel", "Linux32");
    allowedOptions.put("output.disable", "false");
    allowedOptions.put("statistics.export", "true");
    allowedOptions.put("log.usedOptions.export", "false");
    allowedOptions.put("log.level", "OFF");
    allowedOptions.put("limits.time.wall", "540s"); // 9 minutes

    unsupportedConfigurations.add("chc.properties");
    unsupportedConfigurations.add("lddAnalysis.properties");
    unsupportedConfigurations.add("octagonAnalysis.properties");
    unsupportedConfigurations.add("separationlogic.properties");
    unsupportedConfigurations.add("explicitAnalysis-java-with-RTT.properties");
    unsupportedConfigurations.add("explicitAnalysis-java.properties");
    unsupportedConfigurations.add("predicateAnalysis-bitprecise.properties");
    unsupportedConfigurations.add("predicateAnalysis-PredAbsRefiner-ABEl-bitprecise.properties");
    unsupportedConfigurations.add("sv-comp14--02-challenge.properties");
    unsupportedConfigurations.add("sv-comp14--05-predicateAnalysis-bitprecise.properties");
    unsupportedConfigurations.add("sv-comp14--cex-check-predicateAnalysis-bitprecise.properties");

    /*
     * CPAs that do not work:
     * cpa.chc.CHCCPA
     * cpa.ldd.LDDAbstractionCPA
     * cpa.octagon.OctagonCPA
     * cpa.seplogic.SeplogicCPA
     */
  }


  /**
   * Returns the allowed options and their default values.
   */
  public static Map<String, String> getDefaultOptions() {
    return allowedOptions;
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
    if (!getDefaultOptions().containsKey(key)) { return false; }

    if (!getDefaultOptions().get(key).equals(value)) {

      // log level needs to be UPPERCASE otherwise we'll have an exception later on
      if (key.equals("log.level")) {
        value = value.toUpperCase();
      }

      // walltime must not be negative or too large
      if (key.equals("limits.time.wall")) {
        int defaultValue;
        int newValue;
        try {
          String cleanDefault = getDefault("limits.time.wall").replaceAll("[^0-9]*$", "");
          String cleanValue = value.replaceAll("[^0-9]*$", "");
          defaultValue = Integer.parseInt(cleanDefault);
          newValue = Integer.parseInt(cleanValue);

          if (newValue < 0 || newValue > defaultValue) {
            value = getDefault("limits.time.wall");
          }
        } catch (NumberFormatException e) {
          value = getDefault("limits.time.wall");
        }
      }

      usedOptions.put(key, value);
      return true;
    }

    return false;
  }

  /**
   * Sets a batch of options.
   * Comfort method for {@link #setOption(String, String)}
   *
   * @param options The options to set
   */
  public void setOptions(Map<String, String> options) {
    for (String option : options.keySet()) {
      setOption(option, options.get(option));
    }
  }

  /**
   * Returns all options that were previously set.
   *
   * @return The used options
   */
  public Map<String, String> getUsedOptions() {
    return usedOptions;
  }

  /**
   * Returns the default value of an option.
   *
   * @param key The name of the option.
   * @return The default value
   */
  public static String getDefault(String key) {
    return getDefaultOptions().get(key);
  }

  /**
   * Returns a list of configuration files that are known not to work on Google
   * App Engine.
   *
   * @return A list of unsupported configuration files.
   */
  public static List<String> getUnsupportedConfigurations() {
    return unsupportedConfigurations;
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
    try (InputStream in = Paths.get("WEB-INF", "default-options.properties").asByteSource().openStream()) {
      properties.load(in);
    }


    for (String key : properties.stringPropertyNames()) {
      options.put(key, properties.getProperty(key));
    }

    return options;
  }

  /**
   * Returns a map containing a combination of all default options and all
   * immutable options.
   *
   * @see DefaultOptions#getDefaultOptions()
   * @see DefaultOptions#getImmutableOptions()
   *
   * @return All options
   * @throws IOException If immutable options cannot be read.
   */
  public static Map<String, String> getAllOptions() throws IOException {
    Map<String, String> opts = new HashMap<>();
    opts.putAll(getDefaultOptions());
    opts.putAll(getImmutableOptions());
    return opts;
  }

  /**
   * Returns an array of all available specification files.
   *
   * @return The available specification files.
   */
  public static List<String> getSpecifications() {
    Path specificationDir = Paths.get("WEB-INF/specifications");
    File[] files = specificationDir.toFile().listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File pDir, String pName) {
        // exclude directories from the list
        return pName.endsWith(".spc");
      }
    });

    List<String> specifications = new ArrayList<>();
    for (File file : files) {
      specifications.add(file.getName());
    }
    return specifications;
  }

  /**
   * Returns an array of all available configuration files.
   *
   * @return The available configuration files.
   */
  public static List<String> getConfigurations() {
    Path configurationDir = Paths.get("WEB-INF/configurations");
    File[] files = configurationDir.toFile().listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File pDir, String pName) {
        // exclude directories from the list
        return pName.endsWith(".properties");
      }
    });

    List<String> configurations = new ArrayList<>();
    for (File file : files) {
      configurations.add(file.getName());
    }
    return configurations;
  }
}
