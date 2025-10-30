// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.nondet_num_statements.SeqCountUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeUtil;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;

public class NumStatementsNondeterministicSimulation {

  static String buildThreadSimulations(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses = pFields.clauses;

    StringBuilder rLines = new StringBuilder();
    for (MPORThread thread : clauses.keySet()) {
      rLines.append(
          buildSingleThreadSimulation(
              pOptions,
              pFields.ghostElements,
              thread,
              MPORUtil.withoutElement(clauses.keySet(), thread),
              clauses.get(thread),
              pUtils));
    }
    return rLines.toString();
  }

  static String buildSingleThreadSimulation(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableList<SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    StringBuilder rLines = new StringBuilder();

    // create T{thread_id}: label
    if (pGhostElements.isThreadLabelPresent(pActiveThread)) {
      rLines.append(pGhostElements.getThreadLabelByThread(pActiveThread).toASTString());
    }

    // create "if (pc != 0 ...)" condition
    CBinaryExpression ifCondition =
        SeqExpressionBuilder.buildPcUnequalExitPc(
            pGhostElements.getPcVariables().getPcLeftHandSide(pActiveThread.id()),
            pUtils.binaryExpressionBuilder());
    ImmutableList.Builder<String> ifBlock = ImmutableList.builder();

    // add the round_max = nondet assignment for this thread
    ifBlock.add(
        SeqStatementBuilder.buildNondetIntegerAssignment(pOptions, SeqIdExpressions.ROUND_MAX)
            .toASTString());

    // if (round_max > 0) ...
    String innerIfCondition =
        buildRoundMaxGreaterZeroExpression(
            pOptions, pActiveThread, pOtherThreads, pGhostElements, pUtils);
    ImmutableList.Builder<String> innerIfBlock = ImmutableList.builder();

    // reset round only when needed i.e. after if (...) for performance
    CExpressionAssignmentStatement roundReset = NondeterministicSimulationUtil.buildRoundReset();
    innerIfBlock.add(roundReset.toASTString());

    // add the thread simulation statements
    innerIfBlock.add(
        buildSingleThreadClausesWithCount(
            pOptions, pGhostElements, pActiveThread, pClauses, pUtils.binaryExpressionBuilder()));
    SeqBranchStatement innerIfStatement =
        new SeqBranchStatement(innerIfCondition, innerIfBlock.build());
    ifBlock.add(innerIfStatement.toASTString());
    SeqBranchStatement ifStatement =
        new SeqBranchStatement(ifCondition.toASTString(), ifBlock.build());

    // add all and return
    return rLines.append(ifStatement.toASTString()).toString();
  }

  private static String buildRoundMaxGreaterZeroExpression(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      GhostElements pGhostElements,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    CBinaryExpressionBuilder binaryExpressionBuilder = pUtils.binaryExpressionBuilder();
    // round_max > 0
    CExpression roundMaxGreaterZero =
        binaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpressions.ROUND_MAX,
            SeqIntegerLiteralExpressions.INT_0,
            BinaryOperator.GREATER_THAN);

    if (!pOptions.reduceIgnoreSleep()) {
      return roundMaxGreaterZero.toASTString();
    }
    // if enabled, add bit vector evaluation: "round_max > 0 || {bitvector_evaluation}"
    BitVectorEvaluationExpression bitVectorEvaluationExpression =
        BitVectorEvaluationBuilder.buildVariableOnlyEvaluation(
            pOptions,
            pActiveThread,
            pOtherThreads,
            pGhostElements.bitVectorVariables().orElseThrow(),
            pUtils);
    // ensure that thread is not at a thread sync location: !sync && !conflict
    CIdExpression syncFlag = pGhostElements.threadSyncFlags().getSyncFlag(pActiveThread);
    CBinaryExpression notSync = binaryExpressionBuilder.negateExpressionAndSimplify(syncFlag);
    ExpressionTree<String> notSyncAndNotConflict =
        And.of(
            ExpressionTreeUtil.toExpressionTree(
                notSync.toASTString(), bitVectorEvaluationExpression.negate()));
    // the usual bit vector expression is true if there is a conflict
    //  -> negate (we want no conflict if we ignore round_max == 0)
    return Or.of(LeafExpression.of(roundMaxGreaterZero.toASTString()), notSyncAndNotConflict)
        .toString();
  }

  private static String buildSingleThreadClausesWithCount(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    StringBuilder rLines = new StringBuilder();

    ImmutableList<SeqThreadStatementClause> clauses =
        buildSingleThreadClauses(
            pOptions,
            pGhostElements.threadSyncFlags().getSyncFlag(pThread),
            pClauses,
            pBinaryExpressionBuilder);

    ProgramCounterVariables pcVariables = pGhostElements.getPcVariables();
    CLeftHandSide expression = pcVariables.getPcLeftHandSide(pThread.id());
    Optional<CFunctionCallStatement> assumption =
        NondeterministicSimulationUtil.tryBuildPcUnequalExitAssumption(
            pOptions, pcVariables, pThread, pBinaryExpressionBuilder);

    ImmutableMap<CExpression, ? extends SeqStatement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            pOptions,
            pcVariables.getPcLeftHandSide(pThread.id()),
            clauses,
            pBinaryExpressionBuilder);
    SeqMultiControlStatement multiControlStatement =
        MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
            pOptions.controlEncodingStatement(),
            expression,
            assumption.isPresent()
                ? ImmutableList.of(assumption.orElseThrow())
                : ImmutableList.of(),
            expressionClauseMap,
            pBinaryExpressionBuilder);

    return rLines.append(multiControlStatement.toASTString()).toString();
  }

  private static ImmutableList<SeqThreadStatementClause> buildSingleThreadClauses(
      MPOROptions pOptions,
      CIdExpression pSyncFlag,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);

    ImmutableList.Builder<SeqThreadStatementClause> updatedClauses = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        newBlocks.add(
            injectCountAndRoundGotoIntoBlock(
                pOptions, block, pSyncFlag, labelClauseMap, pBinaryExpressionBuilder));
      }
      updatedClauses.add(clause.cloneWithBlocks(newBlocks.build()));
    }
    return updatedClauses.build();
  }

  // Injections ====================================================================================

  private static SeqThreadStatementBlock injectCountAndRoundGotoIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      CIdExpression pSyncFlag,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    SeqThreadStatementBlock withCountUpdate =
        tryInjectCountUpdatesIntoBlock(pOptions, pBlock, pBinaryExpressionBuilder);
    SeqThreadStatementBlock withRoundGoto =
        NondeterministicSimulationUtil.injectRoundGotoIntoBlock(
            pOptions, withCountUpdate, pLabelClauseMap, pBinaryExpressionBuilder);
    return NondeterministicSimulationUtil.injectSyncUpdatesIntoBlock(
        pOptions, withRoundGoto, pSyncFlag, pLabelClauseMap);
  }

  private static SeqThreadStatementBlock tryInjectCountUpdatesIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (!pOptions.isThreadCountRequired()) {
      return pBlock;
    }
    ImmutableList.Builder<CSeqThreadStatement> newStatements = ImmutableList.builder();
    for (CSeqThreadStatement statement : pBlock.getStatements()) {
      CSeqThreadStatement withCountUpdates =
          tryInjectCountUpdatesIntoStatement(statement, pBinaryExpressionBuilder);
      newStatements.add(withCountUpdates);
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static CSeqThreadStatement tryInjectCountUpdatesIntoStatement(
      CSeqThreadStatement pStatement, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pStatement instanceof SeqThreadCreationStatement) {
      CExpressionAssignmentStatement countIncrement =
          SeqStatementBuilder.buildIncrementStatement(
              SeqIdExpressions.CNT, pBinaryExpressionBuilder);
      SeqCountUpdateStatement countIncrementStatement = new SeqCountUpdateStatement(countIncrement);
      return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
          pStatement, countIncrementStatement);

    } else if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc == Sequentialization.EXIT_PC) {
        CExpressionAssignmentStatement countDecrement =
            SeqStatementBuilder.buildDecrementStatement(
                SeqIdExpressions.CNT, pBinaryExpressionBuilder);
        SeqCountUpdateStatement countDecrementStatement =
            new SeqCountUpdateStatement(countDecrement);
        return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
            pStatement, countDecrementStatement);
      }
    }
    return pStatement;
  }
}
