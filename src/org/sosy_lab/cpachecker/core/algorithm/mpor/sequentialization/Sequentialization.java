// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.collect.ImmutableList;
import java.time.Year;
import java.time.ZoneId;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqWriter.FileExtension;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
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

  private final CFA cfa;

  private final String inputFileName;

  private final String outputFileName;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final ShutdownNotifier shutdownNotifier;

  private final LogManager logger;

  public Sequentialization(
      MPOROptions pOptions,
      CFA pCfa,
      String pInputFileName,
      String pOutputFileName,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger) {

    options = pOptions;
    cfa = pCfa;
    inputFileName = pInputFileName;
    outputFileName = pOutputFileName;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
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

  private static SequentializationFields buildFields(
      MPOROptions pOptions,
      CFA pCfa,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    return new SequentializationFields(pOptions, pCfa, pBinaryExpressionBuilder, pLogger);
  }

  /** Generates and returns the sequentialized program that contains dummy reach_error calls. */
  private ImmutableList<LineOfCode> initProgram() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> rProgram = ImmutableList.builder();

    SequentializationFields fields = buildFields(options, cfa, binaryExpressionBuilder, logger);

    // add bit vector type (before, otherwise parse error) and all input program type declarations
    rProgram.addAll(LineOfCodeUtil.buildOriginalDeclarations(options, fields.threads));
    rProgram.addAll(LineOfCodeUtil.buildBitVectorTypeDeclarations());
    // add input function declarations without definition when their function pointers are used
    rProgram.addAll(
        LineOfCodeUtil.buildEmptyInputFunctionDeclarations(fields.substituteEdges.values()));
    // add struct and variable declarations
    rProgram.addAll(
        LineOfCodeUtil.buildInputGlobalVariableDeclarations(options, fields.mainSubstitution));
    rProgram.addAll(
        LineOfCodeUtil.buildInputLocalVariableDeclarations(options, fields.substitutions));
    rProgram.addAll(LineOfCodeUtil.buildInputParameterDeclarations(options, fields.substitutions));
    rProgram.addAll(
        LineOfCodeUtil.buildMainFunctionArgDeclarations(options, fields.mainSubstitution));
    rProgram.addAll(
        LineOfCodeUtil.buildStartRoutineArgDeclarations(options, fields.mainSubstitution));
    rProgram.addAll(LineOfCodeUtil.buildStartRoutineExitDeclarations(options, fields.threads));

    // add custom function declarations and definitions
    rProgram.addAll(LineOfCodeUtil.buildFunctionDeclarations(options));
    rProgram.addAll(
        LineOfCodeUtil.buildFunctionDefinitions(options, fields, binaryExpressionBuilder, logger));

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
