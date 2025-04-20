// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Year;
import java.time.ZoneId;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqWriter.FileExtension;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqLeftHandSideBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.GhostVariableUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdgeBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadUtil;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Sequentialization {

  private static final String license = "Apache-2.0";

  private final ImmutableList<LineOfCode> licenseHeader =
      ImmutableList.of(
          LineOfCode.of(0, "// This file is part of CPAchecker,"),
          LineOfCode.of(0, "// a tool for configurable software verification:"),
          LineOfCode.of(0, "// https://cpachecker.sosy-lab.org"),
          LineOfCode.of(0, "//"),
          LineOfCode.of(
              0,
              "// SPDX-"
                  + "FileCopyrightText: "
                  + Year.now(ZoneId.systemDefault()).getValue()
                  + " Dirk Beyer <https://www.sosy-lab.org>"),
          LineOfCode.of(0, "//"),
          // splitting this with + so that 'reuse lint' accepts it
          LineOfCode.of(0, "// SPDX-" + "License-" + "Identifier: " + license));

  private static final ImmutableList<LineOfCode> mporHeader =
      ImmutableList.of(
          LineOfCode.of(
              0,
              "// This sequentialization (transformation of a concurrent program into an"
                  + " equivalent"),
          LineOfCode.of(
              0,
              "// sequential program) was created by the MPORAlgorithm implemented in CPAchecker."),
          LineOfCode.of(0, "//"),
          LineOfCode.of(
              0,
              "// Assertion fails from the function "
                  + SeqToken.__SEQUENTIALIZATION_ERROR__
                  + " mark faulty sequentializations."),
          LineOfCode.of(0, "// All other assertion fails are induced by faulty input programs."),
          LineOfCode.empty());

  public static final String inputReachErrorDummy =
      SeqExpressionBuilder.buildReachError(
                  SeqToken.__FILE_NAME_PLACEHOLDER__, -1, SeqToken.__PRETTY_FUNCTION__)
              .toASTString()
          + SeqSyntax.SEMICOLON;

  public static final String outputReachErrorDummy =
      SeqExpressionBuilder.buildReachError(
                  SeqToken.__FILE_NAME_PLACEHOLDER__, -1, SeqToken.__SEQUENTIALIZATION_ERROR__)
              .toASTString()
          + SeqSyntax.SEMICOLON;

  public static final int INIT_PC = 0;

  public static final int EXIT_PC = -1;

  private static final int FIRST_LINE = 1;

  private final ImmutableList<MPORSubstitution> substitutions;

  private final MPOROptions options;

  private final String inputFileName;

  private final String outputFileName;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final ShutdownNotifier shutdownNotifier;

  private final LogManager logger;

  private final PcVariables pcVariables;

  public Sequentialization(
      ImmutableList<MPORSubstitution> pSubstitutions,
      MPOROptions pOptions,
      String pInputFileName,
      String pOutputFileName,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger) {

    substitutions = pSubstitutions;
    inputFileName = pInputFileName;
    outputFileName = pOutputFileName;
    options = pOptions;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    pcVariables =
        new PcVariables(
            SeqLeftHandSideBuilder.buildPcLeftHandSides(pSubstitutions.size(), options.scalarPc));
  }

  @Override
  public String toString() {
    try {
      ImmutableList<LineOfCode> initProgram = initProgram();
      ImmutableList<LineOfCode> finalProgram = finalProgram(initProgram);
      String program = LineOfCodeUtil.buildString(finalProgram);
      return options.validateParse && options.inputTypeDeclarations
          ? SeqValidator.validateProgramParsing(program, options, shutdownNotifier, logger)
          : program;

    } catch (UnrecognizedCodeException
        | InvalidConfigurationException
        | ParserException
        | InterruptedException e) {
      // we convert to RuntimeExceptions for unit tests
      logger.log(Level.SEVERE, e);
      throw new RuntimeException(e);
    }
  }

  /** Generates and returns the sequentialized program that contains dummy reach_error calls. */
  private ImmutableList<LineOfCode> initProgram() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> rProgram = ImmutableList.builder();

    // first initialize some variables needed for the declarations and definitions
    ImmutableSet<MPORThread> threads = SubstituteUtil.extractThreads(substitutions);
    MPORThread mainThread = ThreadUtil.extractMainThread(threads);
    MPORSubstitution mainThreadSubstitution =
        substitutions.stream().filter(s -> s.thread.equals(mainThread)).findAny().orElseThrow();
    ImmutableMap<ThreadEdge, SubstituteEdge> substituteEdges =
        SubstituteEdgeBuilder.substituteEdges(options, substitutions);
    BitVectorVariables bitVectorVariables = GhostVariableUtil.buildBitVectors(options, threads);
    ThreadSimulationVariables threadSimulationVariables =
        GhostVariableUtil.buildThreadSimulationVariables(options, threads, substituteEdges);

    // add bit vector type (before, otherwise parse error) and all input program type declarations
    rProgram.addAll(LineOfCodeUtil.buildBitVectorTypeDeclarations());
    rProgram.addAll(LineOfCodeUtil.buildOriginalDeclarations(options, threads));
    // add function, struct, variable declarations in the order: global, local, parameters
    rProgram.addAll(LineOfCodeUtil.buildGlobalDeclarations(options, mainThreadSubstitution));
    rProgram.addAll(LineOfCodeUtil.buildLocalDeclarations(options, substitutions));
    rProgram.addAll(LineOfCodeUtil.buildParameterDeclarations(options, substitutions));

    // add variable declarations for ghost variables
    rProgram.addAll(
        LineOfCodeUtil.buildThreadSimulationVariableDeclarations(
            options, threadSimulationVariables));

    // add custom function declarations and definitions
    rProgram.addAll(LineOfCodeUtil.buildFunctionDeclarations(options));
    rProgram.addAll(
        LineOfCodeUtil.buildFunctionDefinitions(
            options,
            substitutions,
            substituteEdges,
            bitVectorVariables,
            pcVariables,
            threadSimulationVariables,
            binaryExpressionBuilder,
            logger));

    return rProgram.build();
  }

  /**
   * Adds the license and sequentialization comments at the top of pInitProgram and replaces the
   * file name and line in {@code reach_error("__FILE_NAME_PLACEHOLDER__", -1,
   * "__SEQUENTIALIZATION_ERROR__");} with pOutputFileName and the actual line.
   */
  private ImmutableList<LineOfCode> finalProgram(ImmutableList<LineOfCode> pInitProgram) {
    // consider license and seq comment header for line numbers
    int currentLine =
        options.comments ? licenseHeader.size() + mporHeader.size() + FIRST_LINE : FIRST_LINE;
    ImmutableList.Builder<LineOfCode> rProgram = ImmutableList.builder();
    if (options.license) {
      rProgram.addAll(licenseHeader);
    }
    if (options.comments) {
      rProgram.addAll(mporHeader);
    }
    for (LineOfCode lineOfCode : pInitProgram) {
      // replace dummy line numbers (-1) with actual line numbers in the seq
      rProgram.add(replaceReachErrorDummies(lineOfCode, currentLine));
      currentLine++;
    }
    return rProgram.build();
  }

  /**
   * Replaces dummy calls to {@code reach_error}, or returns {@code pLineOfCode} as is if there is
   * none.
   */
  private LineOfCode replaceReachErrorDummies(LineOfCode pLineOfCode, int pLineNumber) {
    String code = pLineOfCode.code;

    if (code.contains(inputReachErrorDummy)) {
      // reach_error calls from the input program
      CFunctionCallExpression reachErrorCall =
          SeqExpressionBuilder.buildReachError(
              inputFileName, pLineNumber, SeqToken.__PRETTY_FUNCTION__);
      String replacement =
          code.replace(inputReachErrorDummy, reachErrorCall.toASTString() + SeqSyntax.SEMICOLON);
      return pLineOfCode.copyWithCode(replacement);

    } else if (code.contains(outputReachErrorDummy)) {
      // reach_error calls injected by the sequentialization
      CFunctionCallExpression reachErrorCall =
          SeqExpressionBuilder.buildReachError(
              outputFileName + FileExtension.I.suffix,
              pLineNumber,
              SeqToken.__SEQUENTIALIZATION_ERROR__);
      String replacement =
          code.replace(outputReachErrorDummy, reachErrorCall.toASTString() + SeqSyntax.SEMICOLON);
      return pLineOfCode.copyWithCode(replacement);
    }
    return pLineOfCode;
  }
}
