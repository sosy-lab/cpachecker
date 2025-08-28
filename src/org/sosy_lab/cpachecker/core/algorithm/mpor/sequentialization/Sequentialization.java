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
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqWriter.FileExtension;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqLeftHandSideBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModelBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdgeBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Sequentialization {

  // TODO move all this to hardcoded strings?

  private static final String license = "Apache-2.0";

  private final ImmutableList<LineOfCode> licenseHeader =
      ImmutableList.of(
          LineOfCode.of("// This file is part of CPAchecker,"),
          LineOfCode.of("// a tool for configurable software verification:"),
          LineOfCode.of("// https://cpachecker.sosy-lab.org"),
          LineOfCode.of("//"),
          LineOfCode.of(
              "// SPDX-"
                  + "FileCopyrightText: "
                  + Year.now(ZoneId.systemDefault()).getValue()
                  + " Dirk Beyer <https://www.sosy-lab.org>"),
          LineOfCode.of("//"),
          // splitting this with + so that 'reuse lint' accepts it
          LineOfCode.of("// SPDX-" + "License-" + "Identifier: " + license));

  private static final ImmutableList<LineOfCode> mporHeader =
      ImmutableList.of(
          LineOfCode.of(
              "// This sequentialization (transformation of a concurrent program into an"
                  + " equivalent"),
          LineOfCode.of(
              "// sequential program) was created by the MPORAlgorithm implemented in CPAchecker."),
          LineOfCode.of("//"),
          LineOfCode.of(
              "// Assertion fails from the function "
                  + SeqToken.__SEQUENTIALIZATION_ERROR__
                  + " mark faulty sequentializations."),
          LineOfCode.of("// All other assertion fails are induced by faulty input programs."));

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

  public static final String defaultCaseClauseError =
      SeqToken._default
          + SeqSyntax.COLON
          + SeqSyntax.SPACE
          + Sequentialization.outputReachErrorDummy;

  public static final int INIT_PC = 1;

  public static final int EXIT_PC = 0;

  public static final int MAIN_THREAD_ID = 0;

  private static final int FIRST_LINE = 1;

  private final MPOROptions options;

  private final ImmutableList<MPORSubstitution> substitutions;

  private final String inputFileName;

  private final String outputFileName;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final ShutdownNotifier shutdownNotifier;

  private final LogManager logger;

  private final ProgramCounterVariables pcVariables;

  public Sequentialization(
      ImmutableList<MPORSubstitution> pSubstitutions,
      MPOROptions pOptions,
      String pInputFileName,
      String pOutputFileName,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    options = pOptions;
    substitutions = pSubstitutions;
    inputFileName = pInputFileName;
    outputFileName = pOutputFileName;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    ImmutableList<CLeftHandSide> pcLeftHandSides =
        SeqLeftHandSideBuilder.buildPcLeftHandSides(options, pSubstitutions.size());
    ImmutableList<CBinaryExpression> threadNotActiveExpressions =
        SeqExpressionBuilder.buildThreadNotActiveExpressions(
            pcLeftHandSides, binaryExpressionBuilder);
    pcVariables = new ProgramCounterVariables(pcLeftHandSides, threadNotActiveExpressions);
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
    ImmutableList<MPORThread> threads = SubstituteUtil.extractThreads(substitutions);
    MPORSubstitution mainSubstitution = SubstituteUtil.extractMainThreadSubstitution(substitutions);
    ImmutableMap<ThreadEdge, SubstituteEdge> substituteEdges =
        SubstituteEdgeBuilder.substituteEdges(options, substitutions);
    GhostElements ghostElements =
        GhostElementUtil.buildGhostElements(
            options, threads, substitutions, substituteEdges, pcVariables, binaryExpressionBuilder);
    ImmutableSet<MemoryLocation> initialMemoryLocations =
        SubstituteUtil.getInitialMemoryLocations(substituteEdges.values());
    Optional<MemoryModel> memoryModel =
        MemoryModelBuilder.tryBuildMemoryModel(
            options, threads, initialMemoryLocations, substituteEdges.values());
    Optional<BitVectorVariables> bitVectorVariables =
        GhostElementUtil.buildBitVectorVariables(options, threads, memoryModel);

    // add bit vector type (before, otherwise parse error) and all input program type declarations
    rProgram.addAll(LineOfCodeUtil.buildOriginalDeclarations(options, threads));
    rProgram.addAll(LineOfCodeUtil.buildBitVectorTypeDeclarations());
    // add input function declarations without definition when their function pointers are used
    rProgram.addAll(LineOfCodeUtil.buildEmptyInputFunctionDeclarations(substituteEdges.values()));
    // add struct and variable declarations
    rProgram.addAll(LineOfCodeUtil.buildInputGlobalVariableDeclarations(options, mainSubstitution));
    rProgram.addAll(LineOfCodeUtil.buildInputLocalVariableDeclarations(options, substitutions));
    rProgram.addAll(LineOfCodeUtil.buildInputParameterDeclarations(options, substitutions));
    rProgram.addAll(LineOfCodeUtil.buildMainFunctionArgDeclarations(options, mainSubstitution));
    rProgram.addAll(LineOfCodeUtil.buildStartRoutineArgDeclarations(options, mainSubstitution));
    rProgram.addAll(LineOfCodeUtil.buildStartRoutineExitDeclarations(options, threads));

    // add custom function declarations and definitions
    rProgram.addAll(LineOfCodeUtil.buildFunctionDeclarations(options));
    rProgram.addAll(
        LineOfCodeUtil.buildFunctionDefinitions(
            options,
            substitutions,
            memoryModel,
            substituteEdges,
            bitVectorVariables,
            ghostElements,
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
      return pLineOfCode.cloneWithCode(replacement);

    } else if (code.contains(outputReachErrorDummy)) {
      // reach_error calls injected by the sequentialization
      CFunctionCallExpression reachErrorCall =
          SeqExpressionBuilder.buildReachError(
              outputFileName + FileExtension.I.suffix,
              pLineNumber,
              SeqToken.__SEQUENTIALIZATION_ERROR__);
      String replacement =
          code.replace(outputReachErrorDummy, reachErrorCall.toASTString() + SeqSyntax.SEMICOLON);
      return pLineOfCode.cloneWithCode(replacement);
    }
    return pLineOfCode;
  }
}
