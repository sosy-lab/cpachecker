// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpv;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.AbstractSingleProperty;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.AutomataSingleProperty;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class MPVTest {

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
  private static final int DEFAULT_TIME_LIMIT_PER_PROPERTY = 5; // seconds
  private static final String BAD_NAME = "_bad_name_";

  /*
   * Automata specifications, which are used for these tests.
   */
  private static final String[] AUTOMATA_FILES = {
    "test/config/automata/ldv/alloc_spinlock.spc",
    "test/config/automata/ldv/bitops.spc",
    "test/config/automata/ldv/class.spc",
    "test/config/automata/ldv/module.spc",
    "test/config/automata/ldv/mutex.spc",
    "test/config/automata/ldv/rwlock.spc",
    "test/config/automata/ldv/spinlock.spc",
    "test/config/automata/ldv/usblock.spc",
    "test/config/automata/ldv/rculock.spc"
  };

  /*
   * In this test 2 specification are violated, the others are satisfied.
   */
  private static final String SIMPLE_TEST =
      "test/programs/ldv-automata/mpv/mpv_test_false_simple.c";

  /*
   * In this test overall number of violations for specification 'linux_rculock' is undefined.
   */
  private static final String MEA_TEST = "test/programs/ldv-automata/mpv/mpv_test_false_mea.c";

  /*
   * In this test specification 'linux_rculock' may not be checked successfully.
   */
  private static final String ITL_TEST = "test/programs/ldv-automata/mpv/mpv_test_false_itl.c";

  /*
   * MPV results are presented in the following format for comparison:
   * property_1 verdict_1 all_violations_found_1 relevancy_1
   * ...
   * property_N verdict_N all_violations_found_N relevancy_N
   * These matrixes must be exactly the same for ideal and actual results.
   * This matrix represents results for file SIMPLE_TEST
   * (specifications 'linux_alloc_spinlock' and 'linux_spinlock' are violated).
   */
  private static final String[][] BASIC_IDEAL_RESULTS = {
    {"linux_alloc_spinlock", "FALSE", "true", "true"},
    {"linux_bitops", "TRUE", "false", "false"},
    {"linux_class", "TRUE", "false", "false"},
    {"linux_module", "TRUE", "false", "false"},
    {"linux_mutex", "TRUE", "false", "true"},
    {"linux_rwlock", "TRUE", "false", "false"},
    {"linux_spinlock", "FALSE", "true", "true"},
    {"linux_alloc_usblock", "TRUE", "false", "false"},
    {"linux_rculock", "TRUE", "false", "true"},
    {"linux_rculockbh", "TRUE", "false", "false"}
  };

  /*
   * This matrix represents results for file SIMPLE_TEST, if each specification file is a single property
   * (here automata 'linux_rculock' and 'linux_rculockbh' are treated as a single property 'rculock').
   */
  private static final String[][] BASIC_FILE_IDEAL_RESULTS = {
    {"alloc_spinlock", "FALSE", "true", "true"},
    {"bitops", "TRUE", "false", "false"},
    {"class", "TRUE", "false", "false"},
    {"module", "TRUE", "false", "false"},
    {"mutex", "TRUE", "false", "true"},
    {"rwlock", "TRUE", "false", "false"},
    {"spinlock", "FALSE", "true", "true"},
    {"usblock", "TRUE", "false", "false"},
    {"rculock", "TRUE", "false", "true"}
  };

  /*
   * This matrix represents results for file MEA_TEST
   * (the number of violations for specification 'linux_rculock' is undefined).
   */
  private static final String[][] MEA_IDEAL_RESULTS = {
    {"linux_alloc_spinlock", "FALSE", "true", "true"},
    {"linux_bitops", "TRUE", "false", "false"},
    {"linux_class", "TRUE", "false", "false"},
    {"linux_module", "TRUE", "false", "false"},
    {"linux_mutex", "TRUE", "false", "true"},
    {"linux_rwlock", "TRUE", "false", "false"},
    {"linux_spinlock", "FALSE", "true", "true"},
    {"linux_alloc_usblock", "TRUE", "false", "false"},
    {"linux_rculock", "FALSE", "false", "true"},
    {"linux_rculockbh", "TRUE", "false", "false"}
  };

  /*
   * This matrix represents results for file MEA_TEST
   * (specification 'linux_rculock' may not be checked).
   */
  private static final String[][] ITL_IDEAL_RESULTS = {
    {"linux_alloc_spinlock", "FALSE", "true", "true"},
    {"linux_bitops", "TRUE", "false", "false"},
    {"linux_class", "TRUE", "false", "false"},
    {"linux_module", "TRUE", "false", "false"},
    {"linux_mutex", "TRUE", "false", "true"},
    {"linux_rwlock", "TRUE", "false", "false"},
    {"linux_spinlock", "FALSE", "true", "true"},
    {"linux_alloc_usblock", "TRUE", "false", "false"},
    {"linux_rculock", "UNKNOWN", "false", "true"},
    {"linux_rculockbh", "TRUE", "false", "false"}
  };

  private static final Pattern PROPERTY_RESULT_PATTERN =
      Pattern.compile("Property '(.+)': (\\w+)$");
  private static final Pattern PROPERTY_PATTERN = Pattern.compile("^Property '(.+)'$");
  private static final Pattern RELEVANCY_PATTERN = Pattern.compile("^  Relevant:\\s+(true|false)$");
  private static final Pattern ALL_VIOLATIONS_PATTERN =
      Pattern.compile("^    All violations found:\\s+(true|false)$");

  private Map<String, String> createConfig(
      final String[] automataFiles,
      String propertySeparator,
      String partitioningOperator,
      boolean findAllViolations) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    builder.put(
        "CompositeCPA.cpas",
        "cpa.location.LocationCPA,cpa.callstack.CallstackCPA,cpa.value.ValueAnalysisCPA,cpa.predicate.PredicateCPA");
    StringBuilder specificationFiles = new StringBuilder();
    for (String file : automataFiles) {
      if (specificationFiles.length() > 0) {
        specificationFiles.append("," + file);
      } else {
        specificationFiles.append(file);
      }
    }
    builder.put("specification", specificationFiles.toString());
    builder.put("mpv.limits.cpuTimePerProperty", String.valueOf(DEFAULT_TIME_LIMIT_PER_PROPERTY));
    builder.put("analysis.algorithm.MPV", "true");
    builder.put("mpv.propertySeparator", propertySeparator);
    builder.put("mpv.partitionOperator", partitioningOperator);
    builder.put("mpv.findAllViolations", String.valueOf(findAllViolations));
    return builder.buildOrThrow();
  }

  private List<AbstractSingleProperty> parseResult(CPAcheckerResult result) {
    // Get property names with their results based on 'printResult'
    ImmutableList.Builder<AbstractSingleProperty> builder = ImmutableList.builder();
    ByteArrayOutputStream outputStreamResults = new ByteArrayOutputStream();
    @SuppressWarnings("checkstyle:IllegalInstantiation") // ok for results
    PrintStream printStreamResults = new PrintStream(outputStreamResults, true, DEFAULT_CHARSET);
    result.printResult(printStreamResults);

    for (String line : Splitter.on("\n").split(outputStreamResults.toString(DEFAULT_CHARSET))) {
      Matcher matcher = PROPERTY_RESULT_PATTERN.matcher(line);
      if (matcher.find()) {
        String name = matcher.group(1);
        String verdict = matcher.group(2);
        AbstractSingleProperty property = new AutomataSingleProperty(name, ImmutableList.of());
        property.updateResult(Result.valueOf(verdict));
        builder.add(property);
      }
    }
    List<AbstractSingleProperty> properties = builder.build();

    // Get additional parameters for properties from statistics
    ByteArrayOutputStream outputStreamStats = new ByteArrayOutputStream();
    @SuppressWarnings("checkstyle:IllegalInstantiation") // ok for statistics
    PrintStream printStreamStats = new PrintStream(outputStreamStats, true, DEFAULT_CHARSET);
    result.printStatistics(printStreamStats);

    AbstractSingleProperty currentProperty = null;
    for (String line : Splitter.on("\n").split(outputStreamStats.toString(DEFAULT_CHARSET))) {
      Matcher matcher = PROPERTY_PATTERN.matcher(line);
      if (matcher.find()) {
        String name = matcher.group(1);
        for (AbstractSingleProperty property : properties) {
          if (property.getName().equals(name)) {
            currentProperty = property;
          }
        }
      }
      matcher = RELEVANCY_PATTERN.matcher(line);
      if (matcher.find()) {
        boolean isRelevant = Boolean.parseBoolean(matcher.group(1));
        assert currentProperty != null;
        if (isRelevant) {
          currentProperty.setRelevant();
        }
      }
      matcher = ALL_VIOLATIONS_PATTERN.matcher(line);
      if (matcher.find()) {
        boolean isAllViolationsFound = Boolean.parseBoolean(matcher.group(1));
        assert currentProperty != null;
        if (isAllViolationsFound) {
          currentProperty.allViolationsFound();
        }
      }
    }

    return properties;
  }

  private void compareResultsMatrixes(
      String[][] idealResults, List<AbstractSingleProperty> propertiesResults) {
    // transform list of AbstractSingleProperty into matrix for comparison
    String[][] actualResults = new String[propertiesResults.size()][4];
    int i = 0;
    for (AbstractSingleProperty property : propertiesResults) {
      actualResults[i][0] = property.getName();
      actualResults[i][1] = property.getResult().toString();
      actualResults[i][2] = String.valueOf(property.isAllViolationsFound());
      actualResults[i][3] = String.valueOf(property.isRelevant());
      i++;
    }

    // do not compare ideal verdicts UNKNOWN
    for (i = 0; i < idealResults.length; i++) {
      if (idealResults[i][1].equals("UNKNOWN")) {
        assertThat(actualResults[i][0]).isEqualTo(idealResults[i][0]);
        for (int j = 0; j < idealResults[i].length; j++) {
          idealResults[i][j] = actualResults[i][j];
        }
      }
    }

    // results matrixes should be the same
    assertThat(actualResults).isEqualTo(idealResults);
  }

  private void checkResults(
      TestResults actualResults, String[][] idealResults, Result overallExpectedResult) {
    actualResults.assertIs(overallExpectedResult);
    List<AbstractSingleProperty> propertiesResults = parseResult(actualResults.getCheckerResult());
    if (overallExpectedResult.equals(Result.NOT_YET_STARTED)) {
      assertThat(propertiesResults).isEmpty();
    } else {
      compareResultsMatrixes(idealResults, propertiesResults);
    }
  }

  @Test
  public void simpleTest() throws Exception {
    TestResults results =
        CPATestRunner.run(
            createConfig(AUTOMATA_FILES, "AUTOMATON", "NoPartitioningOperator", false),
            SIMPLE_TEST);
    checkResults(results, BASIC_IDEAL_RESULTS, Result.FALSE);
  }

  @Test
  public void filePropertySeparator() throws Exception {
    TestResults results =
        CPATestRunner.run(
            createConfig(AUTOMATA_FILES, "FILE", "NoPartitioningOperator", false), SIMPLE_TEST);
    checkResults(results, BASIC_FILE_IDEAL_RESULTS, Result.FALSE);
  }

  @Test
  public void separatePartitioning() throws Exception {
    TestResults results =
        CPATestRunner.run(
            createConfig(AUTOMATA_FILES, "AUTOMATON", "SeparatePartitioningOperator", false),
            SIMPLE_TEST);
    checkResults(results, BASIC_IDEAL_RESULTS, Result.FALSE);
  }

  @Test
  public void relevancePartitioning() throws Exception {
    TestResults results =
        CPATestRunner.run(
            createConfig(AUTOMATA_FILES, "AUTOMATON", "RelevancePartitioningOperator", false),
            SIMPLE_TEST);
    checkResults(results, BASIC_IDEAL_RESULTS, Result.FALSE);
  }

  @Test
  public void jointPartitioning() throws Exception {
    TestResults results =
        CPATestRunner.run(
            createConfig(AUTOMATA_FILES, "AUTOMATON", "JointPartitioningOperator", false),
            SIMPLE_TEST);
    checkResults(results, BASIC_IDEAL_RESULTS, Result.FALSE);
  }

  @Test
  public void meaAllViolations() throws Exception {
    TestResults results =
        CPATestRunner.run(
            createConfig(AUTOMATA_FILES, "AUTOMATON", "NoPartitioningOperator", true), SIMPLE_TEST);
    checkResults(results, BASIC_IDEAL_RESULTS, Result.FALSE);
  }

  @Test
  public void onlySafeVerdicts() throws Exception {
    String[] safeAutomata =
        Arrays.stream(AUTOMATA_FILES).filter(s -> !s.contains("spin")).toArray(String[]::new);
    TestResults results =
        CPATestRunner.run(
            createConfig(safeAutomata, "AUTOMATON", "NoPartitioningOperator", true), SIMPLE_TEST);
    String[][] idealResults =
        Arrays.stream(BASIC_IDEAL_RESULTS)
            .filter(s -> !s[0].contains("spin"))
            .toArray(String[][]::new);
    checkResults(results, idealResults, Result.TRUE);
  }

  @Test
  public void meaPartialResult() throws Exception {
    TestResults results =
        CPATestRunner.run(
            createConfig(AUTOMATA_FILES, "AUTOMATON", "SeparatePartitioningOperator", true),
            MEA_TEST);
    checkResults(results, MEA_IDEAL_RESULTS, Result.FALSE);
  }

  @Test
  public void innerTimeLimit() throws Exception {
    TestResults results =
        CPATestRunner.run(
            createConfig(AUTOMATA_FILES, "AUTOMATON", "SeparatePartitioningOperator", false),
            ITL_TEST);
    checkResults(results, ITL_IDEAL_RESULTS, Result.FALSE);
  }

  @Test
  public void overallUnknown() throws Exception {
    String[] safeOrUnknownAutomata =
        Arrays.stream(AUTOMATA_FILES).filter(s -> !s.contains("spin")).toArray(String[]::new);
    TestResults results =
        CPATestRunner.run(
            createConfig(safeOrUnknownAutomata, "AUTOMATON", "SeparatePartitioningOperator", true),
            ITL_TEST);
    String[][] idealResults =
        Arrays.stream(ITL_IDEAL_RESULTS)
            .filter(s -> !s[0].contains("spin"))
            .toArray(String[][]::new);
    checkResults(results, idealResults, Result.UNKNOWN);
  }

  @Test
  public void badSeparator() throws Exception {
    TestResults results =
        CPATestRunner.run(
            createConfig(AUTOMATA_FILES, BAD_NAME, "NoPartitioningOperator", true), SIMPLE_TEST);
    checkResults(results, null, Result.NOT_YET_STARTED);
  }

  @Test
  public void badPartitioning() throws Exception {
    TestResults results =
        CPATestRunner.run(createConfig(AUTOMATA_FILES, "AUTOMATON", BAD_NAME, true), SIMPLE_TEST);
    checkResults(results, null, Result.NOT_YET_STARTED);
  }

  @Test
  public void badFileName() throws Exception {
    TestResults results =
        CPATestRunner.run(
            createConfig(AUTOMATA_FILES, "AUTOMATON", "NoPartitioningOperator", true), BAD_NAME);
    checkResults(results, null, Result.NOT_YET_STARTED);
  }
}
