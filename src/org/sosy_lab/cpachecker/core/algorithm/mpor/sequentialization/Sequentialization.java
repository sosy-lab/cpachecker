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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Sequentialization {

  // TODO move all this to hardcoded strings?

  private static final String license = "Apache-2.0";

  private final ImmutableList<String> licenseHeader =
      ImmutableList.of(
          "// This file is part of CPAchecker,",
          "// a tool for configurable software verification:",
          "// https://cpachecker.sosy-lab.org",
          "//",
          "// SPDX-"
              + "FileCopyrightText: "
              + Year.now(ZoneId.systemDefault()).getValue()
              + " Dirk Beyer <https://www.sosy-lab.org>",
          "//",
          // splitting this with + so that 'reuse lint' accepts it
          "// SPDX-" + "License-" + "Identifier: " + license);

  private static final ImmutableList<String> mporHeader =
      ImmutableList.of(
          "// This sequentialization (transformation of a concurrent program into an"
              + " equivalent",
          "// sequential program) was created by the MPORAlgorithm implemented in CPAchecker.",
          "//",
          "// Assertion fails from the function "
              + SeqToken.__SEQUENTIALIZATION_ERROR__
              + " mark faulty sequentializations.",
          "// All other assertion fails are induced by faulty input programs.");

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

  public static final int FIRST_LINE = 1;

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
      SequentializationFields fields = buildFields();
      return toString(fields);
    } catch (UnrecognizedCodeException e) {
      // we convert to RuntimeExceptions for unit tests
      throw new RuntimeException(e);
    }
  }

  public String toString(SequentializationFields pFields) {
    try {
      ImmutableList<String> initProgram = initProgram(pFields);
      ImmutableList<String> finalProgram = finalProgram(initProgram);
      String program = SeqStringUtil.joinWithNewlines(finalProgram);
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

  SequentializationFields buildFields() throws UnrecognizedCodeException {
    return new SequentializationFields(options, cfa, binaryExpressionBuilder, logger);
  }

  /** Generates and returns the sequentialized program that contains dummy reach_error calls. */
  private ImmutableList<String> initProgram(SequentializationFields pFields)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> rProgram = ImmutableList.builder();

    // add bit vector type (before, otherwise parse error) and all input program type declarations
    rProgram.addAll(SequentializationBuilder.buildOriginalDeclarations(options, pFields.threads));
    rProgram.addAll(SequentializationBuilder.buildBitVectorTypeDeclarations());
    // add struct and variable declarations
    rProgram.addAll(
        SequentializationBuilder.buildInputGlobalVariableDeclarations(
            options, pFields.mainSubstitution));
    rProgram.addAll(
        SequentializationBuilder.buildInputLocalVariableDeclarations(
            options, pFields.substitutions));
    rProgram.addAll(
        SequentializationBuilder.buildInputParameterDeclarations(options, pFields.substitutions));
    rProgram.addAll(
        SequentializationBuilder.buildMainFunctionArgDeclarations(
            options, pFields.mainSubstitution));
    rProgram.addAll(
        SequentializationBuilder.buildStartRoutineArgDeclarations(
            options, pFields.mainSubstitution));
    rProgram.addAll(
        SequentializationBuilder.buildStartRoutineExitDeclarations(options, pFields.threads));

    // add custom function declarations and definitions
    rProgram.addAll(SequentializationBuilder.buildFunctionDeclarations(options));
    rProgram.addAll(
        SequentializationBuilder.buildFunctionDefinitions(
            options, pFields, binaryExpressionBuilder, logger));

    return rProgram.build();
  }

  /**
   * Adds the license and sequentialization comments at the top of pInitProgram and replaces the
   * file name and line in {@code reach_error("__FILE_NAME_PLACEHOLDER__", -1,
   * "__SEQUENTIALIZATION_ERROR__");} with pOutputFileName and the actual line.
   */
  private ImmutableList<String> finalProgram(ImmutableList<String> pInitProgram) {
    // consider license and seq comment header for line numbers
    int currentLine =
        options.comments ? licenseHeader.size() + mporHeader.size() + FIRST_LINE : FIRST_LINE;
    ImmutableList.Builder<String> rProgram = ImmutableList.builder();
    if (options.license) {
      rProgram.addAll(licenseHeader);
    }
    if (options.comments) {
      rProgram.addAll(mporHeader);
    }
    for (String lineOfCode : pInitProgram) {
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
  private String replaceReachErrorDummies(String pLineOfCode, int pLineNumber) {
    if (pLineOfCode.contains(inputReachErrorDummy)) {
      // reach_error calls from the input program
      CFunctionCallExpression reachErrorCall =
          SeqExpressionBuilder.buildReachError(
              inputFileName, pLineNumber, SeqToken.__PRETTY_FUNCTION__);
      return pLineOfCode.replace(
          inputReachErrorDummy, reachErrorCall.toASTString() + SeqSyntax.SEMICOLON);

    } else if (pLineOfCode.contains(outputReachErrorDummy)) {
      // reach_error calls injected by the sequentialization
      CFunctionCallExpression reachErrorCall =
          SeqExpressionBuilder.buildReachError(
              outputFileName + FileExtension.I.suffix,
              pLineNumber,
              SeqToken.__SEQUENTIALIZATION_ERROR__);
      return pLineOfCode.replace(
          outputReachErrorDummy, reachErrorCall.toASTString() + SeqSyntax.SEMICOLON);
    }
    return pLineOfCode;
  }
}
