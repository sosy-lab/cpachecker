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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Year;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqWriter.FileExtension;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqFunctionCallExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqLeftHandSides;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.GhostVariableUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.GhostPcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.GhostThreadVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.function.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.function.SeqMainFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.function.SeqReachErrorFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class Sequentialization {

  private static final ImmutableList<LineOfCode> licenseHeader =
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
          LineOfCode.of(0, "// SPDX-License-Identifier: " + "Apache-2.0"),
          LineOfCode.empty());

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
          LineOfCode.of(0, "// All other assertion fails are induced by faulty input programs"),
          LineOfCode.empty());

  public static final String inputReachErrorDummy =
      SeqFunctionCallExpressionBuilder.buildReachError(
                  SeqToken.__FILE_NAME_PLACEHOLDER__, -1, SeqToken.__PRETTY_FUNCTION__)
              .toASTString()
          + SeqSyntax.SEMICOLON;

  public static final String outputReachErrorDummy =
      SeqFunctionCallExpressionBuilder.buildReachError(
                  SeqToken.__FILE_NAME_PLACEHOLDER__, -1, SeqToken.__SEQUENTIALIZATION_ERROR__)
              .toASTString()
          + SeqSyntax.SEMICOLON;

  public static final int INIT_PC = 0;

  public static final int EXIT_PC = -1;

  private final ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> substitutions;

  private final MPOROptions options;

  private final String inputFileName;

  private final String outputFileName;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final LogManager logger;

  private final GhostPcVariables pcVariables;

  public Sequentialization(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions,
      MPOROptions pOptions,
      String pInputFileName,
      String pOutputFileName,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger) {

    substitutions = pSubstitutions;
    inputFileName = pInputFileName;
    outputFileName = pOutputFileName;
    options = pOptions;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    logger = pLogger;
    pcVariables =
        new GhostPcVariables(
            SeqLeftHandSides.buildPcLeftHandSides(pSubstitutions.size(), options.scalarPc));
  }

  @Override
  public String toString() {
    try {
      ImmutableList<LineOfCode> initProgram = initProgram();
      ImmutableList<LineOfCode> finalProgram = finalProgram(initProgram);
      return LineOfCodeUtil.buildString(finalProgram);
    } catch (UnrecognizedCodeException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO add separate functions for better overview
  /** Generates and returns the sequentialized program that contains dummy reach_error calls. */
  private ImmutableList<LineOfCode> initProgram() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> rProgram = ImmutableList.builder();
    ImmutableSet<MPORThread> threads = substitutions.keySet();

    // add all original program declarations that are not substituted
    rProgram.add(LineOfCode.of(0, SeqComment.UNCHANGED_DECLARATIONS));
    for (MPORThread thread : threads) {
      ImmutableList<CDeclaration> nonVariableDeclarations =
          ThreadUtil.extractNonVariableDeclarations(thread);
      rProgram.addAll(LineOfCodeUtil.buildLinesOfCode(nonVariableDeclarations));
    }
    rProgram.add(LineOfCode.empty());

    // add all var substitute declarations in the order global - local - params - return_pc
    // global var substitutes
    rProgram.add(LineOfCode.of(0, SeqComment.GLOBAL_VAR_DECLARATIONS));
    MPORThread mainThread = ThreadUtil.extractMainThread(threads);
    ImmutableList<CVariableDeclaration> globalDeclarations =
        Objects.requireNonNull(substitutions.get(mainThread)).getGlobalDeclarations();
    rProgram.addAll(LineOfCodeUtil.buildLinesOfCode(globalDeclarations));
    rProgram.add(LineOfCode.empty());
    // local var substitutes
    rProgram.add(LineOfCode.of(0, SeqComment.LOCAL_VAR_DECLARATIONS));
    for (CSimpleDeclarationSubstitution substitution : substitutions.values()) {
      ImmutableList<CVariableDeclaration> localDeclarations = substitution.getLocalDeclarations();
      rProgram.addAll(LineOfCodeUtil.buildLinesOfCode(localDeclarations));
    }
    rProgram.add(LineOfCode.empty());
    // parameter variables storing function arguments
    rProgram.add(LineOfCode.of(0, SeqComment.PARAMETER_VAR_SUBSTITUTES));
    for (CSimpleDeclarationSubstitution substitution : substitutions.values()) {
      ImmutableList<CVariableDeclaration> parameterDeclarations =
          substitution.getParameterDeclarations();
      rProgram.addAll(LineOfCodeUtil.buildLinesOfCode(parameterDeclarations));
    }
    rProgram.add(LineOfCode.empty());
    // ghost return pcs storing calling contexts
    rProgram.add(LineOfCode.of(0, SeqComment.RETURN_PCS));
    ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>> returnPcVariables =
        GhostVariableUtil.buildReturnPcVariables(threads);
    for (ImmutableMap<CFunctionDeclaration, CIdExpression> map : returnPcVariables.values()) {
      for (CIdExpression returnPc : map.values()) {
        rProgram.add(LineOfCode.of(0, returnPc.getDeclaration().toASTString()));
      }
    }
    rProgram.add(LineOfCode.empty());

    // add thread simulation vars
    rProgram.add(LineOfCode.of(0, SeqComment.THREAD_SIMULATION));
    ImmutableMap<ThreadEdge, SubstituteEdge> subEdges =
        SubstituteBuilder.substituteEdges(substitutions);
    GhostThreadVariables threadVars = GhostVariableUtil.buildThreadVariables(threads, subEdges);
    for (CIdExpression threadVar : threadVars.getIdExpressions()) {
      assert threadVar.getDeclaration() instanceof CVariableDeclaration;
      CVariableDeclaration varDeclaration = (CVariableDeclaration) threadVar.getDeclaration();
      rProgram.add(LineOfCode.of(0, varDeclaration.toASTString()));
    }
    rProgram.add(LineOfCode.empty());

    // add all custom function declarations
    rProgram.add(LineOfCode.of(0, SeqComment.CUSTOM_FUNCTION_DECLARATIONS));
    // reach_error, abort, assert, nondet_int may be duplicate depending on the input program
    rProgram.add(LineOfCode.of(0, SeqFunctionDeclaration.ASSERT_FAIL.toASTString()));
    rProgram.add(LineOfCode.of(0, SeqFunctionDeclaration.VERIFIER_NONDET_INT.toASTString()));
    rProgram.add(LineOfCode.of(0, SeqFunctionDeclaration.ABORT.toASTString()));
    rProgram.add(LineOfCode.of(0, SeqFunctionDeclaration.REACH_ERROR.toASTString()));
    rProgram.add(LineOfCode.of(0, SeqFunctionDeclaration.ASSUME.toASTString()));
    // main should always be duplicate
    rProgram.add(LineOfCode.of(0, SeqFunctionDeclaration.MAIN.toASTString()));
    rProgram.add(LineOfCode.empty());

    // add non main() function definitions
    SeqReachErrorFunction reachError = new SeqReachErrorFunction();
    SeqAssumeFunction assume = new SeqAssumeFunction(binaryExpressionBuilder);
    rProgram.addAll(reachError.buildDefinition());
    rProgram.add(LineOfCode.empty());
    rProgram.addAll(assume.buildDefinition());
    rProgram.add(LineOfCode.empty());

    // TODO the case clauses should be multimaps
    // create case clauses in main method
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> caseClauses =
        SeqCaseClauseBuilder.buildCaseClauses(
            substitutions,
            subEdges,
            returnPcVariables,
            pcVariables,
            threadVars,
            binaryExpressionBuilder,
            logger);
    // optional: include POR assumptions
    Optional<ImmutableList<SeqFunctionCallExpression>> porAssumptions =
        options.partialOrderReduction
            ? Optional.of(
                SeqAssumptionBuilder.createPORAssumptions(
                    caseClauses, pcVariables, binaryExpressionBuilder))
            : Optional.empty();
    ImmutableList<SeqFunctionCallExpression> threadSimulationAssumptions =
        SeqAssumptionBuilder.createThreadSimulationAssumptions(
            pcVariables, threadVars, binaryExpressionBuilder);
    SeqMainFunction mainMethod =
        new SeqMainFunction(
            substitutions.size(),
            options,
            threadSimulationAssumptions,
            porAssumptions,
            caseClauses,
            pcVariables,
            binaryExpressionBuilder);
    rProgram.addAll(mainMethod.buildDefinition());

    return rProgram.build();
  }

  /**
   * Adds the license and sequentialization comments at the top of pInitProgram and replaces the
   * file name and line in {@code reach_error("__FILE_NAME_PLACEHOLDER__", -1,
   * "__SEQUENTIALIZATION_ERROR__");} with pOutputFileName and the actual line.
   */
  private ImmutableList<LineOfCode> finalProgram(ImmutableList<LineOfCode> pInitProgram) {
    // consider license and seq comment header for line numbers
    int currentLine = licenseHeader.size() + mporHeader.size() + 1;
    ImmutableList.Builder<LineOfCode> rProgram = ImmutableList.builder();
    rProgram.addAll(licenseHeader);
    rProgram.addAll(mporHeader);
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
          SeqFunctionCallExpressionBuilder.buildReachError(
              inputFileName, pLineNumber, SeqToken.__PRETTY_FUNCTION__);
      String replacement =
          code.replace(inputReachErrorDummy, reachErrorCall.toASTString() + SeqSyntax.SEMICOLON);
      return pLineOfCode.copyWithCode(replacement);

    } else if (code.contains(outputReachErrorDummy)) {
      // reach_error calls injected by the sequentialization
      CFunctionCallExpression reachErrorCall =
          SeqFunctionCallExpressionBuilder.buildReachError(
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
