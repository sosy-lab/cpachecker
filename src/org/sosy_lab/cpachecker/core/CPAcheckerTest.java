// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core;

import com.google.common.base.Verify;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.sosy_lab.cpachecker.cfa.model.k3.K3CfaMetadata;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

/** Integration tests for CPAchecker. */
public class CPAcheckerTest {

  /** The configuration file to use for running CPAchecker. */
  private static final String CONFIGURATION_FILE_C = "config/valueAnalysis-NoCegar.properties";

  private static final String CONFIGURATION_FILE_K3 = "config/predicateAnalysis-k3.properties";
  private static final String CONFIGURATION_FILE_LLVM = "config/valueAnalysis-NoCegar.properties";
  private static final String CONFIGURATION_FILE_JAVA =
      "config/valueAnalysis-java-NoCegar.properties";

  private static final String SPECIFICATION_C = "config/specification/default.spc";
  // This is a dummy specification for K3 programs, since the actual specification is inside
  // the program itself, as annotations.
  private static final String SPECIFICATION_K3 = "config/specification/correct-tags.spc";
  // labels are removed in LLVM IR and assert_fail is renamed, so we need a different specification
  private static final String SPECIFICATION_LLVM = "config/specification/sv-comp-reachability.spc";
  private static final String SPECIFICATION_JAVA = "config/specification/JavaAssertion.spc";

  private static final String SAFE_PROGRAM_C = "doc/examples/example.c";
  private static final String UNSAFE_PROGRAM_C = "doc/examples/example_bug.c";

  private static final String SAFE_PROGRAM_K3 = "test/programs/k3/simple-correct.smt2";
  private static final String SAFE_LOOP_PROGRAM_K3 = "test/programs/k3/loop-simple.smt2";
  private static final String UNSAFE_PROGRAM_K3 = "test/programs/k3/simple-incorrect.smt2";

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
    Configuration config = getConfig(CONFIGURATION_FILE_C, Language.C, SPECIFICATION_C);

    // Code duplication in the later tests is on purpose; we don't want to hide the method calls
    // that are included in the test through indirection, as long as the tests stay as simple
    // as they currently are
    TestResults result = CPATestRunner.run(config, SAFE_PROGRAM_C);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsSafe();
  }

  @Test
  public void testRunForSafeK3Program() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_K3, Language.K3, SPECIFICATION_K3);
    TestResults result = CPATestRunner.run(config, SAFE_PROGRAM_K3);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsSafe();
  }

  @Test
  public void testWitnessExportForSafeK3Program() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_K3, Language.K3, SPECIFICATION_K3);
    TestResults result = CPATestRunner.run(config, SAFE_LOOP_PROGRAM_K3);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsSafe();

    Optional<K3CfaMetadata> k3CfaMetadataOptional =
        result.getCheckerResult().getCfa().getMetadata().getK3CfaMetadata();

    Verify.verify(
        k3CfaMetadataOptional.isPresent(),
        "K3 CFA Metadata should be present for every K3 program");
    K3CfaMetadata k3CfaMetadata = k3CfaMetadataOptional.orElseThrow();

    Verify.verify(
        k3CfaMetadata.exportWitness(),
        "For the safe K3 program '"
            + SAFE_LOOP_PROGRAM_K3
            + "', the witness export should be enabled");

    Optional<Path> witnessPath = k3CfaMetadata.getExportWitnessPath();
    Verify.verify(
        witnessPath.isPresent(),
        "For the safe K3 program '"
            + SAFE_LOOP_PROGRAM_K3
            + "', the witness path should be present after exporting the witness");

    // Read entire file content as a single string (UTF-8)
    // This is safe to do, since the witness files are small.
    String content = Files.readString(witnessPath.orElseThrow());
    Verify.verify(
        content.contains(":invariant"), "The witness should contain at least one invariant.");
    Verify.verify(
        content.contains("(annotate-tag"),
        "The witness should contain at least one annotate-tag command.");

    // TODO: Should we cleanup after the test by deleting the witness file?
    //      The other tests do not do this either, so for consistency we leave it as is for now.
  }

  @Test
  public void testRunForUnsafeK3Program() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_K3, Language.K3, SPECIFICATION_K3);
    TestResults result = CPATestRunner.run(config, UNSAFE_PROGRAM_K3);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsUnsafe();
  }

  @Test
  public void testRunForUnsafeCProgram() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_C, Language.C, SPECIFICATION_C);

    TestResults result = CPATestRunner.run(config, UNSAFE_PROGRAM_C);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsUnsafe();
  }

  @Test
  public void testRunForSafeJavaProgram() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_JAVA, Language.JAVA, SPECIFICATION_JAVA);

    TestResults result = CPATestRunner.run(config, SAFE_PROGRAM_JAVA);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsSafe();
  }

  @Test
  public void testRunForUnsafeJavaProgram() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_JAVA, Language.JAVA, SPECIFICATION_JAVA);

    TestResults result = CPATestRunner.run(config, UNSAFE_PROGRAM_JAVA);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsUnsafe();
  }

  @Test
  @Deprecated // cf. https://gitlab.com/sosy-lab/software/cpachecker/-/issues/1356
  @Ignore
  public void testRunForSafeLlvmProgram() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_LLVM, Language.LLVM, SPECIFICATION_LLVM);

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
    Configuration config = getConfig(CONFIGURATION_FILE_LLVM, Language.LLVM, SPECIFICATION_LLVM);

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

  private Configuration getConfig(
      String configurationFile, Language inputLanguage, String specificationFile)
      throws InvalidConfigurationException, IOException {

    // Do not use TestDataTools.configurationForTest() because we want output files
    Configuration configForFiles =
        Configuration.builder()
            .setOption("output.path", tempFolder.getRoot().getAbsolutePath())
            .build();
    FileTypeConverter fileTypeConverter = FileTypeConverter.create(configForFiles);
    Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);
    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder
        .loadFromFile(configurationFile)
        .setOption("language", inputLanguage.name())
        .setOption("specification", specificationFile)
        .setOption("java.classpath", JAVA_CLASSPATH)
        .addConverter(FileOption.class, fileTypeConverter);
    return configBuilder.build();
  }
}
