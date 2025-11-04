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
import java.util.StringJoiner;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.ClangFormatter;

public class Sequentialization {

  private static final String license = "Apache-2.0";

  private static final ImmutableList<String> mporHeader =
      ImmutableList.of(
          "// This sequentialization (transformation of a concurrent program into an"
              + " equivalent",
          "// sequential program) was created by the MPORAlgorithm implemented in CPAchecker.");

  public static final String inputReachErrorDummy =
      SeqExpressionBuilder.buildReachError(
                  SeqToken.FILE_NAME_PLACEHOLDER, -1, SeqToken.PRETTY_FUNCTION_KEYWORD)
              .toASTString()
          + SeqSyntax.SEMICOLON;

  public static final int INIT_PC = 1;

  public static final int EXIT_PC = 0;

  public static final int MAIN_THREAD_ID = 0;

  public static final int FIRST_LINE = 1;

  public static String tryBuildProgramString(
      MPOROptions pOptions,
      CFA pCfa,
      String pInputFileName,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {

    try {
      SequentializationUtils utils =
          SequentializationUtils.of(pCfa.getMachineModel(), pLogger, pShutdownNotifier);
      SequentializationFields fields = new SequentializationFields(pOptions, pCfa, utils);
      return buildProgramString(pOptions, pInputFileName, fields, utils);
    } catch (UnrecognizedCodeException e) {
      // we convert to RuntimeExceptions for unit tests
      throw new RuntimeException(e);
    }
  }

  private static String buildProgramString(
      MPOROptions pOptions,
      String pInputFileName,
      SequentializationFields pFields,
      SequentializationUtils pUtils) {

    try {
      String initProgram = initProgram(pOptions, pFields, pUtils);
      String formattedProgram =
          pOptions.clangFormatStyle().isEnabled()
              ? ClangFormatter.tryFormat(initProgram, pOptions.clangFormatStyle(), pUtils.logger())
              : initProgram;
      // replace dummy reach_errors after formatting so that line numbers are exact
      String finalProgram = replaceDummyReachErrors(pInputFileName, formattedProgram);
      return pOptions.validateParse() && pOptions.inputTypeDeclarations()
          ? SeqValidator.validateProgramParsing(finalProgram, pOptions, pUtils)
          : finalProgram;

    } catch (UnrecognizedCodeException
        | InvalidConfigurationException
        | ParserException
        | InterruptedException e) {
      // we convert to RuntimeExceptions for unit tests
      pUtils.logger().log(Level.SEVERE, e);
      throw new RuntimeException(e);
    }
  }

  /** Generates and returns the sequentialized program that contains dummy reach_error calls. */
  private static String initProgram(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    StringJoiner rProgram = new StringJoiner(SeqSyntax.NEWLINE);

    // if enabled, add a license header
    ImmutableList<String> licenseHeader =
        buildLicenseHeader(Year.now(ZoneId.systemDefault()).getValue());
    if (pOptions.license()) {
      licenseHeader.forEach(line -> rProgram.add(line));
    }
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

  /**
   * Adds the license and sequentialization comments at the top of pInitProgram and replaces the
   * file name and line in {@code reach_error();} dummies with the actual values.
   */
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
      String pInputFileName, String pLineOfCode, int pLineNumber) {

    if (pLineOfCode.contains(inputReachErrorDummy)) {
      // reach_error calls from the input program
      CFunctionCallExpression reachErrorCall =
          SeqExpressionBuilder.buildReachError(
              pInputFileName, pLineNumber, SeqToken.PRETTY_FUNCTION_KEYWORD);
      return pLineOfCode.replace(
          inputReachErrorDummy, reachErrorCall.toASTString() + SeqSyntax.SEMICOLON);
    }
    return pLineOfCode;
  }
}
