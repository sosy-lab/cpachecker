// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.nondet_num_statements.SeqRoundGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.thread_sync.SeqSyncUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.ASeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqThreadSimulationFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Contains methods and fields used across multiple nondeterministic simulations. */
public class NondeterministicSimulationUtil {

  public static ImmutableList<String> buildThreadSimulationsByNondeterminismSource(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.nondeterminismSource) {
      case NEXT_THREAD ->
          NextThreadNondeterministicSimulation.buildThreadSimulations(
              pOptions, pFields, pUtils.getBinaryExpressionBuilder());
      case NUM_STATEMENTS ->
          NumStatementsNondeterministicSimulation.buildThreadSimulations(pOptions, pFields, pUtils);
      case NEXT_THREAD_AND_NUM_STATEMENTS ->
          NextThreadAndNumStatementsNondeterministicSimulation.buildThreadSimulations(
              pOptions, pFields, pUtils.getBinaryExpressionBuilder());
    };
  }

  public static ImmutableList<String> buildSingleThreadSimulationByNondeterminismSource(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pThread,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableList<SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    return switch (pOptions.nondeterminismSource) {
      case NEXT_THREAD ->
          NextThreadNondeterministicSimulation.buildSingleThreadSimulation(
              pOptions,
              pGhostElements.getPcVariables(),
              pThread,
              pClauses,
              pUtils.getBinaryExpressionBuilder());
      case NUM_STATEMENTS ->
          NumStatementsNondeterministicSimulation.buildSingleThreadSimulation(
              pOptions, pGhostElements, pThread, pOtherThreads, pClauses, pUtils);
      case NEXT_THREAD_AND_NUM_STATEMENTS ->
          NextThreadAndNumStatementsNondeterministicSimulation.buildSingleThreadSimulation(
              pOptions, pGhostElements, pThread, pClauses, pUtils.getBinaryExpressionBuilder());
    };
  }

  // Thread Simulation Functions ===================================================================

  public static ImmutableList<CFunctionCallStatement> buildThreadSimulationFunctionCallStatements(
      MPOROptions pOptions, SequentializationFields pFields) {

    ImmutableList.Builder<CFunctionCallStatement> rFunctionCalls = ImmutableList.builder();
    // start with main function call
    CFunctionCallStatement mainThreadFunctionCallStatement =
        pFields.mainThreadSimulationFunction.orElseThrow().getFunctionCallStatement();
    rFunctionCalls.add(mainThreadFunctionCallStatement);
    for (int i = 0; i < pOptions.loopIterations; i++) {
      for (SeqThreadSimulationFunction function : pFields.threadSimulationFunctions) {
        if (!function.thread.isMain()) {
          // continue with all other threads
          rFunctionCalls.add(function.getFunctionCallStatement());
        }
      }
      // end on main thread
      rFunctionCalls.add(mainThreadFunctionCallStatement);
    }
    return rFunctionCalls.build();
  }

  static Optional<ImmutableList<CStatement>> buildNextThreadStatementsForThreadSimulationFunction(
      MPOROptions pOptions, MPORThread pThread, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    checkArgument(
        pOptions.nondeterminismSource.isNextThreadNondeterministic(),
        "nondeterminismSource must contain NEXT_THREAD");

    if (!pOptions.loopUnrolling) {
      // when loopUnrolling is disabled, the next_thread is chosen -> no assumption needed
      return Optional.empty();
    }

    // next_thread = __VERIFIER_nondet_...()
    CFunctionCallAssignmentStatement nextThreadAssignment =
        SeqStatementBuilder.buildNondetIntegerAssignment(pOptions, SeqIdExpressions.NEXT_THREAD);
    // assume(next_thread == {thread_id})
    CBinaryExpression nextThreadEqualsThreadId =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.NEXT_THREAD,
            SeqExpressionBuilder.buildIntegerLiteralExpression(pThread.getId()),
            BinaryOperator.EQUALS);
    CFunctionCallStatement nextThreadAssumption =
        SeqAssumptionBuilder.buildAssumption(nextThreadEqualsThreadId);
    return Optional.of(ImmutableList.of(nextThreadAssignment, nextThreadAssumption));
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
        pOptions.controlEncodingThread,
        SeqIdExpressions.NEXT_THREAD,
        // the outer multi control statement never has an assumption
        ImmutableList.of(),
        pInnerMultiControlStatements,
        pBinaryExpressionBuilder);
  }

  static Optional<CFunctionCallStatement> tryBuildPcUnequalExitAssumption(
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

  // round and round_max statements/expressions ====================================================

  /** Returns the expression for {@code round = 1;} */
  static CExpressionAssignmentStatement buildRoundReset() {
    // r is set to 1, because we increment after the r < K check succeeds
    return SeqStatementBuilder.buildExpressionAssignmentStatement(
        SeqIdExpressions.ROUND, SeqIntegerLiteralExpressions.INT_1);
  }

  // round and round_max injections ================================================================

  static SeqThreadStatementBlock injectRoundGotoIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<ASeqThreadStatement> newStatements = ImmutableList.builder();
    for (ASeqThreadStatement statement : pBlock.getStatements()) {
      ASeqThreadStatement withRoundGoto =
          tryInjectRoundGotoIntoStatement(
              pOptions, statement, pLabelClauseMap, pBinaryExpressionBuilder);
      newStatements.add(withRoundGoto);
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static ASeqThreadStatement tryInjectRoundGotoIntoStatement(
      MPOROptions pOptions,
      ASeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pStatement.getTargetPc().isPresent()) {
      // int target is present -> retrieve label by pc from map
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        SeqThreadStatementClause target = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        // check if the target is a separate loop
        if (!SeqThreadStatementClauseUtil.isSeparateLoopStart(pOptions, target)) {
          return injectRoundGotoIntoStatementByTargetPc(
              targetPc, pStatement, pLabelClauseMap, pBinaryExpressionBuilder);
        }
      }
    }
    if (pStatement.getTargetGoto().isPresent()) {
      // target goto present -> use goto label for injection
      return injectRoundGotoIntoStatementByTargetGoto(
          pStatement.getTargetGoto().orElseThrow(), pStatement, pBinaryExpressionBuilder);
    }
    // no int target pc -> no replacement
    return pStatement;
  }

  private static ASeqThreadStatement injectRoundGotoIntoStatementByTargetPc(
      int pTargetPc,
      ASeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    SeqThreadStatementClause target = Objects.requireNonNull(pLabelClauseMap.get(pTargetPc));
    CBinaryExpression roundSmallerMax =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.ROUND, SeqIdExpressions.ROUND_MAX, BinaryOperator.LESS_THAN);
    CExpressionAssignmentStatement roundIncrement =
        SeqStatementBuilder.buildIncrementStatement(
            SeqIdExpressions.ROUND, pBinaryExpressionBuilder);
    SeqRoundGotoStatement roundGoto =
        new SeqRoundGotoStatement(
            roundSmallerMax,
            roundIncrement,
            Objects.requireNonNull(target).getFirstBlock().getLabel());
    return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(pStatement, roundGoto);
  }

  private static ASeqThreadStatement injectRoundGotoIntoStatementByTargetGoto(
      SeqBlockLabelStatement pTargetGoto,
      ASeqThreadStatement pStatement,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CBinaryExpression roundSmallerMax =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.ROUND, SeqIdExpressions.ROUND_MAX, BinaryOperator.LESS_THAN);
    CExpressionAssignmentStatement roundIncrement =
        SeqStatementBuilder.buildIncrementStatement(
            SeqIdExpressions.ROUND, pBinaryExpressionBuilder);
    SeqRoundGotoStatement roundGoto =
        new SeqRoundGotoStatement(roundSmallerMax, roundIncrement, pTargetGoto);
    return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(pStatement, roundGoto);
  }

  // sync injections ===============================================================================

  static SeqThreadStatementBlock injectSyncUpdatesIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      CIdExpression pSyncFlag,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    // sync variables are only required with reduceIgnoreSleep enabled
    if (!pOptions.reduceIgnoreSleep) {
      return pBlock;
    }
    ImmutableList.Builder<ASeqThreadStatement> newStatements = ImmutableList.builder();
    for (ASeqThreadStatement statement : pBlock.getStatements()) {
      ASeqThreadStatement withGoto =
          tryInjectSyncUpdateIntoStatement(statement, pSyncFlag, pLabelClauseMap);
      newStatements.add(withGoto);
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static ASeqThreadStatement tryInjectSyncUpdateIntoStatement(
      ASeqThreadStatement pStatement,
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

  private static ASeqThreadStatement injectSyncUpdateIntoStatementByTargetPc(
      ASeqThreadStatement pStatement,
      Optional<SeqThreadStatementClause> pTargetClause,
      CIdExpression pSyncVariable) {

    boolean isSync =
        pTargetClause.isPresent()
            && SeqThreadStatementUtil.anySynchronizesThreads(
                pTargetClause.orElseThrow().getAllStatements());
    CIntegerLiteralExpression value =
        isSync ? SeqIntegerLiteralExpressions.INT_1 : SeqIntegerLiteralExpressions.INT_0;
    SeqSyncUpdateStatement syncUpdate =
        new SeqSyncUpdateStatement(
            SeqStatementBuilder.buildExpressionAssignmentStatement(pSyncVariable, value));
    return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(pStatement, syncUpdate);
  }
}
