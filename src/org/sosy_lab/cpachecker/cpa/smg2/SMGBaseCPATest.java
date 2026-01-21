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

  /** Default, MemSafety, and MemCleanup specifications are used commonly in SMG2 */
  private static final String SPECIFICATION_DEFAULT = "config/specification/default.spc";

  private static final String SPECIFICATION_MEMSAFETY = "config/specification/memorysafety.spc";
  private static final String SPECIFICATION_MEMCLEANUP = "config/specification/memorycleanup.spc";

  // TODO: overflows?

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

  void runAndAssertSafe(String testProgram) throws Exception {
    TestResults results = CPATestRunner.run(configuration, testProgram);
    results.assertIsSafe();
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
