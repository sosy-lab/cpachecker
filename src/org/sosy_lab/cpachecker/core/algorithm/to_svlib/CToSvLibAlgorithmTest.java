// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibAstParseException;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibScript;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class CToSvLibAlgorithmTest {

  private final String encodeBitvectorsAsIntegersOption = "INTEGER";
  private final String encodeBitvectorsAsBitvectorsOption = "BITVECTOR";

  private void testTransformationToSvLib(Path pInputFilePath, String bitVectorEncoding)
      throws InvalidConfigurationException,
          ParserException,
          IOException,
          InterruptedException,
          SvLibAstParseException,
          CPATransferException {
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOptions(
                ImmutableMap.of(
                    "cpa.predicate.encodeBitvectorAs",
                    bitVectorEncoding,
                    "cpa.predicate.ignoreIrrelevantVariables",
                    "false",
                    "solver.solver",
                    "z3"))
            .build();
    CFACreator cfaCreator = new CFACreator(config, logger, shutdownNotifier);
    CFA inputCfa = cfaCreator.parseFileAndCreateCFA(ImmutableList.of(pInputFilePath.toString()));

    SvLibScript script;
    try (CToSvLibAlgorithm algorithm =
        new CToSvLibAlgorithm(
            config, Specification.alwaysSatisfied(), logger, shutdownNotifier, inputCfa)) {
      script = algorithm.transformCfaToSvLibScript();
    }

    String scriptAsString = script.toASTString();
    SvLibToAstParser.parseScript(scriptAsString);
  }

  // *********************************** Test for config file ***********************************
  private void transformationConfigFileTest(Path pInputFilePath) throws Exception {
    Configuration config =
        TestDataTools.configurationForTest()
            .loadFromFile(Path.of("config/transformToSvLib.properties"))
            .build();

    CPATestRunner.run(config, pInputFilePath.toString());
  }

  @Test
  public void testSimple_File() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "simple-division.c");
    transformationConfigFileTest(inputFilePath);
  }

  // *********************************** ToSvLibTransformation ***********************************

  private String examplesPathToSvLibTransformation() {
    return Path.of("test", "programs", "to_svlib_transformation").toAbsolutePath().toString();
  }

  @Test(timeout = 1800)
  public void testSimpleDivision() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "simple-division.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsIntegersOption);
  }

  // *********************************** CfaToCExport ***********************************

  // This test actually only takes about 600 ms, but when running it in isolation within an IDE,
  // one needs to factor in a startup time of around 1 second.
  @Test(timeout = 3000)
  public void testAllCfaToC() throws Exception {
    Path directoryPath = Path.of("test", "programs", "cfa_to_c_export").toAbsolutePath();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.c")) {
      for (Path path : stream) {
        testTransformationToSvLib(path, encodeBitvectorsAsIntegersOption);
      }
    } catch (IOException e) {
      throw new SvLibAstParseException("Could not read input files", e);
    }
  }

  // *********************************** Programtranslation  ***********************************

  private String examplesPathProgramTranslation() {
    return Path.of("test", "programs", "programtranslation").toAbsolutePath().toString();
  }

  @Test(timeout = 1800)
  public void testGotos() throws Exception {
    Path inputFilePath = Path.of(examplesPathProgramTranslation(), "gotos.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsIntegersOption);
  }

  // FIXME casting incompatible types in procedure call with ITE does not yet work for bitvectors
  /*@Test(timeout = 1800)
  public void testGotosBitvectorEncoding() throws Exception {
    Path inputFilePath = Path.of(examplesPathProgramTranslation(), "gotos.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsBitvectorsOption);
  }*/

  @Test(timeout = 1800)
  public void testFunctionReturn() throws Exception {
    Path inputFilePath = Path.of(examplesPathProgramTranslation(), "functionreturn.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsIntegersOption);
  }

  // *********************************** Real C ***********************************

  private String examplesPathRealC() {
    return Path.of("test", "programs", "realc").toAbsolutePath().toString();
  }

  @Test(timeout = 1800)
  public void testTestOr() throws Exception {
    Path inputFilePath = Path.of(examplesPathRealC(), "test-or.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsIntegersOption);
  }

  @Test(timeout = 1800)
  public void testRandom() throws Exception {
    Path inputFilePath = Path.of(examplesPathRealC(), "random.c");
    testTransformationToSvLib(inputFilePath, encodeBitvectorsAsIntegersOption);
  }
}
