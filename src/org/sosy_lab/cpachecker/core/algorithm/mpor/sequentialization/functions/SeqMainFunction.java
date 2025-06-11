// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqInitializerBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.declaration.SeqBitVectorDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.declaration.SeqBitVectorDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single.SeqSingleControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single.SeqSingleControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqMainFunction extends SeqFunction {

  private static final SeqSingleControlFlowStatement whileTrue =
      new SeqSingleControlFlowStatement(
          SeqIntegerLiteralExpression.INT_1, SeqControlFlowStatementType.WHILE);

  private final MPOROptions options;

  private final MPORSubstitution mainSubstitution;

  private final CIdExpression numThreadsVariable;

  /** The thread-specific clauses in the while loop. */
  private final ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> clauses;

  private final ImmutableList<SeqBitVectorDeclaration> bitVectorDeclarations;

  private final ImmutableList<CVariableDeclaration> pcDeclarations;

  private final CFunctionCallAssignmentStatement nextThreadAssignment;

  private final ImmutableList<CFunctionCallStatement> nextThreadAssumptions;

  private final Optional<CFunctionCallStatement> nextThreadActiveAssumption;

  private final PcVariables pcVariables;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  private final LogManager logger;

  public SeqMainFunction(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      Optional<BitVectorVariables> pBitVectorVariables,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    int numThreads = pSubstitutions.size();

    options = pOptions;
    mainSubstitution = SubstituteUtil.extractMainThreadSubstitution(pSubstitutions);
    numThreadsVariable =
        SeqExpressionBuilder.buildIdExpression(
            SeqDeclarationBuilder.buildVariableDeclaration(
                false,
                SeqSimpleType.CONST_INT,
                SeqToken.NUM_THREADS,
                SeqInitializerBuilder.buildInitializerExpression(
                    SeqExpressionBuilder.buildIntegerLiteralExpression(numThreads))));
    clauses = pClauses;
    pcVariables = pPcVariables;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
    logger = pLogger;

    bitVectorDeclarations =
        SeqBitVectorDeclarationBuilder.buildBitVectorDeclarationsByEncoding(
            options, pBitVectorVariables, pClauses.keySet());
    pcDeclarations =
        SeqDeclarationBuilder.buildPcDeclarations(pcVariables, numThreads, pOptions.scalarPc);

    nextThreadAssignment = SeqStatementBuilder.buildNextThreadAssignment(pOptions.signedNondet);
    nextThreadAssumptions =
        SeqAssumptionBuilder.buildNextThreadAssumption(
            pOptions.signedNondet, numThreadsVariable, binaryExpressionBuilder);
    nextThreadActiveAssumption =
        SeqAssumptionBuilder.buildNextThreadActiveAssumption(options, binaryExpressionBuilder);
  }

  @Override
  public ImmutableList<LineOfCode> buildBody() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> rBody = ImmutableList.builder();
    // declare main() local variables NUM_THREADS, pc, next_thread
    // TODO its probably best to remove num threads entirely and just place the int
    rBody.addAll(
        buildThreadSimulationVariableDeclarations(
            options, numThreadsVariable.getDeclaration(), bitVectorDeclarations, pcDeclarations));
    // add main function argument non-deterministic assignments
    rBody.addAll(buildMainFunctionArgNondetAssignments(mainSubstitution, logger));
    // --- loop starts here ---
    rBody.add(LineOfCode.of(1, SeqStringUtil.appendOpeningCurly(whileTrue.toASTString())));
    if (options.comments) {
      rBody.add(LineOfCode.empty());
      rBody.add(LineOfCode.of(2, SeqComment.NEXT_THREAD_NONDET));
    }
    rBody.add(LineOfCode.of(2, nextThreadAssignment.toASTString()));
    if (options.comments) {
      rBody.add(LineOfCode.empty());
    }
    for (CFunctionCallStatement nextThreadAssumption : nextThreadAssumptions) {
      rBody.add(LineOfCode.of(2, nextThreadAssumption.toASTString()));
    }
    // assumptions over next_thread being active (pc != -1)
    if (options.comments) {
      rBody.add(LineOfCode.empty());
      rBody.add(LineOfCode.of(2, SeqComment.NEXT_THREAD_ACTIVE));
    }
    if (nextThreadActiveAssumption.isPresent()) {
      CFunctionCallStatement assumption = nextThreadActiveAssumption.orElseThrow();
      rBody.addAll(LineOfCodeUtil.buildLinesOfCode(2, assumption.toASTString()));
    }
    // add all assumptions over thread variables
    if (options.comments) {
      rBody.add(LineOfCode.empty());
      rBody.add(LineOfCode.of(2, SeqComment.THREAD_SIMULATION_ASSUMPTIONS));
    }
    // add all thread simulation control flow statements
    if (options.comments) {
      rBody.add(LineOfCode.empty());
      rBody.add(LineOfCode.of(2, SeqComment.THREAD_SIMULATION_CONTROL_FLOW));
    }
    if (options.nondeterminismSource.hasThreadLoops()) {
      rBody.addAll(
          SeqThreadLoopBuilder.buildThreadLoopsSwitchStatements(
              options, pcVariables, clauses, binaryExpressionBuilder));
    } else {
      rBody.addAll(
          SeqSingleLoopBuilder.buildSingleLoopThreadSimulationStatements(
              options, pcVariables, clauses, binaryExpressionBuilder));
    }
    rBody.add(LineOfCode.of(1, SeqSyntax.CURLY_BRACKET_RIGHT));
    // --- loop ends here ---
    if (options.sequentializationErrors) {
      // end of main function, only reachable if thread simulation finished incorrectly -> error
      rBody.add(LineOfCode.of(1, Sequentialization.outputReachErrorDummy));
    }
    return rBody.build();
  }

  @Override
  public CType getReturnType() {
    return SeqSimpleType.INT;
  }

  @Override
  public CIdExpression getFunctionName() {
    return SeqIdExpression.MAIN;
  }

  @Override
  public ImmutableList<CParameterDeclaration> getParameters() {
    return ImmutableList.of();
  }

  /**
   * Adds the non-deterministic initializations of {@code main} function arguments, e.g. {@code arg
   * = __VERIFIER_nondet_int;}
   */
  private ImmutableList<LineOfCode> buildMainFunctionArgNondetAssignments(
      MPORSubstitution pMainSubstitution, LogManager pLogger) {

    ImmutableList.Builder<LineOfCode> rMainArgAssignments = ImmutableList.builder();
    for (CIdExpression mainArg : pMainSubstitution.mainFunctionArgSubstitutes.values()) {
      CType mainArgType = mainArg.getExpressionType();
      Optional<CFunctionCallExpression> verifierNondet =
          VerifierNondetFunctionType.buildVerifierNondetByType(mainArgType);
      if (verifierNondet.isPresent()) {
        CFunctionCallAssignmentStatement assignment =
            SeqStatementBuilder.buildFunctionCallAssignmentStatement(
                mainArg, verifierNondet.orElseThrow());
        rMainArgAssignments.add(LineOfCode.of(1, assignment.toASTString()));
      } else {
        pLogger.log(
            Level.WARNING,
            "WARNING - could not find __VERIFIER_nondet function "
                + "for the following main function argument type: "
                + mainArgType.toASTString(""));
      }
    }
    return rMainArgAssignments.build();
  }

  /** Returns {@link LineOfCode} for thread simulation declarations. */
  private ImmutableList<LineOfCode> buildThreadSimulationVariableDeclarations(
      MPOROptions pOptions,
      CSimpleDeclaration pNumThreads,
      ImmutableList<SeqBitVectorDeclaration> pBitVectorDeclarations,
      ImmutableList<CVariableDeclaration> pPcDeclarations)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rVariableDeclarations = ImmutableList.builder();

    // NUM_THREADS
    rVariableDeclarations.add(LineOfCode.of(1, pNumThreads.toASTString()));

    // next_thread
    if (pOptions.signedNondet) {
      rVariableDeclarations.add(
          LineOfCode.of(1, SeqVariableDeclaration.NEXT_THREAD_SIGNED.toASTString()));
    } else {
      rVariableDeclarations.add(
          LineOfCode.of(1, SeqVariableDeclaration.NEXT_THREAD_UNSIGNED.toASTString()));
    }

    // pc
    if (pOptions.comments) {
      rVariableDeclarations.add(LineOfCode.empty());
      rVariableDeclarations.add(LineOfCode.of(1, SeqComment.PC_DECLARATION));
    }
    for (CVariableDeclaration pcDeclaration : pPcDeclarations) {
      rVariableDeclarations.add(LineOfCode.of(1, pcDeclaration.toASTString()));
    }

    // if enabled: bit vectors (for partial order reductions)
    if (pOptions.bitVectorReduction.isEnabled() && pOptions.bitVectorEncoding.isEnabled()) {
      for (SeqBitVectorDeclaration bitVectorDeclaration : pBitVectorDeclarations) {
        rVariableDeclarations.add(LineOfCode.of(1, bitVectorDeclaration.toASTString()));
      }
    }

    // if enabled: K and r (for thread loops iterations)
    if (pOptions.nondeterminismSource.hasThreadLoops()) {
      rVariableDeclarations.add(LineOfCode.of(1, SeqVariableDeclaration.R.toASTString()));
      if (pOptions.signedNondet) {
        rVariableDeclarations.add(LineOfCode.of(1, SeqVariableDeclaration.K_SIGNED.toASTString()));
      } else {
        rVariableDeclarations.add(
            LineOfCode.of(1, SeqVariableDeclaration.K_UNSIGNED.toASTString()));
      }
    }

    return rVariableDeclarations.build();
  }
}
