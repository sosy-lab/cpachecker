// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.junit.AssumptionViolatedException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibCfaMetadata;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

/** Integration tests for CPAchecker. */
public class CPAcheckerTest {

  /** The configuration file to use for running CPAchecker. */
  private static final String CONFIGURATION_FILE_C = "config/valueAnalysis-NoCegar.properties";

  private static final String CONFIGURATION_FILE_SvLib =
      "config/predicateAnalysis-svlib.properties";
  private static final String CONFIGURATION_FILE_LLVM = "config/valueAnalysis-NoCegar.properties";
  private static final String CONFIGURATION_FILE_JAVA =
      "config/valueAnalysis-java-NoCegar.properties";

  private static final String SPECIFICATION_C = "config/specification/default.spc";
  // This is a dummy specification for SV-LIB programs, since the actual specification is inside
  // the program itself, as annotations.
  private static final String SPECIFICATION_SvLib = "config/specification/correct-tags.spc";
  // labels are removed in LLVM IR and assert_fail is renamed, so we need a different specification
  private static final String SPECIFICATION_LLVM = "config/specification/sv-comp-reachability.spc";
  private static final String SPECIFICATION_JAVA = "config/specification/JavaAssertion.spc";

  private static final String SAFE_PROGRAM_C = "doc/examples/example.c";
  private static final String UNSAFE_PROGRAM_C = "doc/examples/example_bug.c";

  private static final String SAFE_PROGRAM_SvLib = "test/programs/sv-lib/simple-correct.svlib";
  private static final String SAFE_LOOP_PROGRAM_SvLib =
      "test/programs/sv-lib/loop-simple-safe.svlib";
  private static final String UNSAFE_PROGRAM_SvLib = "test/programs/sv-lib/simple-incorrect.svlib";
  private static final String UNSAFE_LOOP_PROGRAM_SvLib =
      "test/programs/sv-lib/loop-simple-unsafe.svlib";

  private static final String SAFE_PROGRAM_LLVM = "test/programs/llvm/functionCall.ll";
  private static final String UNSAFE_PROGRAM_LLVM = "test/programs/llvm/functionCall2.ll";

  private static final String JAVA_CLASSPATH = "test/programs/java/Statements/";
  private static final String SAFE_PROGRAM_JAVA = "Switch_true_assert";
  private static final String UNSAFE_PROGRAM_JAVA = "Switch2_false_assert";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  // discard printed statistics; we only care about generation
  @SuppressWarnings("checkstyle:IllegalInstantiation") // ok for statistics
  private final PrintStream statisticsStream =
      new PrintStream(ByteStreams.nullOutputStream(), true, Charset.defaultCharset());

  @Test
  public void testRunForSafeCProgram() throws Exception {
    Configuration config =
        getConfigWithOutputFiles(CONFIGURATION_FILE_C, Language.C, SPECIFICATION_C);

    // Code duplication in the later tests is on purpose; we don't want to hide the method calls
    // that are included in the test through indirection, as long as the tests stay as simple
    // as they currently are
    TestResults result = CPATestRunner.run(config, SAFE_PROGRAM_C);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsSafe();
  }

  @Test
  public void testRunForSafeSvLibProgram() throws Exception {
    Configuration config =
        getConfigWithOutputFiles(CONFIGURATION_FILE_SvLib, Language.SVLIB, SPECIFICATION_SvLib);
    TestResults result = CPATestRunner.run(config, SAFE_PROGRAM_SvLib);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsSafe();
  }

  private Configuration svLibConfigWithWitnessOutput(Path witnessOutputPath)
      throws InvalidConfigurationException, IOException {
    return Configuration.builder()
        .copyFrom(
            getConfigWithOutputFiles(CONFIGURATION_FILE_SvLib, Language.SVLIB, SPECIFICATION_SvLib))
        .setOption("output.path", witnessOutputPath.getParent().toString())
        .setOption("counterexample.export.svlib", witnessOutputPath.getFileName().toString())
        .setOption("cpa.arg.svLibCorrectnessWitness", witnessOutputPath.getFileName().toString())
        .build();
  }

  private String obtainSvLibWitnessContentCheckingOutputCorrectness(
      TestResults pResult, Path pWitnessOutputPath, String pProgramPath) throws IOException {

    assertWithMessage("CFA should be present in the result")
        .that(pResult.getCheckerResult().getCfa())
        .isNotNull();

    Optional<SvLibCfaMetadata> svLibCfaMetadataOptional =
        Objects.requireNonNull(pResult.getCheckerResult().getCfa())
            .getMetadata()
            .getSvLibCfaMetadata();

    assertWithMessage("SV-LIB CFA Metadata should be present for every SV-LIB program")
        .that(svLibCfaMetadataOptional)
        .isPresent();

    assertWithMessage("SV-LIB CFA Metadata should be present for every SV-LIB program")
        .that(svLibCfaMetadataOptional)
        .isPresent();

    assertWithMessage(
            "For the safe SV-LIB program '%s', the witness path should be present after exporting"
                + " the witness",
            pProgramPath)
        .that(Files.exists(pWitnessOutputPath))
        .isTrue();

    // Read entire file content as a single string (UTF-8)
    // This is safe to do, since the witness files are small.
    return Files.readString(pWitnessOutputPath);
  }

  @Test
  public void testWitnessExportForSafeSvLibProgram() throws Exception {
    Path witnessOutputPath = Path.of(tempFolder.getRoot().getAbsolutePath(), "witness.svlib");
    Configuration config = svLibConfigWithWitnessOutput(witnessOutputPath);
    TestResults result = CPATestRunner.run(config, SAFE_LOOP_PROGRAM_SvLib);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsSafe();

    String content =
        obtainSvLibWitnessContentCheckingOutputCorrectness(
            result, witnessOutputPath, SAFE_LOOP_PROGRAM_SvLib);

    assertWithMessage("The witness should contain at least one invariant.")
        .that(content)
        .contains(":invariant");
    assertWithMessage("The witness should contain at least one annotate-tag command.")
        .that(content)
        .contains("annotate-tag");
  }

  @Test
  public void testRunForUnsafeSvLibProgram() throws Exception {
    Configuration config =
        getConfigWithOutputFiles(CONFIGURATION_FILE_SvLib, Language.SVLIB, SPECIFICATION_SvLib);
    TestResults result = CPATestRunner.run(config, UNSAFE_PROGRAM_SvLib);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsUnsafe();
  }

  @Test
  public void testWitnessExportForUnsafeSvLibProgram() throws Exception {
    Path witnessOutputPath = Path.of(tempFolder.getRoot().getAbsolutePath(), "witness.svlib");
    Configuration config = svLibConfigWithWitnessOutput(witnessOutputPath);
    TestResults result = CPATestRunner.run(config, UNSAFE_LOOP_PROGRAM_SvLib);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsUnsafe();

    String content =
        obtainSvLibWitnessContentCheckingOutputCorrectness(
            result, witnessOutputPath, UNSAFE_LOOP_PROGRAM_SvLib);

    assertWithMessage("The witness should contain at least one error-path.")
        .that(content)
        .contains("select-trace");
  }

  @Test
  public void testRunForUnsafeCProgram() throws Exception {
    Configuration config =
        getConfigWithOutputFiles(CONFIGURATION_FILE_C, Language.C, SPECIFICATION_C);

    TestResults result = CPATestRunner.run(config, UNSAFE_PROGRAM_C);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsUnsafe();
  }

  @Test
  public void testRunForSafeJavaProgram() throws Exception {
    Configuration config =
        getConfigWithOutputFiles(CONFIGURATION_FILE_JAVA, Language.JAVA, SPECIFICATION_JAVA);

    TestResults result = CPATestRunner.run(config, SAFE_PROGRAM_JAVA);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsSafe();
  }

  @Test
  public void testRunForUnsafeJavaProgram() throws Exception {
    Configuration config =
        getConfigWithOutputFiles(CONFIGURATION_FILE_JAVA, Language.JAVA, SPECIFICATION_JAVA);

    TestResults result = CPATestRunner.run(config, UNSAFE_PROGRAM_JAVA);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsUnsafe();
  }

  @Test
  @Deprecated // cf. https://gitlab.com/sosy-lab/software/cpachecker/-/issues/1356
  @Ignore
  public void testRunForSafeLlvmProgram() throws Exception {
    Configuration config =
        getConfigWithOutputFiles(CONFIGURATION_FILE_LLVM, Language.LLVM, SPECIFICATION_LLVM);

    TestResults result;
    try {
      result = CPATestRunner.run(config, SAFE_PROGRAM_LLVM);
    } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
      throw new AssumptionViolatedException("LLVM library could not be loaded, aborting test", e);
    }
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsSafe();
  }

  @Test
  @Deprecated // cf. https://gitlab.com/sosy-lab/software/cpachecker/-/issues/1356
  @Ignore
  public void testRunForUnsafeLlvmProgram() throws Exception {
    Configuration config =
        getConfigWithOutputFiles(CONFIGURATION_FILE_LLVM, Language.LLVM, SPECIFICATION_LLVM);

    TestResults result;
    try {
      result = CPATestRunner.run(config, UNSAFE_PROGRAM_LLVM);
    } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
      throw new AssumptionViolatedException("LLVM library could not be loaded, aborting test", e);
    }

    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsUnsafe();
  }

  protected Configuration getConfigWithOutputFiles(
      String configurationFile, Language inputLanguage, String specificationFile)
      throws InvalidConfigurationException, IOException {

    // Do not use TestDataTools.configurationForTest() because we want output files
    Configuration configForFiles =
        Configuration.builder()
            .setOption("output.path", tempFolder.getRoot().getAbsolutePath())
            .build();
    return setUpConfiguration(
        configurationFile, inputLanguage, specificationFile, configForFiles, MachineModel.LINUX32);
  }

  public static Configuration setUpConfiguration(
      String configurationFile,
      Language inputLanguage,
      String specificationFile,
      Configuration pConfigForFiles,
      MachineModel machineModel)
      throws InvalidConfigurationException, IOException {
    FileTypeConverter fileTypeConverter = FileTypeConverter.create(pConfigForFiles);
    Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);
    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder
        .loadFromFile(configurationFile)
        .setOption("analysis.machineModel", machineModel.toString())
        .setOption("language", inputLanguage.name())
        .setOption("specification", specificationFile)
        .setOption("java.classpath", JAVA_CLASSPATH)
        .addConverter(FileOption.class, fileTypeConverter);
    return configBuilder.build();
  }
}
