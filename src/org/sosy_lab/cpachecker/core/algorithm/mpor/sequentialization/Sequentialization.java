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
import org.sosy_lab.cpachecker.core.algorithm.mpor.output.MPORWriter.FileExtension;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Sequentialization {

  // TODO move all this to hardcoded strings?

  private static final String license = "Apache-2.0";

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

  public static String tryBuildProgramString(
      MPOROptions pOptions,
      CFA pCfa,
      String pInputFileName,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger) {

    try {
      CBinaryExpressionBuilder binaryExpressionBuilder =
          new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger);
      SequentializationFields fields =
          new SequentializationFields(pOptions, pCfa, binaryExpressionBuilder, pLogger);
      return buildProgramString(
          pOptions, pInputFileName, fields, binaryExpressionBuilder, pShutdownNotifier, pLogger);
    } catch (UnrecognizedCodeException e) {
      // we convert to RuntimeExceptions for unit tests
      throw new RuntimeException(e);
    }
  }

  private static String buildProgramString(
      MPOROptions pOptions,
      String pInputFileName,
      SequentializationFields pFields,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger) {

    try {
      ImmutableList<String> initProgram =
          initProgram(pOptions, pFields, pBinaryExpressionBuilder, pLogger);
      ImmutableList<String> finalProgram = finalProgram(pOptions, pInputFileName, initProgram);
      String program = SeqStringUtil.joinWithNewlines(finalProgram);
      return pOptions.validateParse && pOptions.inputTypeDeclarations
          ? SeqValidator.validateProgramParsing(program, pOptions, pShutdownNotifier, pLogger)
          : program;

    } catch (UnrecognizedCodeException
        | InvalidConfigurationException
        | ParserException
        | InterruptedException e) {
      // we convert to RuntimeExceptions for unit tests
      pLogger.log(Level.SEVERE, e);
      throw new RuntimeException(e);
    }
  }

  /** Generates and returns the sequentialized program that contains dummy reach_error calls. */
  private static ImmutableList<String> initProgram(
      MPOROptions pOptions,
      SequentializationFields pFields,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> rProgram = ImmutableList.builder();

    // add bit vector type (before, otherwise parse error) and all input program type declarations
    rProgram.addAll(SequentializationBuilder.buildOriginalDeclarations(pOptions, pFields.threads));
    rProgram.addAll(SequentializationBuilder.buildBitVectorTypeDeclarations());
    // add struct and variable declarations
    rProgram.addAll(
        SequentializationBuilder.buildInputGlobalVariableDeclarations(
            pOptions, pFields.mainSubstitution));
    rProgram.addAll(
        SequentializationBuilder.buildInputLocalVariableDeclarations(
            pOptions, pFields.substitutions));
    rProgram.addAll(
        SequentializationBuilder.buildInputParameterDeclarations(pOptions, pFields.substitutions));
    rProgram.addAll(
        SequentializationBuilder.buildMainFunctionArgDeclarations(
            pOptions, pFields.mainSubstitution));
    rProgram.addAll(
        SequentializationBuilder.buildStartRoutineArgDeclarations(
            pOptions, pFields.mainSubstitution));
    rProgram.addAll(
        SequentializationBuilder.buildStartRoutineExitDeclarations(pOptions, pFields.threads));

    // add thread simulation variables (i.e. ghost elements)
    rProgram.addAll(
        SequentializationBuilder.buildThreadSimulationVariableDeclarations(pOptions, pFields));

    // add custom function declarations and definitions
    rProgram.addAll(SequentializationBuilder.buildFunctionDeclarations(pOptions, pFields));
    rProgram.addAll(
        SequentializationBuilder.buildFunctionDefinitions(
            pOptions, pFields, pBinaryExpressionBuilder, pLogger));

    return rProgram.build();
  }

  /**
   * Adds the license and sequentialization comments at the top of pInitProgram and replaces the
   * file name and line in {@code reach_error("__FILE_NAME_PLACEHOLDER__", -1,
   * "__SEQUENTIALIZATION_ERROR__");} with pOutputFileName and the actual line.
   */
  private static ImmutableList<String> finalProgram(
      MPOROptions pOptions, String pInputFileName, ImmutableList<String> pInitProgram) {

    // consider license and seq comment header for line numbers
    ImmutableList<String> licenseHeader =
        buildLicenseHeader(Year.now(ZoneId.systemDefault()).getValue());
    int currentLine =
        pOptions.comments ? licenseHeader.size() + mporHeader.size() + FIRST_LINE : FIRST_LINE;
    ImmutableList.Builder<String> rProgram = ImmutableList.builder();
    if (pOptions.license) {
      rProgram.addAll(licenseHeader);
    }
    if (pOptions.comments) {
      rProgram.addAll(mporHeader);
    }
    String outputFileName = SeqNameUtil.buildOutputFileName(pInputFileName);
    for (String lineOfCode : pInitProgram) {
      // replace dummy line numbers (-1) with actual line numbers in the seq
      rProgram.add(
          replaceReachErrorDummies(pInputFileName, outputFileName, lineOfCode, currentLine));
      currentLine++;
    }
    return rProgram.build();
  }

  private static ImmutableList<String> buildLicenseHeader(int pYear) {
    return ImmutableList.of(
        "// This file is part of CPAchecker,",
        "// a tool for configurable software verification:",
        "// https://cpachecker.sosy-lab.org",
        "//",
        "// SPDX-" + "FileCopyrightText: " + pYear + " Dirk Beyer <https://www.sosy-lab.org>",
        "//",
        // splitting this with + so that 'reuse lint' accepts it
        "// SPDX-" + "License-" + "Identifier: " + license);
  }

  /**
   * Replaces dummy calls to {@code reach_error}, or returns {@code pLineOfCode} as is if there is
   * none.
   */
  private static String replaceReachErrorDummies(
      String pInputFileName, String pOutputFileName, String pLineOfCode, int pLineNumber) {

    if (pLineOfCode.contains(inputReachErrorDummy)) {
      // reach_error calls from the input program
      CFunctionCallExpression reachErrorCall =
          SeqExpressionBuilder.buildReachError(
              pInputFileName, pLineNumber, SeqToken.__PRETTY_FUNCTION__);
      return pLineOfCode.replace(
          inputReachErrorDummy, reachErrorCall.toASTString() + SeqSyntax.SEMICOLON);

    } else if (pLineOfCode.contains(outputReachErrorDummy)) {
      // reach_error calls injected by the sequentialization
      CFunctionCallExpression reachErrorCall =
          SeqExpressionBuilder.buildReachError(
              pOutputFileName + FileExtension.I.suffix,
              pLineNumber,
              SeqToken.__SEQUENTIALIZATION_ERROR__);
      return pLineOfCode.replace(
          outputReachErrorDummy, reachErrorCall.toASTString() + SeqSyntax.SEMICOLON);
    }
    return pLineOfCode;
  }
}
