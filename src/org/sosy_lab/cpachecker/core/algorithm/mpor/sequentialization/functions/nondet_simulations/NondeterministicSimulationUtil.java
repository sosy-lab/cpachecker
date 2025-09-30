// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.nondet_simulations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.nondet_num_statements.SeqRoundGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.thread_sync.SeqSyncUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Contains methods and fields used across multiple nondeterministic simulations. */
public class NondeterministicSimulationUtil {

  public static ImmutableList<String> buildThreadSimulationsByNondeterminismSource(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.nondeterminismSource) {
      case NEXT_THREAD ->
          NextThreadNondeterministicSimulation.buildThreadSimulations(
              pOptions, pGhostElements.getPcVariables(), pClauses, pBinaryExpressionBuilder);
      case NUM_STATEMENTS ->
          NumStatementsNondeterministicSimulation.buildThreadSimulations(
              pOptions, pGhostElements, pClauses, pBinaryExpressionBuilder);
      case NEXT_THREAD_AND_NUM_STATEMENTS ->
          NextThreadAndNumStatementsNondeterministicSimulation.buildThreadSimulations(
              pOptions, pGhostElements, pClauses, pBinaryExpressionBuilder);
    };
  }

  // Multi Control Flow Statements =================================================================

  /**
   * Creates the outer {@link SeqMultiControlStatement} used for matching the {@code next_thread}
   * variable.
   */
  static SeqMultiControlStatement buildOuterMultiControlStatement(
      MPOROptions pOptions,
      ImmutableMap<CExpression, SeqMultiControlStatement> pInnerMultiControlStatements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    return MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
        pOptions,
        pOptions.controlEncodingThread,
        SeqIdExpression.NEXT_THREAD,
        // the outer multi control statement never has an assumption
        ImmutableList.of(),
        pInnerMultiControlStatements,
        pBinaryExpressionBuilder);
  }

  static Optional<CFunctionCallStatement> tryBuildNextThreadActiveAssumption(
      MPOROptions pOptions,
      ProgramCounterVariables pPcVariables,
      MPORThread pThread,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (!pOptions.nondeterminismSource.isNextThreadNondeterministic()) {
      // without next_thread, no assumption is required due to if (pc != -1) ... check
      return Optional.empty();
    }
    if (pOptions.scalarPc) {
      CBinaryExpression threadActiveExpression =
          SeqExpressionBuilder.buildPcUnequalExitPc(
              pPcVariables.getPcLeftHandSide(pThread.getId()), pBinaryExpressionBuilder);
      CFunctionCallStatement assumeCall =
          SeqAssumptionBuilder.buildAssumption(threadActiveExpression);
      return Optional.of(assumeCall);
    }
    return Optional.empty();
  }

  // r and K statements/expressions ================================================================

  /** Returns the expression for {@code K = __VERIFIER_nondet_{int, uint}()} */
  public static CFunctionCallAssignmentStatement buildKNondetAssignment(
      MPOROptions pOptions, CIdExpression pKVariable) {

    return SeqStatementBuilder.buildFunctionCallAssignmentStatement(
        pKVariable,
        pOptions.nondeterminismSigned
            ? VerifierNondetFunctionType.INT.getFunctionCallExpression()
            : VerifierNondetFunctionType.UINT.getFunctionCallExpression());
  }

  /** Returns the expression for {@code r = 1;} */
  static CExpressionAssignmentStatement buildRReset() {
    // r is set to 1, because we increment after the r < K check succeeds
    return SeqStatementBuilder.buildExpressionAssignmentStatement(
        SeqIdExpression.R, SeqIntegerLiteralExpression.INT_1);
  }

  // r and K injections ============================================================================

  static SeqThreadStatementBlock injectRoundGotoIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      CBinaryExpression pRSmallerK,
      CExpressionAssignmentStatement pRIncrement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      SeqThreadStatement withRoundGoto =
          tryInjectRoundGotoIntoStatement(
              pOptions, pRSmallerK, pRIncrement, statement, pLabelClauseMap);
      newStatements.add(withRoundGoto);
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static SeqThreadStatement tryInjectRoundGotoIntoStatement(
      MPOROptions pOptions,
      CBinaryExpression pRSmallerK,
      CExpressionAssignmentStatement pRIncrement,
      SeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (pStatement.getTargetPc().isPresent()) {
      // int target is present -> retrieve label by pc from map
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        SeqThreadStatementClause target = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        // check if the target is a separate loop
        if (!SeqThreadStatementClauseUtil.isSeparateLoopStart(pOptions, target)) {
          return injectRoundGotoIntoStatementByTargetPc(
              targetPc, pRSmallerK, pRIncrement, pStatement, pLabelClauseMap);
        }
      }
    }
    if (pStatement.getTargetGoto().isPresent()) {
      // target goto present -> use goto label for injection
      return injectRoundGotoIntoStatementByTargetGoto(
          pStatement.getTargetGoto().orElseThrow(), pRSmallerK, pRIncrement, pStatement);
    }
    // no int target pc -> no replacement
    return pStatement;
  }

  private static SeqThreadStatement injectRoundGotoIntoStatementByTargetPc(
      int pTargetPc,
      CBinaryExpression pRSmallerK,
      CExpressionAssignmentStatement pRIncrement,
      SeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    SeqThreadStatementClause target = Objects.requireNonNull(pLabelClauseMap.get(pTargetPc));
    SeqRoundGotoStatement roundGoto =
        new SeqRoundGotoStatement(
            pRSmallerK, pRIncrement, Objects.requireNonNull(target).getFirstBlock().getLabel());
    return pStatement.cloneAppendingInjectedStatements(ImmutableList.of(roundGoto));
  }

  private static SeqThreadStatement injectRoundGotoIntoStatementByTargetGoto(
      SeqBlockLabelStatement pTargetGoto,
      CBinaryExpression pRSmallerK,
      CExpressionAssignmentStatement pRIncrement,
      SeqThreadStatement pStatement) {

    SeqRoundGotoStatement roundGoto =
        new SeqRoundGotoStatement(pRSmallerK, pRIncrement, pTargetGoto);
    return pStatement.cloneAppendingInjectedStatements(ImmutableList.of(roundGoto));
  }

  // sync injections ===============================================================================

  static SeqThreadStatementBlock injectSyncUpdatesIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      CIdExpression pSyncVariable,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    // sync variables are only required with kIgnoreZeroReduction enabled
    if (!pOptions.kIgnoreZeroReduction) {
      return pBlock;
    }
    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      SeqThreadStatement withGoto =
          tryInjectSyncUpdateIntoStatement(statement, pSyncVariable, pLabelClauseMap);
      newStatements.add(withGoto);
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static SeqThreadStatement tryInjectSyncUpdateIntoStatement(
      SeqThreadStatement pStatement,
      CIdExpression pSyncVariable,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (pStatement.getTargetPc().isPresent()) {
      // int target is present -> retrieve label by pc from map
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        SeqThreadStatementClause targetClause =
            Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        return injectSyncUpdateIntoStatementByTargetPc(
            pStatement, Optional.of(targetClause), pSyncVariable);
      } else {
        return injectSyncUpdateIntoStatementByTargetPc(pStatement, Optional.empty(), pSyncVariable);
      }
    }
    // no int target pc -> no replacement
    return pStatement;
  }

  private static SeqThreadStatement injectSyncUpdateIntoStatementByTargetPc(
      SeqThreadStatement pStatement,
      Optional<SeqThreadStatementClause> pTargetClause,
      CIdExpression pSyncVariable) {

    boolean isSync =
        pTargetClause.isPresent()
            && SeqThreadStatementUtil.anySynchronizesThreads(
                pTargetClause.orElseThrow().getAllStatements());
    CIntegerLiteralExpression value =
        isSync ? SeqIntegerLiteralExpression.INT_1 : SeqIntegerLiteralExpression.INT_0;
    SeqSyncUpdateStatement syncUpdate =
        new SeqSyncUpdateStatement(
            SeqStatementBuilder.buildExpressionAssignmentStatement(pSyncVariable, value));
    return pStatement.cloneAppendingInjectedStatements(ImmutableList.of(syncUpdate));
  }
}
