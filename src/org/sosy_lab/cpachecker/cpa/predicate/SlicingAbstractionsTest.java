/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

/**
 * Integration testing for Slicing Abstractions.
 */
@RunWith(Parameterized.class)
public class SlicingAbstractionsTest {

  private static final String TEST_DIR_PATH = "test/programs/slicingabstractions/";
  private static final String CONFIG_DIR_PATH = "config/";
  private static final FileFilter CONFIG_FILTER = new FileFilter() {
    @Override
    public boolean accept(File pPathname) {
      if (pPathname.getName().contains("Kojak") ||
          pPathname.getName().contains("SlicingAbstractions")) {
        return true;
      }
      return false;
    }
  };
  private static final ImmutableMap<String, String> EMPTY_OPTIONS = ImmutableMap.of();
  private static final ImmutableMap<String, String> LINEAR_OPTIONS =
      ImmutableMap.of(
          "cpa.predicate.encodeBitvectorAs", "INTEGER",
          "cpa.predicate.encodeFloatAs", "RATIONAL",
          "cpa.predicate.handleFieldAccess", "false");
  private static final ImmutableMap<String, String> UNOPTIMIZED_OPTION =
      ImmutableMap.of("cpa.predicate.slicingabstractions.optimizeslicing", "false");
  private static final ImmutableMap<String, String> MINIMAL_OPTION =
      ImmutableMap.of("cpa.predicate.slicingabstractions.minimalslicing", "true");

  private String filename;
  private String configname;
  private Map<String, String> extraOptions;

  @Parameters(name = "{3}: {0}")
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public static Collection<Object[]> data() {
    File taskfolder = new File(TEST_DIR_PATH);
    List<Object> files =
        FluentIterable.from(taskfolder.listFiles()).<Object>transform(x -> x.getName()).toList();

    File configfolder = new File(CONFIG_DIR_PATH);
    List<Object> configs =
        FluentIterable.from(configfolder.listFiles(CONFIG_FILTER))
            .<Object>transform(x -> x.getName())
            .toList();

    List<Object> solverModes = ImmutableList.of(EMPTY_OPTIONS, LINEAR_OPTIONS);

    List<Object> optimizeModes = ImmutableList.of(EMPTY_OPTIONS, UNOPTIMIZED_OPTION);

    List<Object> minimalModes = ImmutableList.of(EMPTY_OPTIONS, MINIMAL_OPTION);

    return FluentIterable
        .from(Lists.cartesianProduct(files, configs, solverModes, optimizeModes, minimalModes))
        .transform(x -> repack(x))
        .filter(x -> filter(x))
        .toList();
  }

  @SuppressWarnings("unchecked")
  private static Object[] repack(List<Object> x) {
    Object[] result = new Object[4];

    result[0] = x.get(0);
    result[1] = x.get(1);

    Map<String,String> extraOptions = new HashMap<>();
    Map<String, String> solverMode = (Map<String, String>) x.get(2);
    Map<String, String> optimizeMode = (Map<String, String>) x.get(3);
    Map<String, String> minimalMode = (Map<String, String>) x.get(4);
    extraOptions.putAll(solverMode);
    extraOptions.putAll(optimizeMode);
    extraOptions.putAll(minimalMode);
    result[2] = extraOptions;

    String modeString = ((String) x.get(1))
        .replace("predicateAnalysis-", "")
        .replace(".properties", "");
    modeString += (solverMode == EMPTY_OPTIONS) ? "-bitvector" : "-linear";
    modeString += (optimizeMode == EMPTY_OPTIONS) ? "-optimized" : "-unoptimized";
    modeString += (minimalMode == EMPTY_OPTIONS) ? "-maximal" : "-minimal";
    result[3] = modeString;

    return result;
  }

  @SuppressWarnings("unused")
  private static boolean filter(Object[] x) {
    String modeString = ((String) x[3]);
    if (modeString
        .matches(".*SlicingAbstractionsAbstractionRefiner.*unoptimized.*")) { return false; }
    return true;
  }

  public SlicingAbstractionsTest(String filename, String configname,
      Map<String, String> extraOptions,
      @SuppressWarnings("unused") String name) {
    this.filename = filename;
    this.configname = configname;
    this.extraOptions = extraOptions;
  }

  @Test
  public void check() throws Exception {
    check(filename, extraOptions);
  }

  private void check(String pFilename, Map<String, String> extra) throws Exception {
    check(pFilename, getProperties(configname, extra));
  }

  private void check(String pFilename, Configuration config) throws Exception {
    String fullPath = Paths.get(TEST_DIR_PATH, filename).toString();

    TestResults results = CPATestRunner.run(config, fullPath);
    if (pFilename.contains("_true_assert") || pFilename.contains("_true-unreach")) {
      results.assertIsSafe();
    } else if (pFilename.contains("_false_assert") || pFilename.contains("_false-unreach")) {
      results.assertIsUnsafe();
    }
  }

  private Configuration getProperties(String configFile, Map<String, String> extra)
      throws InvalidConfigurationException, IOException {
    return TestDataTools.configurationForTest()
        .loadFromFile(CONFIG_DIR_PATH + configFile)
        .setOptions(extra)
        .build();
  }
}
