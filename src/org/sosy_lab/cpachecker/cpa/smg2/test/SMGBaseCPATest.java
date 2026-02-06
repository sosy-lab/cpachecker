// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.test;

import static org.sosy_lab.cpachecker.core.CPAcheckerTest.setUpConfiguration;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Set;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

/**
 * Base class to execute the SMG2 CPA with test programs. Override to execute with a specific {@link
 * MachineModel}.
 */
@RunWith(Parameterized.class)
public class SMGBaseCPATest {

  /**
   * The default configuration files to use for running SMG2 as Symbolic Execution and Value
   * Analysis
   */
  private static final String SMG_SYMBOLIC_EXECUTION = "smgSymbolicExecution.properties";

  private static final String SMG_VALUE_ANALYSIS = "smgValueAnalysis.properties";

  /** Default, MemSafety, MemCleanup, and No-Overflow specifications are usable with SMG2 */
  private static final String DEFAULT_SPECIFICATION = "default.spc";

  private static final String MEMSAFETY_SPECIFICATION = "memorysafety.spc";
  private static final String MEMCLEANUP_SPECIFICATION = "memorycleanup.spc";
  private static final String OVERFLOW_SPECIFICATION = "overflow.spc";

  private static final String TEST_PROGRAM_COMMON_PREFIX = "test/programs/";
  private static final String CPA_CONFIG_COMMON_PREFIX = "config/";
  private static final String SPECIFICATION_COMMON_PREFIX = "config/specification/";

  @Parameters(name = "CPA: {0} with specification: {1}")
  public static String[][] getAllConfigurationsAndSpecifications() {
    return getAllSMGCPAConfigurationsAndSpecificationsForTests();
  }

  private static String[][] getAllSMGCPAConfigurationsAndSpecificationsForTests() {
    Set<String> cpasToRun = ImmutableSet.of(SMG_SYMBOLIC_EXECUTION, SMG_VALUE_ANALYSIS);
    Set<String> specsToRun =
        ImmutableSet.of(
            DEFAULT_SPECIFICATION,
            MEMSAFETY_SPECIFICATION,
            MEMCLEANUP_SPECIFICATION,
            OVERFLOW_SPECIFICATION);
    String[][] params = new String[cpasToRun.size() * specsToRun.size()][2];
    int count = 0;
    for (String cpa : cpasToRun) {
      for (String spec : specsToRun) {
        params[count][0] = cpa;
        params[count][1] = spec;
        count++;
      }
    }
    return params;
  }

  @Parameter(0)
  public String configToUse;

  @Parameter(1)
  public String specToUse;

  private static final MachineModel machineModel = getMachineModel();

  /**
   * Executes the program given in the current {@link Configuration} and asserts that it is SAFE.
   *
   * @param testProgram path to test program, e.g.
   *     'test/programs/basics/array_tests/array_usage_32_true.c'. The common path-prefix
   *     'test/programs/' is automatically added if not present, i.e.
   *     'basics/array_tests/array_usage_32_true.c' is equivalent to the previous path.
   */
  void runAndAssertSafe(String testProgram) throws Exception {
    runProgram(testProgram).assertIsSafe();
  }

  /**
   * Executes the program given in the current {@link Configuration} and asserts that it is UNSAFE.
   *
   * @param testProgram path to test program, e.g.
   *     'test/programs/basics/array_tests/array_usage_32_false.c'. The common path-prefix
   *     'test/programs/' is automatically added if not present, i.e.
   *     'basics/array_tests/array_usage_32_false.c' is equivalent to the previous path.
   */
  void runAndAssertUnsafe(String testProgram) throws Exception {
    runProgram(testProgram).assertIsUnsafe();
  }

  private TestResults runProgram(String testProgram) throws Exception {
    if (!testProgram.startsWith(TEST_PROGRAM_COMMON_PREFIX)) {
      testProgram = TEST_PROGRAM_COMMON_PREFIX + testProgram;
    }
    return CPATestRunner.run(buildConfigForC(configToUse, specToUse), testProgram);
  }

  private static Configuration buildConfigForC(String cpaConfiguration, String specification)
      throws IOException, InvalidConfigurationException {
    return getConfig(
        CPA_CONFIG_COMMON_PREFIX + cpaConfiguration,
        Language.C,
        SPECIFICATION_COMMON_PREFIX + specification);
  }

  /** Uses the default {@link Configuration} and does not allow generated files to be accessed. */
  protected static Configuration getConfig(
      String configurationFile, Language inputLanguage, String specificationFile)
      throws InvalidConfigurationException, IOException {

    Configuration configForFiles = Configuration.defaultConfiguration();
    return setUpConfiguration(
        configurationFile, inputLanguage, specificationFile, configForFiles, machineModel);
  }

  protected static MachineModel getMachineModel() {
    return MachineModel.LINUX32;
  }
}
