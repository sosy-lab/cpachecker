// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.test;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assert_;
import static org.sosy_lab.cpachecker.core.CPAcheckerTest.setUpConfiguration;
import static org.sosy_lab.cpachecker.cpa.smg2.test.SMGBaseCPATest.ProgramSubject.assertUsing;

import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Fact;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.SimpleSubjectBuilder;
import com.google.common.truth.Subject;
import java.io.IOException;
import java.util.Set;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
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

  private Configuration analysisToUse;

  private static final MachineModel machineModel = getMachineModel();

  @Before
  public void init() throws IOException, InvalidConfigurationException {
    analysisToUse = buildConfigForC(configToUse, specToUse);
  }

  private static String addProgramPathPrefixIfNeeded(String programPath) {
    if (!programPath.startsWith(TEST_PROGRAM_COMMON_PREFIX)) {
      return TEST_PROGRAM_COMMON_PREFIX + programPath;
    }
    return programPath;
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

  /**
   * Use this for checking assertions about the result of a program verification on a program given
   * via its path with Truth: <code>
   * assertThatProgram(pathToProgram).is...()</code>.
   *
   * @param pathToProgram path to test program, e.g.
   *     'test/programs/basics/array_tests/array_usage_32_true.c'. The common path-prefix
   *     'test/programs/' is automatically added if not present, i.e.
   *     'basics/array_tests/array_usage_32_true.c' is equivalent to the previous path.
   */
  protected final ProgramSubject assertThatProgram(String pathToProgram) {
    return assertUsing(analysisToUse).that(pathToProgram);
  }

  /**
   * {@link Subject} subclass for testing assertions about a program verification with Truth (allows
   * using <code>assert_().about(...).that(String).isSafe()</code> etc.) that is given the path to a
   * program to be analyzed.
   *
   * <p>For a test use {@link ProgramSubject#assertThatProgram(String)}.
   */
  public static final class ProgramSubject extends Subject {

    private final Configuration analysis;
    private final String programPath;

    private ProgramSubject(
        FailureMetadata pMetadata, String pProgramPath, Configuration pAnalysis) {
      super(pMetadata, pProgramPath);
      programPath = addProgramPathPrefixIfNeeded(checkNotNull(pProgramPath));
      analysis = checkNotNull(pAnalysis);
    }

    /** heck that the analysis result of the program is SAFE in the current analysis. */
    public void isSafe() throws Exception {
      isExpectedResult(Result.TRUE, "safe");
    }

    /** heck that the analysis result of the program is UNSAFE in the current analysis. */
    public void isUnsafe() throws Exception {
      isExpectedResult(Result.FALSE, "unsafe");
    }

    /** Check that the analysis result of the program is UNKNOWN in the current analysis. */
    public void isUnknown() throws Exception {
      isExpectedResult(Result.UNKNOWN, "unknown");
    }

    /**
     * Check that the subject is a certain result, returning an error with the String when failing.
     */
    public void isExpectedResult(Result expectedResult, String expectedResultString)
        throws Exception {
      TestResults results = CPATestRunner.run(analysis, programPath);
      Result verdict = results.getCheckerResult().getResult();
      if (verdict == expectedResult) {
        return;
      }
      failWithActual(
          Fact.fact("analysis result expected to be", expectedResultString),
          Fact.fact("but was", verdict),
          Fact.fact("which has log", results.getLog().trim()));
    }

    /**
     * Use this for checking assertions with Truth: <code>
     * assertUsing(context)).that(formula).is...()</code>.
     */
    public static SimpleSubjectBuilder<ProgramSubject, String> assertUsing(
        final Configuration analysis) {
      return assert_().about(programSubjectOf(analysis));
    }

    /**
     * Use this for checking assertions about programs (given the corresponding analysis) with
     * Truth: <code>assert_().about(programSubjectOf(analysis)).that(pathToProgram).is...()</code>.
     */
    public static Subject.Factory<ProgramSubject, String> programSubjectOf(
        final Configuration analysis) {
      return (metadata, pathToProgram) -> new ProgramSubject(metadata, pathToProgram, analysis);
    }
  }
}
