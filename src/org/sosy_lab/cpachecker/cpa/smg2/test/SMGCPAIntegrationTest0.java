// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.test;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;
import static com.google.common.truth.TruthJUnit.assume;
import static org.sosy_lab.cpachecker.cpa.smg2.test.SMGCPAIntegrationTest0.ProgramSubject.assertUsing;
import static org.sosy_lab.cpachecker.cpa.smg2.test.SMGCPAIntegrationTest0.WitnessType.GRAPHML_VIOLATION;

import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Fact;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.SimpleSubjectBuilder;
import com.google.common.truth.Subject;
import com.google.common.truth.TruthJUnit;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cmdline.InvalidCmdlineArgumentException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.SegmentRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointAction;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointType;

/**
 * Base class to execute common configurations of the SMG2-CPA with test programs for multiple
 * common specifications.
 */
@RunWith(Parameterized.class)
public abstract class SMGCPAIntegrationTest0 {

  enum WitnessType {
    GRAPHML_VIOLATION {
      @Override
      public String toString() {
        return "witness.graphml";
      }
    },
    GRAPHML_CORRECTNESS,
    YML_VIOLATION {
      @Override
      public String toString() {
        return "witness.yml";
      }
    },
    YML_CORRECTNESS
  }

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  protected static final String CPA_CONFIG_COMMON_PREFIX = "config/";
  protected static final String SPECIFICATION_COMMON_PREFIX = "config/specification/";

  /**
   * The default configuration files to use for running SMG2 as Symbolic Execution and Value
   * Analysis
   */
  protected static final String SMG_SYMBOLIC_EXECUTION = "smgSymbolicExecution.properties";

  private static final String SMG_VALUE_ANALYSIS = "smgValueAnalysis.properties";
  protected static final String SVCOMP27 = "svcomp27.properties";

  /** Default, MemSafety, MemCleanup, and No-Overflow specifications are usable with SMG2 */
  private static final String DEFAULT_SPECIFICATION = "default.spc";

  protected static final String MEMSAFETY_SPECIFICATION = "memorysafety.spc";
  private static final String MEMCLEANUP_SPECIFICATION = "memorycleanup.spc";
  private static final String OVERFLOW_SPECIFICATION = "overflow.spc";
  protected static final String VALID_MEMSAFETY_PROPERTY =
      CPA_CONFIG_COMMON_PREFIX + "properties/valid-memsafety.prp";

  private static final String TEST_PROGRAM_COMMON_PREFIX = "test/programs/";

  @Parameter(0)
  public String configToUse;

  @Parameter(1)
  public String specToUse;

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

  protected static String addProgramPathPrefixIfNeeded(String programPath) {
    if (!programPath.startsWith(TEST_PROGRAM_COMMON_PREFIX)) {
      return TEST_PROGRAM_COMMON_PREFIX + programPath;
    }
    return programPath;
  }

  protected abstract Configuration buildConfiguration(
      String pProgramPath, MachineModel pMachineModel)
      throws IOException,
          InvalidConfigurationException,
          InvalidCmdlineArgumentException,
          InterruptedException;

  /**
   * Skips all overflow specifications for a test, starting from the position this method is used.
   */
  protected void doNotTestOverflowSpecification() {
    assume().that(specToUse).isNotEqualTo(OVERFLOW_SPECIFICATION);
  }

  /**
   * Skips all specifications for a test except the default specification, starting from the
   * position this method is used.
   */
  protected void onlyTestDefaultSpecification() {
    assume().that(specToUse).isEqualTo(DEFAULT_SPECIFICATION);
  }

  /**
   * Skips all configurations for a test except the smgSymbolicExecution configuration, starting
   * from the position this method is used.
   */
  protected void onlyTestSMGSymbolicExecutionConfiguration() {
    assume().that(configToUse).isEqualTo(SMG_SYMBOLIC_EXECUTION);
  }

  /**
   * Skips the MemCleanup specification for a test, starting from the position this method is used.
   */
  protected void doNotTestMemCleanupSpecification() {
    assume().that(specToUse).isNotEqualTo(MEMCLEANUP_SPECIFICATION);
  }

  /** Skips SMG-ValueAnalysis configurations, starting from the position this method is used. */
  protected void doNotTestSMGValueAnalysisConfigurations() {
    assume().that(configToUse).isNotEqualTo(SMG_VALUE_ANALYSIS);
  }

  /**
   * Checks assertions about the verification result of a ILP32 program given via its path with
   * {@link com.google.common.truth.Truth}: <code>assertThatILP32Program(pathToProgram).is...()
   * </code>, with e.g. <code>isSafe()</code>, <code>isUnsafe()</code> etc. The verification used is
   * defined via the current test parameters as defined in {@link SMGCPAIntegrationTest0}. You can
   * disable certain {@link Configuration}s via methods like {@link
   * SMGCPAIntegrationTest0#doNotTestOverflowSpecification()}, or {@link TruthJUnit#assume()}
   * statements.
   *
   * @param pathToProgram path to a ILP32 test program, e.g.
   *     'test/programs/basics/array_tests/array_usage_32_true.c'. The common path-prefix
   *     'test/programs/' is automatically added if not present, i.e.
   *     'basics/array_tests/array_usage_32_true.c' is equivalent to the previous path.
   */
  protected final ProgramSubject assertThatILP32Program(String pathToProgram)
      throws IOException,
          InvalidConfigurationException,
          InvalidCmdlineArgumentException,
          InterruptedException {
    return assertThatProgram(pathToProgram, MachineModel.LINUX32);
  }

  /**
   * Checks assertions about the verification result of a LP64 program given via its path with
   * {@link com.google.common.truth.Truth}: <code>assertThatLP64Program(pathToProgram).is...()
   * </code>, with e.g. <code>isSafe()</code>, <code>isUnsafe()</code> etc. The verification used is
   * defined via the current test parameters as defined in {@link SMGCPAIntegrationTest0}. You can
   * disable certain {@link Configuration}s via methods like {@link
   * SMGCPAIntegrationTest0#doNotTestOverflowSpecification()}, or {@link TruthJUnit#assume()}
   * statements.
   *
   * @param pathToProgram path to a LP64 program, e.g.
   *     'test/programs/basics/array_tests/array_usage_64_true.c'. The common path-prefix
   *     'test/programs/' is automatically added if not present, i.e.
   *     'basics/array_tests/array_usage_64_true.c' is equivalent to the previous path.
   */
  protected final ProgramSubject assertThatLP64Program(String pathToProgram)
      throws IOException,
          InvalidConfigurationException,
          InvalidCmdlineArgumentException,
          InterruptedException {
    return assertThatProgram(pathToProgram, MachineModel.LINUX64);
  }

  private ProgramSubject assertThatProgram(String pathToProgram, MachineModel pMachineModel)
      throws IOException,
          InvalidConfigurationException,
          InvalidCmdlineArgumentException,
          InterruptedException {
    String programPath = addProgramPathPrefixIfNeeded(pathToProgram);
    return assertUsing(buildConfiguration(programPath, pMachineModel), tempFolder)
        .that(programPath);
  }

  /**
   * {@link Subject} subclass for testing assertions about a program verification with Truth (allows
   * using <code>assert_().about(...).that(String).isSafe()</code> etc.) that is given the path to a
   * program to be analyzed.
   *
   * <p>For a test use <code>assertThatILP64Program(pathToProgram).is...()</code>. via either {@link
   * SMGCPAIntegrationTest0#assertThatLP64Program(String)} or {@link
   * SMGCPAIntegrationTest0#assertThatILP32Program(String)}.
   */
  public static final class ProgramSubject extends Subject {

    private final Configuration config;
    private final String programPath;
    private final TemporaryFolder tempFolder;

    private ProgramSubject(
        FailureMetadata pMetadata,
        String pProgramPath,
        Configuration pConfig,
        TemporaryFolder pTemporaryFolder) {
      super(pMetadata, pProgramPath);
      programPath = addProgramPathPrefixIfNeeded(checkNotNull(pProgramPath));
      config = checkNotNull(pConfig);
      tempFolder = pTemporaryFolder;
    }

    /** Check that the analysis result of the program is SAFE in the current analysis. */
    public void isSafe() throws Exception {
      verifySafeResult();
    }

    private void verifySafeResult() throws Exception {
      isExpectedResult(runAnalysis(), Result.TRUE, "TRUE (safe program for chosen specification)");
    }

    /** Check that the analysis result of the program is UNSAFE in the current analysis. */
    public void isUnsafe() throws Exception {
      verifyUnsafeResult(runAnalysis());
    }

    private void verifyUnsafeResult(IntegrationTestResult verificationResult) throws Exception {
      isExpectedResult(
          verificationResult,
          Result.FALSE,
          "FALSE (violation found in program for chosen specification)");
    }

    /** Check that the analysis result of the program is UNKNOWN in the current analysis. */
    public void isUnknown() throws Exception {
      verifyUnknownResult();
    }

    private void verifyUnknownResult() throws Exception {
      isExpectedResult(runAnalysis(), Result.UNKNOWN, "UNKNOWN");
    }

    /**
     * Check that the analysis result of the program is UNSAFE in the current analysis, and that a
     * graphml violation witness is returned that contains the given string.
     */
    public void returnsViolationWitnessV1Containing(String stringContainedInWitness)
        throws Exception {
      returnsWitnessContaining(stringContainedInWitness, GRAPHML_VIOLATION);
    }

    /**
     * Checks that the analysis result is unsafe and its YAML violation witness has one target
     * waypoint in the final segment at the given original-input source location.
     *
     * <p>The target description must name the expected violated subproperty. The target has action
     * {@code FOLLOW} and no constraint. The single-target requirement is specific to the sequential
     * test programs currently covered by this assertion.
     */
    public void returnsViolationWitnessWithTargetAt(
        String pExpectedSubproperty, int pExpectedLine, int pExpectedColumn) throws Exception {
      IntegrationTestResult result = runAnalysisWithOutputFiles();
      try (CPAcheckerResult analysisResult = result.cpaCheckerResult()) {
        result.assertIsUnsafe();
        verifyTargetSubproperty(analysisResult, pExpectedSubproperty);
        analysisResult.writeOutputFiles();

        Path witnessPath = getDefaultWitnessOutputPathFor(WitnessType.YML_VIOLATION);
        checkWitnessOutputCorrectness(result, witnessPath);
        List<AbstractEntry> entries;
        try (InputStream witnessInput = Files.newInputStream(witnessPath)) {
          entries = AutomatonWitnessV2ParserUtils.parseYAML(witnessInput);
        }

        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst()).isInstanceOf(ViolationSequenceEntry.class);
        ViolationSequenceEntry violationSequence = (ViolationSequenceEntry) entries.getFirst();
        assertThat(violationSequence.getContent()).isNotEmpty();

        SegmentRecord finalSegment = violationSequence.getContent().getLast();
        List<WaypointRecord> targetWaypoints = new ArrayList<>();
        for (WaypointRecord waypoint : finalSegment.getSegment()) {
          if (waypoint.getType() == WaypointType.TARGET) {
            targetWaypoints.add(waypoint);
          }
        }

        assertThat(targetWaypoints).hasSize(1);
        WaypointRecord targetWaypoint = targetWaypoints.getFirst();
        assertThat(targetWaypoint.getAction()).isEqualTo(WaypointAction.FOLLOW);
        assertThat(targetWaypoint.getConstraint()).isNull();
        assertThat(Path.of(targetWaypoint.getLocation().getFileName()).toAbsolutePath().normalize())
            .isEqualTo(Path.of(programPath).toAbsolutePath().normalize());
        assertThat(targetWaypoint.getLocation().getLine()).isEqualTo(pExpectedLine);
        assertThat(targetWaypoint.getLocation().getColumn().isPresent()).isTrue();
        assertThat(targetWaypoint.getLocation().getColumn().getAsInt()).isEqualTo(pExpectedColumn);
      }
    }

    private void verifyTargetSubproperty(
        CPAcheckerResult pAnalysisResult, String pExpectedSubproperty) {
      UnmodifiableReachedSet reachedSet = checkNotNull(pAnalysisResult.getReached());
      List<String> targetDescriptions = new ArrayList<>();
      String expectedPrefix = pExpectedSubproperty + ":";
      for (TargetInformation targetInformation : reachedSet.getTargetInformation()) {
        String targetDescription = targetInformation.toString();
        targetDescriptions.add(targetDescription);
        if (targetDescription.startsWith(expectedPrefix)) {
          return;
        }
      }
      failWithoutActual(
          Fact.fact("target description expected to start with", expectedPrefix),
          Fact.fact("available target descriptions", targetDescriptions));
    }

    private void returnsWitnessContaining(String stringContainedInWitness, WitnessType witnessType)
        throws Exception {
      IntegrationTestResult res = runAnalysisWithOutputFiles();
      verifyUnsafeResult(res);
      // TODO: do we need statistics?
      // res.getCheckerResult().printStatistics(statisticsStream);
      res.cpaCheckerResult().writeOutputFiles();
      Path witnessPath = getDefaultWitnessOutputPathFor(witnessType);
      checkWitnessOutputCorrectness(res, witnessPath);
      String witness = checkNotNull(Files.readString(witnessPath));

      assertThat(witness).contains("<data key=\"sourcecodelang\">C</data>");
      assertThat(witness).contains("<data key=\"witness-type\">violation_witness</data>");

      checkWitnessType(witness, witnessType);

      if (!isNullOrEmpty(stringContainedInWitness) && !witness.contains(stringContainedInWitness)) {
        failWithActual(
            Fact.fact("witness expected to contain", stringContainedInWitness),
            Fact.simpleFact("but did not contain the wanted string"),
            Fact.fact("with witness", witness));
      }
    }

    private void checkWitnessType(String witnessContent, WitnessType expectedWitnessType) {
      if (isNullOrEmpty(witnessContent)) {
        failWithoutActual(
            Fact.fact("witness expected to be", expectedWitnessType.name()),
            Fact.fact("but was null or empty:", witnessContent));
      }

      WitnessType actualWitnessType = getWitnessType(witnessContent);
      if (actualWitnessType != expectedWitnessType) {
        failWithActual(
            Fact.fact("witness expected to be", expectedWitnessType.name()),
            Fact.fact("but was", actualWitnessType.name()));
      }
    }

    // TODO:
    private WitnessType getWitnessType(String witnessContent) {
      checkNotNull(witnessContent);
      if (witnessContent.contains("<data key=\"sourcecodelang\">C</data>")) {
        if (witnessContent.contains("<data key=\"witness-type\">violation_witness</data>")) {
          return GRAPHML_VIOLATION;
        }
      }
      throw new UnsupportedOperationException("implement me");
    }

    private Path getDefaultWitnessOutputPathFor(WitnessType witnessTypeForName) {
      return Path.of(tempFolder.getRoot().getAbsolutePath(), witnessTypeForName.toString());
    }

    private void checkWitnessOutputCorrectness(
        IntegrationTestResult pResult, Path pWitnessOutputPath) {

      // No CFA -> no witness
      CFA cfa = pResult.cpaCheckerResult().getCfa();
      if (cfa == null) {
        failWithoutActual(
            Fact.fact("CFA should be present in the result when witnesses are requested", cfa));
      }

      if (!Files.exists(pWitnessOutputPath)) {
        failWithoutActual(Fact.fact("No witness could be found using path", pWitnessOutputPath));
      }
    }

    private IntegrationTestResult runAnalysis() throws Exception {
      return runAnalysis(config);
    }

    /**
     * Runs the analysis and sets the "output.path" option to the absolute path of this subjects
     * current tempFolder
     */
    private IntegrationTestResult runAnalysisWithOutputFiles() throws Exception {
      Configuration configWithOutputFiles =
          Configuration.builder()
              .copyFrom(config)
              .setOption("output.path", tempFolder.getRoot().getAbsolutePath())
              .build();
      return runAnalysis(configWithOutputFiles);
    }

    private IntegrationTestResult runAnalysis(Configuration configToRun) throws Exception {
      // Check that the file exists and is a C file before running
      checkArgument(
          programPath.endsWith(".i") || programPath.endsWith(".c"),
          "Test program file ending does not match allowed C files endings '.c' or '.i'");
      checkArgument(
          new File(programPath).isFile(), "Test program could not be found: %s", programPath);
      return IntegrationTestRunner.run(configToRun, programPath);
    }

    /**
     * Check that the subject is a certain result, returning an error with the String when failing.
     */
    public void isExpectedResult(
        IntegrationTestResult actualResult, Result expectedResult, String expectedResultString) {
      Result verdict = actualResult.cpaCheckerResult().getResult();

      if (verdict == expectedResult) {
        return;
      }

      String log = checkNotNull(actualResult.log()).trim();
      if (verdict == Result.NOT_YET_STARTED) {
        failWithoutActual(
            Fact.fact("analysis result expected to be", expectedResultString),
            Fact.fact("but was", verdict),
            Fact.fact("which has log", log));
      }

      failWithActual(
          Fact.fact("analysis result expected to be", expectedResultString),
          Fact.fact("but was", verdict),
          Fact.fact("due to", actualResult.cpaCheckerResult().getTargetDescription()),
          Fact.fact("which has log", log));
    }

    /**
     * Use this for checking assertions with Truth: <code>
     * assertUsing(context)).that(formula).is...()</code>.
     */
    public static SimpleSubjectBuilder<ProgramSubject, String> assertUsing(
        final Configuration analysis, final TemporaryFolder temporaryFolder) {
      return assert_().about(programSubjectOf(analysis, temporaryFolder));
    }

    /**
     * Use this for checking assertions about programs (given the corresponding analysis) with
     * Truth: <code>assert_().about(programSubjectOf(analysis)).that(pathToProgram).is...()</code>.
     */
    public static Subject.Factory<ProgramSubject, String> programSubjectOf(
        final Configuration analysis, final TemporaryFolder temporaryFolder) {
      return (metadata, pathToProgram) ->
          new ProgramSubject(metadata, pathToProgram, analysis, temporaryFolder);
    }
  }
}
