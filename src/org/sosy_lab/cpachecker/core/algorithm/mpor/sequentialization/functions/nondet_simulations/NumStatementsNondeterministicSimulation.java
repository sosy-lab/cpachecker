// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.nondet_simulations;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalOrExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.nondet_num_statements.SeqCountUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class NumStatementsNondeterministicSimulation {

  static ImmutableList<String> buildThreadSimulations(
      MPOROptions pOptions,
      SequentializationFields pFields,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses = pFields.clauses;

    ImmutableList.Builder<String> rLines = ImmutableList.builder();
    if (!pOptions.kAssignLazy) {
      rLines.addAll(buildKAssignments(pOptions, clauses, pBinaryExpressionBuilder));
      rLines.add(buildKSumAssumption(clauses.keySet(), pBinaryExpressionBuilder));
    }
    for (MPORThread thread : clauses.keySet()) {
      rLines.addAll(
          buildThreadSimulation(
              pOptions,
              pFields.ghostElements,
              thread,
              MPORUtil.withoutElement(clauses.keySet(), thread),
              clauses.get(thread),
              pBinaryExpressionBuilder));
    }
    return rLines.build();
  }

  static ImmutableList<String> buildThreadSimulation(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> rLines = ImmutableList.builder();

    // K' > 0
    CBinaryExpression kGreaterZero =
        pBinaryExpressionBuilder.buildBinaryExpression(
            pActiveThread.getKVariable().orElseThrow(),
            SeqIntegerLiteralExpression.INT_0,
            BinaryOperator.GREATER_THAN);

    // create T{thread_id}: label
    if (pActiveThread.getLabel().isPresent()) {
      rLines.add(pActiveThread.getLabel().orElseThrow().toASTString());
    }

    // create "if (pc != 0 ...)" condition
    SeqExpression ifCondition =
        buildIfConditionExpression(
            pOptions,
            pActiveThread,
            pOtherThreads,
            kGreaterZero,
            pGhostElements,
            pBinaryExpressionBuilder);
    SeqIfExpression ifExpression = new SeqIfExpression(ifCondition);
    rLines.add(SeqStringUtil.appendCurlyBracketLeft(ifExpression.toASTString()));

    if (pOptions.kAssignLazy) {
      // add the K = nondet assignment for this thread
      rLines.addAll(
          buildSingleKAssignment(
              pOptions,
              pActiveThread.getKVariable().orElseThrow(),
              pClauses.size(),
              pBinaryExpressionBuilder));
      SeqExpression lazyIfCondition =
          buildKZeroExpression(
              pOptions,
              pActiveThread,
              pOtherThreads,
              kGreaterZero,
              pGhostElements,
              pBinaryExpressionBuilder);

      // if (K > 0) ...
      SeqIfExpression lazyIfExpression = new SeqIfExpression(lazyIfCondition);
      rLines.add(SeqStringUtil.appendCurlyBracketLeft(lazyIfExpression.toASTString()));
    }

    // reset iteration only when needed i.e. after if (...) for performance
    CExpressionAssignmentStatement rReset = NondeterministicSimulationUtil.buildRReset();
    rLines.add(rReset.toASTString());

    // add the thread loop statements (assumptions and switch)
    rLines.addAll(
        buildSingleThreadClausesWithCount(
            pOptions, pGhostElements, pActiveThread, pClauses, pBinaryExpressionBuilder));

    // add additional closing bracket, if needed
    if (pOptions.kAssignLazy) {
      rLines.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    }
    rLines.add(SeqSyntax.CURLY_BRACKET_RIGHT);
    return rLines.build();
  }

  private static ImmutableList<String> buildKAssignments(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> rAssignments = ImmutableList.builder();
    for (MPORThread thread : pClauses.keySet()) {
      CIdExpression kVariable = thread.getKVariable().orElseThrow();
      rAssignments.addAll(
          buildSingleKAssignment(
              pOptions, kVariable, pClauses.get(thread).size(), pBinaryExpressionBuilder));
    }
    return rAssignments.build();
  }

  private static ImmutableList<String> buildSingleKAssignment(
      MPOROptions pOptions,
      CIdExpression pKVariable,
      int pNumStatements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> rAssignment = ImmutableList.builder();
    // k = nondet() ...
    CFunctionCallAssignmentStatement assignment =
        NondeterministicSimulationUtil.buildKNondetAssignment(pOptions, pKVariable);
    rAssignment.add(assignment.toASTString());
    // place k bound after nondet assignment
    if (pOptions.kBound) {
      CIntegerLiteralExpression numStatementsExpression =
          SeqExpressionBuilder.buildIntegerLiteralExpression(pNumStatements);
      CBinaryExpression kBoundExpression =
          pBinaryExpressionBuilder.buildBinaryExpression(
              pKVariable, numStatementsExpression, BinaryOperator.LESS_EQUAL);
      CFunctionCallStatement kBoundAssumption =
          SeqAssumptionBuilder.buildAssumption(kBoundExpression);
      rAssignment.add(kBoundAssumption.toASTString());
    }
    return rAssignment.build();
  }

  private static String buildKSumAssumption(
      ImmutableSet<MPORThread> pThreads, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableSet<CExpression> allK =
        transformedImmutableSetCopy(
            pThreads,
            t -> {
              assert t != null;
              return t.getKVariable().orElseThrow();
            });
    CExpression KSum =
        SeqExpressionBuilder.nestBinaryExpressions(
            allK, BinaryOperator.PLUS, pBinaryExpressionBuilder);
    CBinaryExpression KSumGreaterZero =
        pBinaryExpressionBuilder.buildBinaryExpression(
            KSum, SeqIntegerLiteralExpression.INT_0, BinaryOperator.GREATER_THAN);
    CFunctionCallStatement assumption = SeqAssumptionBuilder.buildAssumption(KSumGreaterZero);
    return assumption.toASTString();
  }

  private static SeqExpression buildIfConditionExpression(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      CBinaryExpression pKGreaterZero,
      GhostElements pGhostElements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    SeqExpression leftHandSide =
        buildIfConditionLeftHandSideExpression(
            pActiveThread, pGhostElements.getPcVariables(), pBinaryExpressionBuilder);
    // for lazy assignments (i.e. after if), just need if (pc != 0)
    if (pOptions.kAssignLazy) {
      return leftHandSide;
    } else {
      // for other assignments (i.e. before if), need if (pc != 0 && K > 0 ...)
      SeqExpression kZeroExpression =
          buildKZeroExpression(
              pOptions,
              pActiveThread,
              pOtherThreads,
              pKGreaterZero,
              pGhostElements,
              pBinaryExpressionBuilder);
      return new SeqLogicalAndExpression(leftHandSide, kZeroExpression);
    }
  }

  private static SeqExpression buildIfConditionLeftHandSideExpression(
      MPORThread pActiveThread,
      ProgramCounterVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CBinaryExpression pcUnequalExitPc =
        SeqExpressionBuilder.buildPcUnequalExitPc(
            pPcVariables.getPcLeftHandSide(pActiveThread.getId()), pBinaryExpressionBuilder);
    return new CToSeqExpression(pcUnequalExitPc);
  }

  private static SeqExpression buildKZeroExpression(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      CBinaryExpression pKGreaterZero,
      GhostElements pGhostElements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.kIgnoreZeroReduction) {
      // if enabled, add bit vector evaluation: "K > 0 || {bitvector_evaluation}"
      BitVectorEvaluationExpression bitVectorEvaluationExpression =
          BitVectorEvaluationBuilder.buildVariableOnlyEvaluation(
              pOptions,
              pActiveThread,
              pOtherThreads,
              pGhostElements.getBitVectorVariables().orElseThrow(),
              pBinaryExpressionBuilder);
      // ensure that thread is not at a thread sync location: !sync && !conflict
      CIdExpression syncVariable =
          pGhostElements.getThreadSynchronizationVariables().sync.get(pActiveThread);
      SeqLogicalNotExpression notSync = new SeqLogicalNotExpression(syncVariable);
      SeqLogicalAndExpression notSyncAndNotConflict =
          new SeqLogicalAndExpression(notSync, bitVectorEvaluationExpression.negate());
      // the usual bit vector expression is true if there is a conflict
      //  -> negate (we want no conflict if we ignore K == 0)
      return new SeqLogicalOrExpression(new CToSeqExpression(pKGreaterZero), notSyncAndNotConflict);
    } else {
      return new CToSeqExpression(pKGreaterZero);
    }
  }

  private static ImmutableList<String> buildSingleThreadClausesWithCount(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<String> rLines = ImmutableList.builder();
    ImmutableList<SeqThreadStatementClause> clauses =
        buildSingleThreadClauses(
            pOptions, pGhostElements, pThread, pClauses, pBinaryExpressionBuilder);

    ProgramCounterVariables pcVariables = pGhostElements.getPcVariables();
    CLeftHandSide expression = pcVariables.getPcLeftHandSide(pThread.getId());
    Optional<CFunctionCallStatement> assumption =
        NondeterministicSimulationUtil.tryBuildNextThreadActiveAssumption(
            pOptions, pcVariables, pThread, pBinaryExpressionBuilder);

    ImmutableMap<CExpression, ? extends SeqStatement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            pOptions,
            pcVariables.getPcLeftHandSide(pThread.getId()),
            clauses,
            pBinaryExpressionBuilder);
    SeqMultiControlStatement multiControlStatement =
        MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
            pOptions,
            pOptions.controlEncodingStatement,
            expression,
            assumption.isPresent()
                ? ImmutableList.of(assumption.orElseThrow())
                : ImmutableList.of(),
            expressionClauseMap,
            pBinaryExpressionBuilder);

    rLines.addAll(SeqStringUtil.splitOnNewline(multiControlStatement.toASTString()));
    return rLines.build();
  }

  private static ImmutableList<SeqThreadStatementClause> buildSingleThreadClauses(
      MPOROptions pOptions,
      GhostElements pGhostElements,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);
    // count
    CExpressionAssignmentStatement countIncrement =
        SeqStatementBuilder.buildIncrementStatement(SeqIdExpression.CNT, pBinaryExpressionBuilder);
    CExpressionAssignmentStatement countDecrement =
        SeqStatementBuilder.buildDecrementStatement(SeqIdExpression.CNT, pBinaryExpressionBuilder);
    // round
    CBinaryExpression rSmallerK =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.R, pThread.getKVariable().orElseThrow(), BinaryOperator.LESS_THAN);
    CExpressionAssignmentStatement rIncrement =
        SeqStatementBuilder.buildIncrementStatement(SeqIdExpression.R, pBinaryExpressionBuilder);
    // sync
    CIdExpression syncVariable =
        pGhostElements.getThreadSynchronizationVariables().sync.get(pThread);

    ImmutableList.Builder<SeqThreadStatementClause> updatedClauses = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        newBlocks.add(
            injectCountAndRoundGotoIntoBlock(
                pOptions,
                block,
                countIncrement,
                countDecrement,
                rSmallerK,
                rIncrement,
                syncVariable,
                labelClauseMap));
      }
      updatedClauses.add(clause.cloneWithBlocks(newBlocks.build()));
    }
    return updatedClauses.build();
  }

  // Injections ====================================================================================

  private static SeqThreadStatementBlock injectCountAndRoundGotoIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      CExpressionAssignmentStatement pCountIncrement,
      CExpressionAssignmentStatement pCountDecrement,
      CBinaryExpression pRSmallerK,
      CExpressionAssignmentStatement pRIncrement,
      CIdExpression pSyncVariable,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    SeqThreadStatementBlock withCountUpdate =
        tryInjectCountUpdatesIntoBlock(pOptions, pBlock, pCountIncrement, pCountDecrement);
    SeqThreadStatementBlock withRoundGoto =
        NondeterministicSimulationUtil.injectRoundGotoIntoBlock(
            pOptions, withCountUpdate, pRSmallerK, pRIncrement, pLabelClauseMap);
    return NondeterministicSimulationUtil.injectSyncUpdatesIntoBlock(
        pOptions, withRoundGoto, pSyncVariable, pLabelClauseMap);
  }

  private static SeqThreadStatementBlock tryInjectCountUpdatesIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      CExpressionAssignmentStatement pCountIncrement,
      CExpressionAssignmentStatement pCountDecrement) {

    if (!pOptions.isThreadCountRequired()) {
      return pBlock;
    }
    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      SeqThreadStatement withCountUpdates =
          tryInjectCountUpdatesIntoStatement(pCountIncrement, pCountDecrement, statement);
      newStatements.add(withCountUpdates);
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static SeqThreadStatement tryInjectCountUpdatesIntoStatement(
      CExpressionAssignmentStatement pCountIncrement,
      CExpressionAssignmentStatement pCountDecrement,
      SeqThreadStatement pStatement) {

    if (pStatement instanceof SeqThreadCreationStatement) {
      SeqCountUpdateStatement countUpdate = new SeqCountUpdateStatement(pCountIncrement);
      return pStatement.cloneAppendingInjectedStatements(ImmutableList.of(countUpdate));

    } else if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc == Sequentialization.EXIT_PC) {
        SeqCountUpdateStatement countUpdate = new SeqCountUpdateStatement(pCountDecrement);
        return pStatement.cloneAppendingInjectedStatements(ImmutableList.of(countUpdate));
      }
    }
    return pStatement;
  }
}
