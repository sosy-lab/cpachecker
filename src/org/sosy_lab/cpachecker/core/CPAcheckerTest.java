// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.PrintStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

/** Integration tests for CPAchecker. */
public class CPAcheckerTest {

  /** The configuration file to use for running CPAchecker. */
  private static final String CONFIGURATION_FILE_C = "config/valueAnalysis-NoCegar.properties";

  private static final String CONFIGURATION_FILE_LLVM = "config/valueAnalysis-NoCegar.properties";
  private static final String CONFIGURATION_FILE_JAVA =
      "config/valueAnalysis-java-NoCegar.properties";

  private static final String SPECIFICATION_C = "config/specification/default.spc";
  // labels are removed in LLVM IR and assert_fail is renamed, so we need a different specification
  private static final String SPECIFICATION_LLVM = "config/specification/sv-comp-reachability.spc";
  private static final String SPECIFICATION_JAVA = "config/specification/JavaAssertion.spc";

  private static final String SAFE_PROGRAM_C = "doc/examples/example.c";
  private static final String UNSAFE_PROGRAM_C = "doc/examples/example_bug.c";

  private static final String SAFE_PROGRAM_LLVM = "test/programs/llvm/functionCall.ll";
  private static final String UNSAFE_PROGRAM_LLVM = "test/programs/llvm/functionCall2.ll";

  private static final String JAVA_CLASSPATH = "test/programs/java/Statements/";
  private static final String SAFE_PROGRAM_JAVA = "Switch_true_assert";
  private static final String UNSAFE_PROGRAM_JAVA = "Switch2_false_assert";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testRunForSafeCProgram() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_C, Language.C, SPECIFICATION_C);
    // discard printed statistics; we only care about generation
    PrintStream statisticsStream = new PrintStream(ByteStreams.nullOutputStream());

    TestResults result = CPATestRunner.run(config, SAFE_PROGRAM_C);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsSafe();
  }

  @Test
  public void testRunForUnsafeCProgram() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_C, Language.C, SPECIFICATION_C);
    // discard printed statistics; we only care about generation
    PrintStream statisticsStream = new PrintStream(ByteStreams.nullOutputStream());

    TestResults result = CPATestRunner.run(config, UNSAFE_PROGRAM_C);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsUnsafe();
  }

  @Test
  public void testRunForSafeJavaProgram() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_JAVA, Language.JAVA, SPECIFICATION_JAVA);
    // discard printed statistics; we only care about generation
    PrintStream statisticsStream = new PrintStream(ByteStreams.nullOutputStream());

    TestResults result = CPATestRunner.run(config, SAFE_PROGRAM_JAVA);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsSafe();
  }

  @Test
  public void testRunForUnsafeJavaProgram() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_JAVA, Language.JAVA, SPECIFICATION_JAVA);
    // discard printed statistics; we only care about generation
    PrintStream statisticsStream = new PrintStream(ByteStreams.nullOutputStream());

    TestResults result = CPATestRunner.run(config, UNSAFE_PROGRAM_JAVA);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsUnsafe();
  }

  @Test
  public void testRunForSafeLlvmProgram() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_LLVM, Language.LLVM, SPECIFICATION_LLVM);
    // discard printed statistics; we only care about generation
    PrintStream statisticsStream = new PrintStream(ByteStreams.nullOutputStream());

    TestResults result = CPATestRunner.run(config, SAFE_PROGRAM_LLVM);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIsSafe();
  }

  @Test
  public void testRunForUnsafeLlvmProgram() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_LLVM, Language.LLVM, SPECIFICATION_LLVM);
    // discard printed statistics; we only care about generation
    PrintStream statisticsStream = new PrintStream(ByteStreams.nullOutputStream());

    TestResults result = CPATestRunner.run(config, UNSAFE_PROGRAM_LLVM);
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
