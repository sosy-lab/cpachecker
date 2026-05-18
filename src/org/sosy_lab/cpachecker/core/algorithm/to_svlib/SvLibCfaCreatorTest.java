// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibAstParseException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

public class SvLibCfaCreatorTest {

  private String examplesPathCfaToSvLibLocal() {
    return Path.of("test", "programs", "to_svlib_transformation", "transformed")
        .toAbsolutePath()
        .toString();
  }

  private void testFileInput(Path inputFilePath)
      throws InvalidConfigurationException, ParserException, IOException, InterruptedException {
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    Configuration innerConfig =
        Configuration.builder()
            .loadFromFile(Path.of("config", "predicateAnalysis-svlib.properties").toAbsolutePath())
            .build();
    CFACreator cfaCreator = new CFACreator(innerConfig, logger, shutdownNotifier);

    cfaCreator.parseFileAndCreateCFA(ImmutableList.of(inputFilePath.toString()));
  }

  private void testStringInput(Path inputPath)
      throws InvalidConfigurationException,
          IOException,
          SvLibAstParseException,
          ParserException,
          InterruptedException {

    String programString;
    try {
      programString = Joiner.on("\n").join(Files.readAllLines(inputPath));
    } catch (IOException e) {
      throw new SvLibAstParseException("Could not read input file: " + inputPath, e);
    }
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    Configuration innerConfig =
        Configuration.builder()
            .loadFromFile(Path.of("config", "predicateAnalysis-svlib.properties").toAbsolutePath())
            .build();
    CFACreator cfaCreator = new CFACreator(innerConfig, logger, shutdownNotifier);

    cfaCreator.parseSourceAndCreateCFA(programString);
  }

  @Test
  public void testSimpleGlobalVar() throws Exception {
    Path inputFilePath = Path.of(examplesPathCfaToSvLibLocal(), "simple-global-var.svlib");
    testFileInput(inputFilePath);
  }

  @Test
  public void testSimpleGlobalVar_String() throws Exception {
    Path inputFilePath = Path.of(examplesPathCfaToSvLibLocal(), "simple-global-var.svlib");
    testStringInput(inputFilePath);
  }

  @Test
  public void testLoopV1() throws Exception {
    Path inputFilePath = Path.of(examplesPathCfaToSvLibLocal(), "loopv1.svlib");
    testFileInput(inputFilePath);
  }

  @Test
  public void testLoopV1_String() throws Exception {
    Path inputFilePath = Path.of(examplesPathCfaToSvLibLocal(), "loopv1.svlib");
    testStringInput(inputFilePath);
  }
}
