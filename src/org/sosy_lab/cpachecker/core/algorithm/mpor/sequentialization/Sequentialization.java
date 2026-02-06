// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class Sequentialization {

  private static final ImmutableList<String> MPOR_HEADER =
      ImmutableList.of(
          "// This sequentialization (transformation of a concurrent program into an"
              + " equivalent",
          "// sequential program) was created by the MPORAlgorithm implemented in CPAchecker.");

  @CanIgnoreReturnValue
  public static String tryBuildProgramString(
      MPOROptions pOptions, CFA pCfa, SequentializationUtils pUtils)
      throws UnrecognizedCodeException, InterruptedException {

    InputRejection.handleRejections(pCfa);
    SequentializationFields fields = new SequentializationFields(pOptions, pCfa, pUtils);
    return buildProgramString(pOptions, fields, pUtils);
  }

  private static String buildProgramString(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException, InterruptedException {

    String initProgram = initProgram(pOptions, pFields, pUtils);

    // if enabled, format program
    String rFormattedProgram =
        pOptions.clangFormatStyle().isEnabled()
            ? pUtils.clangFormatter().tryFormat(initProgram, pOptions.clangFormatStyle())
            : initProgram;

    // if enabled, check that program can be parsed by CPAchecker
    if (pOptions.validateParse()) {
      try {
        SeqValidator.validateProgramParsing(rFormattedProgram, pUtils);
      } catch (ParserException | InterruptedException | InvalidConfigurationException e) {
        pUtils
            .logger()
            .logUserException(
                Level.WARNING, e, "An exception occurred while parsing the sequentialization.");
      }
    }
    return rFormattedProgram;
  }

  /** Initializes and returns the unformatted, sequentialized program. */
  private static String initProgram(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    StringJoiner rProgram = new StringJoiner(SeqSyntax.NEWLINE);

    if (pOptions.comments()) {
      MPOR_HEADER.forEach(line -> rProgram.add(line));
    }

    // add bit vector type (before, otherwise parse error) and all input program type declarations
    rProgram.add(
        SequentializationBuilder.buildInputFunctionAndTypeDeclarations(pOptions, pFields.threads));
    rProgram.add(SequentializationBuilder.buildBitVectorTypeDeclarations());
    // add all variable and parameter declarations, but only if there are any
    Optional.of(
            SequentializationBuilder.buildInputGlobalVariableDeclarations(
                pOptions, pFields.mainSubstitution))
        .filter(s -> !s.isEmpty())
        .ifPresent(rProgram::add);
    Optional.of(
            SequentializationBuilder.buildInputLocalVariableDeclarations(
                pOptions, pFields.substitutions))
        .filter(s -> !s.isEmpty())
        .ifPresent(rProgram::add);
    Optional.of(
            SequentializationBuilder.buildInputParameterDeclarations(
                pOptions, pFields.substitutions))
        .filter(s -> !s.isEmpty())
        .ifPresent(rProgram::add);
    Optional.of(
            SequentializationBuilder.buildMainFunctionArgDeclarations(
                pOptions, pFields.mainSubstitution))
        .filter(s -> !s.isEmpty())
        .ifPresent(rProgram::add);
    Optional.of(
            SequentializationBuilder.buildStartRoutineArgDeclarations(
                pOptions, pFields.mainSubstitution))
        .filter(s -> !s.isEmpty())
        .ifPresent(rProgram::add);
    Optional.of(
            SequentializationBuilder.buildStartRoutineExitDeclarations(pOptions, pFields.threads))
        .filter(s -> !s.isEmpty())
        .ifPresent(rProgram::add);

    // add thread simulation variables (i.e. ghost elements)
    rProgram.add(
        SequentializationBuilder.buildThreadSimulationVariableDeclarations(pOptions, pFields));

    // add custom function declarations and definitions
    rProgram.add(SequentializationBuilder.buildFunctionDeclarations(pOptions, pFields));
    rProgram.add(SequentializationBuilder.buildFunctionDefinitions(pOptions, pFields, pUtils));

    return rProgram.toString();
  }
}
