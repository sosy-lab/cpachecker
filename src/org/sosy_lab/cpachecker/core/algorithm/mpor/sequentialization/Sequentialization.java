// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.collect.ImmutableList;
import java.util.StringJoiner;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Sequentialization {

  private static final ImmutableList<String> mporHeader =
      ImmutableList.of(
          "// This sequentialization (transformation of a concurrent program into an"
              + " equivalent",
          "// sequential program) was created by the MPORAlgorithm implemented in CPAchecker.");

  private static final String PRETTY_FUNCTION_REACH_ERROR_PARAMETER_NAME = "__PRETTY_FUNCTION__";

  public static final String inputReachErrorDummy =
      SeqExpressionBuilder.buildReachError(
                  "__FILE_NAME_PLACEHOLDER__", -1, PRETTY_FUNCTION_REACH_ERROR_PARAMETER_NAME)
              .toASTString()
          + SeqSyntax.SEMICOLON;

  public static final int INIT_PC = 1;

  public static final int EXIT_PC = 0;

  public static final int MAIN_THREAD_ID = 0;

  public static final int FIRST_LINE = 1;

  public static String tryBuildProgramString(
      MPOROptions pOptions, CFA pCfa, String pInputFileName, SequentializationUtils pUtils)
      throws UnrecognizedCodeException, InterruptedException {

    SequentializationFields fields = new SequentializationFields(pOptions, pCfa, pUtils);
    return buildProgramString(pOptions, pInputFileName, fields, pUtils);
  }

  private static String buildProgramString(
      MPOROptions pOptions,
      String pInputFileName,
      SequentializationFields pFields,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException, InterruptedException {

    String initProgram = initProgram(pOptions, pFields, pUtils);
    String formattedProgram = handleProgramFormatting(pOptions, initProgram, pUtils);
    // replace dummy reach_errors after formatting so that line numbers are exact
    String rFinalProgram = replaceDummyReachErrors(pInputFileName, formattedProgram);

    if (pOptions.validateParse()) {
      try {
        return SeqValidator.validateProgramParsing(rFinalProgram, pUtils);
      } catch (ParserException | InterruptedException | InvalidConfigurationException e) {
        pUtils
            .logger()
            .logUserException(
                Level.WARNING, e, "An exception occurred while parsing the sequentialization.");
      }
    }
    return rFinalProgram;
  }

  /** Generates and returns the sequentialized program that contains dummy reach_error calls. */
  private static String initProgram(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    StringJoiner rProgram = new StringJoiner(SeqSyntax.NEWLINE);

    if (pOptions.comments()) {
      mporHeader.forEach(line -> rProgram.add(line));
    }

    // add bit vector type (before, otherwise parse error) and all input program type declarations
    rProgram.add(
        SequentializationBuilder.buildInputFunctionAndTypeDeclarations(pOptions, pFields.threads));
    rProgram.add(SequentializationBuilder.buildBitVectorTypeDeclarations());
    // add struct and variable declarations
    rProgram.add(
        SequentializationBuilder.buildInputGlobalVariableDeclarations(
            pOptions, pFields.mainSubstitution));
    rProgram.add(
        SequentializationBuilder.buildInputLocalVariableDeclarations(
            pOptions, pFields.substitutions));
    rProgram.add(
        SequentializationBuilder.buildInputParameterDeclarations(pOptions, pFields.substitutions));
    rProgram.add(
        SequentializationBuilder.buildMainFunctionArgDeclarations(
            pOptions, pFields.mainSubstitution));
    rProgram.add(
        SequentializationBuilder.buildStartRoutineArgDeclarations(
            pOptions, pFields.mainSubstitution));
    rProgram.add(
        SequentializationBuilder.buildStartRoutineExitDeclarations(pOptions, pFields.threads));

    // add thread simulation variables (i.e. ghost elements)
    rProgram.add(
        SequentializationBuilder.buildThreadSimulationVariableDeclarations(pOptions, pFields));

    // add custom function declarations and definitions
    rProgram.add(SequentializationBuilder.buildFunctionDeclarations(pOptions, pFields));
    rProgram.add(SequentializationBuilder.buildFunctionDefinitions(pOptions, pFields, pUtils));

    return rProgram.toString();
  }

  private static String handleProgramFormatting(
      MPOROptions pOptions, String pProgram, SequentializationUtils pUtils)
      throws InterruptedException {

    if (pOptions.clangFormatStyle().isEnabled()) {
      return pUtils.clangFormatter().tryFormat(pProgram, pOptions.clangFormatStyle());
    }
    return pProgram;
  }

  /** Replaces the file name and line in {@code reach_error();} dummies with the actual values. */
  private static String replaceDummyReachErrors(String pInputFileName, String pInitProgram) {
    StringJoiner rProgram = new StringJoiner(SeqSyntax.NEWLINE);
    int currentLine = FIRST_LINE;
    for (String lineOfCode : SeqStringUtil.splitOnNewline(pInitProgram)) {
      // replace dummy line numbers (-1) with actual line numbers in the seq
      rProgram.add(replaceReachErrorDummies(pInputFileName, lineOfCode, currentLine));
      currentLine++;
    }
    return rProgram.toString();
  }

  /**
   * Replaces dummy calls to {@code reach_error}, or returns {@code pLineOfCode} as is if there is
   * none.
   */
  private static String replaceReachErrorDummies(
      String pInputFileName, String pLineOfCode, int pLineNumber) {

    if (pLineOfCode.contains(inputReachErrorDummy)) {
      // reach_error calls from the input program
      CFunctionCallExpression reachErrorCall =
          SeqExpressionBuilder.buildReachError(
              pInputFileName, pLineNumber, PRETTY_FUNCTION_REACH_ERROR_PARAMETER_NAME);
      return pLineOfCode.replace(
          inputReachErrorDummy, reachErrorCall.toASTString() + SeqSyntax.SEMICOLON);
    }
    return pLineOfCode;
  }
}
