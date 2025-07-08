// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;

import com.google.common.io.ByteStreams;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class PointerAnalysisTest {
  private static final String CONFIGURATION_FILE = "config/pointer.properties";
  private static final String SPECIFICATION = "config/specification/default.spc";
  private static final String PROGRAM_C_SIMPLE = "test/programs/pointer/pointer.c";
  // private static final String JAVA_CLASSPATH = "test/programs/java/Statements/";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  // discard printed statistics; we only care about generation
  @SuppressWarnings("checkstyle:IllegalInstantiation") // ok for statistics
  private final PrintStream statisticsStream =
      new PrintStream(ByteStreams.nullOutputStream(), true, Charset.defaultCharset());

  @org.junit.Test
  public void testRunForSafeCProgram() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE, Language.C, SPECIFICATION);
    TestResults result = CPATestRunner.run(config, PROGRAM_C_SIMPLE);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();
    System.out.println(result.getLog());
    result.assertIsSafe();
  }

  private Configuration getConfig(
      String configurationFile, Language inputLanguage, String specificationFile)
      throws InvalidConfigurationException, IOException {

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
        // .setOption("java.classpath", JAVA_CLASSPATH)
        .setOption("parser.usePreprocessor", "true")
        .addConverter(FileOption.class, fileTypeConverter);
    return configBuilder.build();
  }
}
