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
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibToAstParser.SvLibAstParseException;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibScript;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/**
 * Tests that the transformation of a C program into an SV-LIB script produces a script that can be
 * parsed again.
 *
 * <p>The SMT solver is only used to build the formulas that describe the effect of the CFA edges,
 * so the transformation has to work with every solver. There is one test per combination of a
 * solver and an encoding of the bitvectors, because both influence which formulas are built.
 *
 * <p>Z3 is not among the tested solvers, although the transformation supports it: only a single
 * native SMT solver library can be used per JVM, because Z3 and MathSAT5 both bundle GMP, and
 * creating a Z3 context after a MathSAT5 context crashes the JVM. All tests of a class run in the
 * same JVM, so MathSAT5, which the configurations of the transformation use, and Z3 cannot both be
 * tested here. SMTInterpol is written in Java and therefore has no such conflict.
 */
public class CToSvLibTransformationTest {

  private static final String ENCODE_BITVECTORS_AS_INTEGERS = "INTEGER";
  private static final String ENCODE_BITVECTORS_AS_BITVECTORS = "BITVECTOR";

  /** The programs that are transformed, given as directories and as single files. */
  private static final ImmutableList<Path> INPUT_DIRECTORIES =
      ImmutableList.of(Path.of("test", "programs", "cfa_to_c_export"));

  private static final ImmutableList<Path> INPUT_FILES =
      ImmutableList.of(
          Path.of("test", "programs", "to_svlib_transformation", "simple-division.c"),
          Path.of(
              "test", "programs", "to_svlib_transformation", "nondeterministic-value-in-loop.c"),
          Path.of("test", "programs", "to_svlib_transformation", "allocation-in-loop.c"),
          Path.of("test", "programs", "to_svlib_transformation", "allocation-in-loop-field.c"),
          Path.of("test", "programs", "to_svlib_transformation", "reserved-word-variables.c"),
          Path.of("test", "programs", "programtranslation", "gotos.c"),
          Path.of("test", "programs", "programtranslation", "functionreturn.c"),
          Path.of("test", "programs", "realc", "test-or.c"),
          Path.of("test", "programs", "realc", "random.c"));

  /** Transform and parse all input programs with the given solver and bitvector encoding. */
  private void transformAllPrograms(
      String pSolver, String pBitVectorEncoding, Map<String, String> pAdditionalOptions)
      throws Exception {
    ImmutableList.Builder<Path> inputFiles = ImmutableList.builder();
    inputFiles.addAll(INPUT_FILES);
    for (Path directory : INPUT_DIRECTORIES) {
      try (DirectoryStream<Path> stream =
          Files.newDirectoryStream(directory.toAbsolutePath(), "*.c")) {
        stream.forEach(inputFiles::add);
      } catch (IOException e) {
        throw new SvLibAstParseException("Could not read the input files of " + directory, e);
      }
    }

    for (Path inputFile : inputFiles.build()) {
      transformAndParse(
          inputFile.toAbsolutePath(), pSolver, pBitVectorEncoding, pAdditionalOptions);
    }
  }

  private void transformAndParse(
      Path pInputFile,
      String pSolver,
      String pBitVectorEncoding,
      Map<String, String> pAdditionalOptions)
      throws Exception {
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    Configuration config =
        TestUtils.configurationForTest()
            .setOptions(
                ImmutableMap.<String, String>builder()
                    .put("cpa.predicate.encodeBitvectorAs", pBitVectorEncoding)
                    .put("cpa.predicate.ignoreIrrelevantVariables", "false")
                    .put("solver.solver", pSolver)
                    .putAll(pAdditionalOptions)
                    .buildOrThrow())
            .build();
    CFACreator cfaCreator = new CFACreator(config, logger, shutdownNotifier);
    CFA inputCfa = cfaCreator.parseFileAndCreateCFA(ImmutableList.of(pInputFile.toString()));

    SvLibScript script;
    try (CToSvLibAlgorithm algorithm =
        new CToSvLibAlgorithm(
            config, Specification.alwaysSatisfied(), logger, shutdownNotifier, inputCfa)) {
      script = algorithm.transformCfaToSvLibScript();
    }

    SvLibToAstParser.parseScript(script.toASTString());
  }

  @Test
  public void testMathsat5WithIntegerEncoding() throws Exception {
    transformAllPrograms("mathsat5", ENCODE_BITVECTORS_AS_INTEGERS, ImmutableMap.of());
  }

  @Test
  public void testMathsat5WithBitvectorEncoding() throws Exception {
    transformAllPrograms("mathsat5", ENCODE_BITVECTORS_AS_BITVECTORS, ImmutableMap.of());
  }

  @Test
  public void testSmtInterpolWithIntegerEncoding() throws Exception {
    // SMTInterpol supports neither bitvectors nor floats, so both are encoded with other theories.
    transformAllPrograms(
        "smtinterpol",
        ENCODE_BITVECTORS_AS_INTEGERS,
        ImmutableMap.of("cpa.predicate.encodeFloatAs", "RATIONAL"));
  }
}
