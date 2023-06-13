// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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

/** Integration testing for Slicing Abstractions. */
@RunWith(Parameterized.class)
public class SlicingAbstractionsTest {

  private static final String TEST_DIR_PATH = "test/programs/slicingabstractions/";
  private static final String CONFIG_DIR_PATH = "config/";

  private static boolean isConfig(File pPathname) {
    return ((pPathname.getName().contains("Kojak")
            || pPathname.getName().contains("SlicingAbstractions"))
        && !pPathname.getName().contains("overflow"));
  }

  private static boolean isSlabConfig(File pPathname) {
    return pPathname.getName().contains("Slab");
  }

  private static boolean isOverflowConfig(File pPathname) {
    return ((pPathname.getName().contains("Kojak")
            || pPathname.getName().contains("SlicingAbstractions"))
        && pPathname.getName().contains("overflow"));
  }

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
        FluentIterable.from(taskfolder.listFiles())
            .<Object>transform(x -> x.getName())
            .filter(x -> ((String) x).contains("unreach"))
            .toList();
    List<Object> overflowFiles =
        FluentIterable.from(taskfolder.listFiles())
            .<Object>transform(x -> x.getName())
            .filter(x -> ((String) x).contains("overflow"))
            .toList();

    File configfolder = new File(CONFIG_DIR_PATH);
    List<Object> configs =
        FluentIterable.from(configfolder.listFiles(SlicingAbstractionsTest::isConfig))
            .<Object>transform(x -> x.getName())
            .toList();

    List<Object> slabConfigs =
        FluentIterable.from(configfolder.listFiles(SlicingAbstractionsTest::isSlabConfig))
            .<Object>transform(x -> x.getName())
            .toList();

    List<Object> overflowConfigs =
        FluentIterable.from(configfolder.listFiles(SlicingAbstractionsTest::isOverflowConfig))
            .<Object>transform(x -> x.getName())
            .toList();

    List<Object> solverModes = ImmutableList.of(EMPTY_OPTIONS, LINEAR_OPTIONS);

    List<Object> optimizeModes = ImmutableList.of(EMPTY_OPTIONS, UNOPTIMIZED_OPTION);

    List<Object> minimalModes = ImmutableList.of(EMPTY_OPTIONS, MINIMAL_OPTION);

    FluentIterable<Object[]> firstIterable =
        FluentIterable.from(
                Lists.cartesianProduct(files, configs, solverModes, optimizeModes, minimalModes))
            .transform(x -> repack(x))
            .filter(x -> filter(x));

    FluentIterable<Object[]> secondIterable =
        FluentIterable.from(Lists.cartesianProduct(files, slabConfigs))
            .transform(
                x -> {
                  Object[] result = new Object[4];
                  result[0] = x.get(0);
                  result[1] = x.get(1);
                  result[2] = new HashMap<String, String>();
                  result[3] =
                      ((String) result[1])
                          .replace("predicateAnalysis-", "")
                          .replace(".properties", "");
                  return result;
                });

    FluentIterable<Object[]> thirdIterable =
        FluentIterable.from(Lists.cartesianProduct(overflowFiles, overflowConfigs))
            .transform(
                x -> {
                  Object[] result = new Object[4];
                  result[0] = x.get(0);
                  result[1] = x.get(1);
                  result[2] = new HashMap<String, String>();
                  result[3] =
                      ((String) result[1])
                          .replace("predicateAnalysis-", "")
                          .replace(".properties", "");
                  return result;
                });

    return firstIterable.append(secondIterable).append(thirdIterable).toList();
  }

  @SuppressWarnings("unchecked")
  private static Object[] repack(List<Object> x) {
    Object[] result = new Object[4];

    result[0] = x.get(0); // file to test
    result[1] = x.get(1); // config to test file with

    // result[2] will contain a map of extra options taken from x at positions 2-4
    Map<String, String> extraOptions = new HashMap<>();
    Map<String, String> solverMode = (Map<String, String>) x.get(2);
    Map<String, String> optimizeMode = (Map<String, String>) x.get(3);
    Map<String, String> minimalMode = (Map<String, String>) x.get(4);
    extraOptions.putAll(solverMode);
    extraOptions.putAll(optimizeMode);
    extraOptions.putAll(minimalMode);
    result[2] = extraOptions;

    // result[3] will contain a suitable name for the test to display
    String modeString =
        ((String) x.get(1)).replace("predicateAnalysis-", "").replace(".properties", "");
    modeString += (solverMode == EMPTY_OPTIONS) ? "-bitvector" : "-linear";
    modeString += (optimizeMode == EMPTY_OPTIONS) ? "-optimized" : "-unoptimized";
    modeString += (minimalMode == EMPTY_OPTIONS) ? "-maximal" : "-minimal";
    result[3] = modeString;

    return result;
  }

  @SuppressWarnings("unused")
  private static boolean filter(Object[] x) {
    String modeString = ((String) x[3]);
    if (modeString.matches(".*SlicingAbstractionsAbstractionRefiner.*unoptimized.*")) {
      return false;
    }
    return true;
  }

  public SlicingAbstractionsTest(
      String filename,
      String configname,
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
    String fullPath = Path.of(TEST_DIR_PATH, filename).toString();

    TestResults results = CPATestRunner.run(config, fullPath);
    if (!configname.contains("overflow")) {
      if (pFilename.contains("_true_assert") || pFilename.contains("_true-unreach")) {
        results.assertIsSafe();
      } else if (pFilename.contains("_false_assert") || pFilename.contains("_false-unreach")) {
        results.assertIsUnsafe();
      }
    } else {
      if (pFilename.contains("_true_no_overflow")) {
        results.assertIsSafe();
      } else if (pFilename.contains("_false_no_overflow")) {
        results.assertIsUnsafe();
      }
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
