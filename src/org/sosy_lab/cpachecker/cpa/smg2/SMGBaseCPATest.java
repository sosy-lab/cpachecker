// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static org.sosy_lab.cpachecker.core.CPAcheckerTest.setUpConfiguration;

import java.io.IOException;
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
  private static final String CONFIGURATION_FILE_SMG_SYMBOLIC_EXECUTION =
      "config/smgSymbolicExecution.properties";

  private static final String CONFIGURATION_FILE_SMG_VALUE_ANALYSIS =
      "config/smgValueAnalysis.properties";

  /** Default, MemSafety, MemCleanup, and No-Overflow specifications are usable with SMG2 */
  private static final String SPECIFICATION_DEFAULT = "config/specification/default.spc";

  private static final String SPECIFICATION_MEMSAFETY = "config/specification/memorysafety.spc";
  private static final String SPECIFICATION_MEMCLEANUP = "config/specification/memorycleanup.spc";

  private static final String SPECIFICATION_OVERFLOW = "config/specification/overflow.spc";

  private static final String TEST_PROGRAM_COMMON_PREFIX = "test/programs/";

  @Parameters(name = "{0}")
  public static Configuration[] getAllConfigurations()
      throws IOException, InvalidConfigurationException {
    return new Configuration[] {
      getSMGSymExecConfigWithDefaultSpec(),
      getSMGSymExecConfigWithMemSafetySpec(),
      getSMGSymExecConfigWithMemCleanupSpec(),
      getSMGValueConfigWithDefaultSpec(),
      getSMGValueConfigWithMemSafetySpec(),
      getSMGValueConfigWithMemCleanupSpec()
    };
  }

  @Parameter public Configuration configuration;

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
    return CPATestRunner.run(configuration, testProgram);
  }

  private static Configuration getSMGSymExecConfigWithDefaultSpec()
      throws IOException, InvalidConfigurationException {
    return getConfig(CONFIGURATION_FILE_SMG_SYMBOLIC_EXECUTION, Language.C, SPECIFICATION_DEFAULT);
  }

  private static Configuration getSMGSymExecConfigWithMemSafetySpec()
      throws IOException, InvalidConfigurationException {
    return getConfig(
        CONFIGURATION_FILE_SMG_SYMBOLIC_EXECUTION, Language.C, SPECIFICATION_MEMSAFETY);
  }

  private static Configuration getSMGSymExecConfigWithMemCleanupSpec()
      throws IOException, InvalidConfigurationException {
    return getConfig(
        CONFIGURATION_FILE_SMG_SYMBOLIC_EXECUTION, Language.C, SPECIFICATION_MEMCLEANUP);
  }

  private static Configuration getSMGSymExecConfigWithOverflowSpec()
      throws IOException, InvalidConfigurationException {
    return getConfig(CONFIGURATION_FILE_SMG_SYMBOLIC_EXECUTION, Language.C, SPECIFICATION_OVERFLOW);
  }

  private static Configuration getSMGValueConfigWithDefaultSpec()
      throws IOException, InvalidConfigurationException {
    return getConfig(CONFIGURATION_FILE_SMG_VALUE_ANALYSIS, Language.C, SPECIFICATION_DEFAULT);
  }

  private static Configuration getSMGValueConfigWithMemSafetySpec()
      throws IOException, InvalidConfigurationException {
    return getConfig(CONFIGURATION_FILE_SMG_VALUE_ANALYSIS, Language.C, SPECIFICATION_MEMSAFETY);
  }

  private static Configuration getSMGValueConfigWithMemCleanupSpec()
      throws IOException, InvalidConfigurationException {
    return getConfig(CONFIGURATION_FILE_SMG_VALUE_ANALYSIS, Language.C, SPECIFICATION_MEMCLEANUP);
  }

  private static Configuration getSMGValueConfigWithOverflowSpec()
      throws IOException, InvalidConfigurationException {
    return getConfig(CONFIGURATION_FILE_SMG_VALUE_ANALYSIS, Language.C, SPECIFICATION_OVERFLOW);
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
