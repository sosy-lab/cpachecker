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
package org.sosy_lab.cpachecker.appengine.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * This class manages options of CPAchecker that are set by default on App Engine.
 * It also provides methods to set options that can be overwritten and validates
 * and sanitizes them.
 * Furthermore it provides access to the supported and unsupported specifications
 * and configurations of CPAchecker.
 */
public class DefaultOptions {

  public static final String CONFIGURATIONS_DIR = "WEB-INF/configurations";
  public static final String SPECIFICATIONS_DIR = "WEB-INF/specifications";

  public static final String DEFAUL_WALLTIME_LIMIT = "540"; // 9 minutes

  private static Map<String, String> defaultOptions;
  private static List<String> unsupportedConfigurations;
  private Map<String, String> usedOptions = new HashMap<>();
  private String originalWalltimeLimit;

  static {
    defaultOptions = ImmutableMap.<String, String>builder()
        .put("analysis.machineModel", "Linux32")
        .put("output.disable", "false")
        .put("statistics.export", "true")
        .put("log.level", "FINER")
        .put("limits.time.wall", DEFAUL_WALLTIME_LIMIT)
        .put("gae.instanceType", "FRONTEND")
        .build();

    /*
     * CPAs that do not work:
     * cpa.chc.CHCCPA
     * cpa.ldd.LDDAbstractionCPA
     * cpa.octagon.OctagonCPA
     * cpa.seplogic.SeplogicCPA
     */
  }

  /**
   * Returns the mutable default options and their according values.
   */
  public static Map<String, String> getDefaultOptions() {
    return defaultOptions;
  }

  /**
   * Sets an option.
   * Validates the option that is to be set and makes sure it does not break
   * any constraints imposed by App Engine.
   *
   * @param key The option to set
   * @param value The value to set
   * @return True, if the option will be used as is, false if it was altered.
   */
  public boolean setOption(String key, String value) {
    boolean optionIsUnchanged = true;

    // log level needs to be UPPERCASE and valid
    if (key.equals("log.level")) {
      value = value.toUpperCase();
      try {
        Level.parse(value);
      } catch (IllegalArgumentException e) {
        value = getDefault("log.level");
        optionIsUnchanged = false;
      }
    }

    // walltime must not be negative or too large on front-end instances
    if (key.equals("limits.time.wall")) {
      if (!getOptions().containsKey("gae.instanceType")
          || !getOptions().get("gae.instanceType").equals("BACKEND")) {
        originalWalltimeLimit = value;
        int newValue;
        int defaultValue;
        try {
          defaultValue = Integer.parseInt(getDefault("limits.time.wall").replaceAll("[^0-9]*$", ""));
          newValue = Integer.parseInt(value.replaceAll("[^0-9]*$", ""));
          if (newValue < 0 || newValue > defaultValue) {
            value = getDefault("limits.time.wall");
            optionIsUnchanged = false;
          }
        } catch (NumberFormatException e) {
          value = getDefault("limits.time.wall");
          optionIsUnchanged = false;
        }
      }
    }

    // backends can run forever. so recover a previously overwritten wall time limit
    if (key.equals("gae.instanceType") && value.equals("BACKEND") && originalWalltimeLimit != null) {
      usedOptions.put("limits.time.wall", originalWalltimeLimit);
    }

    usedOptions.put(key, value);
    return optionIsUnchanged;
  }

  /**
   * Sets a batch of options.
   * Comfort method for {@link #setOption(String, String)}
   *
   * @param options The options to set
   */
  public void setOptions(Map<String, String> options) {
    if (options != null) {
      for (Entry<String, String> entry : options.entrySet()) {
        setOption(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * Returns all options that were previously set.
   *
   * @return The used options
   */
  public Map<String, String> getOptions() {
    return ImmutableMap.copyOf(usedOptions);
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
  public static List<String> getUnsupportedConfigurations() throws IOException {
    if (unsupportedConfigurations == null) {
      unsupportedConfigurations = Paths.get("WEB-INF", "unsupported-configurations.txt")
          .asCharSource(Charsets.UTF_8).readLines();
    }

    // remove the first two lines. they are comments.
    return unsupportedConfigurations.subList(2, unsupportedConfigurations.size());
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

    return ImmutableMap.copyOf(options);
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
    return ImmutableMap.<String, String>builder()
        .putAll(getDefaultOptions())
        .putAll(getImmutableOptions())
        .build();
  }

  /**
   * Returns an array of all available specification files.
   *
   * @return The available specification files.
   */
  public static List<String> getSpecifications() {
    Path specificationDir = Paths.get(SPECIFICATIONS_DIR);
    File[] files = specificationDir.toFile().listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File pDir, String pName) {
        // exclude directories from the list
        return pName.endsWith(".spc");
      }
    });

    List<String> specifications = new LinkedList<>();
    for (File file : files) {
      specifications.add(file.getName());
    }
    return ImmutableList.copyOf(specifications);
  }

  /**
   * Returns an array of all available configuration files.
   *
   * @return The available configuration files.
   */
  public static List<String> getConfigurations() throws IOException {
    Path configurationDir = Paths.get(CONFIGURATIONS_DIR);
    File[] files = configurationDir.toFile().listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File pDir, String pName) {
        // exclude directories from the list
        return pName.endsWith(".properties");
      }
    });

    List<String> configurations = new LinkedList<>();
    for (File file : files) {
      configurations.add(file.getName());
    }

    configurations.removeAll(getUnsupportedConfigurations());
    return ImmutableList.copyOf(configurations);
  }
}
