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
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class CToSvLibTransformationAlgorithmTest {

  private void testTransformationToSvLib(Path pInputFilePath)
      throws InvalidConfigurationException,
          ParserException,
          IOException,
          InterruptedException,
          SvLibAstParseException {
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    Configuration config =
        TestDataTools.configurationForTest()
            .setOptions(
                ImmutableMap.of(
                    "cpa.predicate.encodeBitvectorAs",
                    "INTEGER",
                    "cpa.predicate.ignoreIrrelevantVariables",
                    "false",
                    "solver.solver",
                    "z3"))
            .build();
    CFACreator cfaCreator = new CFACreator(config, logger, shutdownNotifier);
    CFA inputCfa = cfaCreator.parseFileAndCreateCFA(ImmutableList.of(pInputFilePath.toString()));

    SvLibScript script;
    try (CToSvLibAlgorithm algorithm =
        new CToSvLibAlgorithm(config, logger, shutdownNotifier, inputCfa)) {
      script = algorithm.transformCfaToSvLib();
    } catch (Exception pE) {
      throw new RuntimeException(pE);
    }

    String scriptAsString = script.toASTString();
    SvLibToAstParser.parseScript(scriptAsString);
  }

  // *********************************** ToSvLibTransformation ***********************************

  /*
  private String examplesPathToSvLibTransformation() {
    return Path.of("test", "programs", "to_svlib_transformation").toAbsolutePath().toString();
  }

  @Test
  public void testSimpleDivision() throws Exception {
    Path inputFilePath = Path.of(examplesPathToSvLibTransformation(), "simple-division.c");
    testTransformationToSvLib(inputFilePath);
  }
  */

  // *********************************** CfaToCExport ***********************************

  // This test acutally only takes about 600 ms, but when running it in isolation within an IDE,
  // one needs to factor in a startup time of around 1 second.
  @Test(timeout = 3000)
  public void testAllCfaToC() throws Exception {
    Path directoryPath = Path.of("test", "programs", "cfa_to_c_export").toAbsolutePath();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.c")) {
      for (Path path : stream) {
        testTransformationToSvLib(path);
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
    testTransformationToSvLib(inputFilePath);
  }

  @Test(timeout = 1800)
  public void testFunctionReturn() throws Exception {
    Path inputFilePath = Path.of(examplesPathProgramTranslation(), "functionreturn.c");
    testTransformationToSvLib(inputFilePath);
  }

  // *********************************** Real C ***********************************

  private String examplesPathRealC() {
    return Path.of("test", "programs", "realc").toAbsolutePath().toString();
  }

  @Test(timeout = 1800)
  public void testTestOr() throws Exception {
    Path inputFilePath = Path.of(examplesPathRealC(), "test-or.c");
    testTransformationToSvLib(inputFilePath);
  }

  @Test(timeout = 1800)
  public void testRandom() throws Exception {
    Path inputFilePath = Path.of(examplesPathRealC(), "random.c");
    testTransformationToSvLib(inputFilePath);
  }
}
