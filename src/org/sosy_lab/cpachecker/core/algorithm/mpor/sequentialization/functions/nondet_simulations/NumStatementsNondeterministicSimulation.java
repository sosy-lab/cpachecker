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
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalAndExpression;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class NumStatementsNondeterministicSimulation {

  static ImmutableList<LineOfCode> buildThreadSimulations(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CExpressionAssignmentStatement rReset = NondeterministicSimulationUtil.buildRReset();
    return buildThreadSimulations(
        pOptions, pPcVariables, pClauses, rReset, pBinaryExpressionBuilder);
  }

  private static ImmutableList<LineOfCode> buildThreadSimulations(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CExpressionAssignmentStatement pRReset,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rLines = ImmutableList.builder();
    rLines.addAll(buildKAssignments(pOptions, pClauses.keySet()));
    rLines.add(buildKSumAssumption(pClauses.keySet(), pBinaryExpressionBuilder));
    for (var entry : pClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      ImmutableList<SeqThreadStatementClause> clauses = entry.getValue();

      // add condition if thread still active and K > 0
      CBinaryExpression pcUnequalExitPc =
          SeqExpressionBuilder.buildPcUnequalExitPc(
              pPcVariables.getPcLeftHandSide(thread.id), pBinaryExpressionBuilder);
      CBinaryExpression kGreaterZero =
          pBinaryExpressionBuilder.buildBinaryExpression(
              thread.getKVariable().orElseThrow(),
              SeqIntegerLiteralExpression.INT_0,
              BinaryOperator.GREATER_THAN);
      SeqLogicalAndExpression loopCondition =
          new SeqLogicalAndExpression(pcUnequalExitPc, kGreaterZero);
      SeqIfExpression ifExpression = new SeqIfExpression(loopCondition);
      rLines.add(LineOfCode.of(SeqStringUtil.appendCurlyBracketRight(ifExpression.toASTString())));

      // reset iteration only when needed i.e. after if (...) for performance
      rLines.add(LineOfCode.of(pRReset.toASTString()));

      // add the thread loop statements (assumptions and switch)
      rLines.addAll(
          buildSingleThreadClausesWithCount(
              pOptions, pPcVariables, thread, clauses, pBinaryExpressionBuilder));
      rLines.add(LineOfCode.of(SeqSyntax.CURLY_BRACKET_RIGHT));
    }
    return rLines.build();
  }

  private static ImmutableList<LineOfCode> buildKAssignments(
      MPOROptions pOptions, ImmutableSet<MPORThread> pThreads) {

    ImmutableList.Builder<LineOfCode> rAssignments = ImmutableList.builder();
    for (MPORThread thread : pThreads) {
      CFunctionCallAssignmentStatement assignment =
          NondeterministicSimulationUtil.buildKNondetAssignment(
              pOptions, thread.getKVariable().orElseThrow());
      rAssignments.add(LineOfCode.of(assignment.toASTString()));
    }
    return rAssignments.build();
  }

  private static LineOfCode buildKSumAssumption(
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
    return LineOfCode.of(assumption.toASTString());
  }

  private static ImmutableList<LineOfCode> buildSingleThreadClausesWithCount(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rLines = ImmutableList.builder();
    ImmutableList<SeqThreadStatementClause> clauses =
        buildSingleThreadClausesWithCount(pThread, pClauses, pBinaryExpressionBuilder);
    CLeftHandSide expression = pPcVariables.getPcLeftHandSide(pThread.id);
    Optional<CFunctionCallStatement> assumption =
        NondeterministicSimulationUtil.tryBuildNextThreadActiveAssumption(
            pOptions, pPcVariables, pThread, pBinaryExpressionBuilder);

    ImmutableMap<CExpression, ? extends SeqStatement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            pOptions,
            pPcVariables.getPcLeftHandSide(pThread.id),
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
            pThread.endLabel,
            tryBuildLastThreadUpdate(pOptions, pThread),
            pBinaryExpressionBuilder);

    rLines.addAll(LineOfCodeUtil.buildLinesOfCode(multiControlStatement.toASTString()));
    return rLines.build();
  }

  private static ImmutableList<SeqThreadStatementClause> buildSingleThreadClausesWithCount(
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);
    CExpressionAssignmentStatement countIncrement =
        SeqStatementBuilder.buildIncrementStatement(SeqIdExpression.CNT, pBinaryExpressionBuilder);
    CExpressionAssignmentStatement countDecrement =
        SeqStatementBuilder.buildDecrementStatement(SeqIdExpression.CNT, pBinaryExpressionBuilder);
    CBinaryExpression rSmallerK =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.R, pThread.getKVariable().orElseThrow(), BinaryOperator.LESS_THAN);
    CExpressionAssignmentStatement rIncrement =
        SeqStatementBuilder.buildIncrementStatement(SeqIdExpression.R, pBinaryExpressionBuilder);

    ImmutableList.Builder<SeqThreadStatementClause> updatedClauses = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      // first inject into block
      SeqThreadStatementBlock newBlock =
          injectCountAndRoundGotoIntoBlock(
              clause.block, countIncrement, countDecrement, rSmallerK, rIncrement, labelClauseMap);
      // then inject into merged blocks
      ImmutableList.Builder<SeqThreadStatementBlock> newMergedBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock mergedBlock : clause.mergedBlocks) {
        newMergedBlocks.add(
            injectCountAndRoundGotoIntoBlock(
                mergedBlock,
                countIncrement,
                countDecrement,
                rSmallerK,
                rIncrement,
                labelClauseMap));
      }
      updatedClauses.add(
          clause.cloneWithBlock(newBlock).cloneWithMergedBlocks(newMergedBlocks.build()));
    }
    return updatedClauses.build();
  }

  // Count injection ===============================================================================

  private static SeqThreadStatementBlock injectCountAndRoundGotoIntoBlock(
      SeqThreadStatementBlock pBlock,
      CExpressionAssignmentStatement pCountIncrement,
      CExpressionAssignmentStatement pCountDecrement,
      CBinaryExpression pRSmallerK,
      CExpressionAssignmentStatement pRIncrement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    return NondeterministicSimulationUtil.injectRoundGotoIntoBlock(
        injectCountUpdatesIntoBlock(pBlock, pCountIncrement, pCountDecrement),
        pRSmallerK,
        pRIncrement,
        pLabelClauseMap);
  }

  private static SeqThreadStatementBlock injectCountUpdatesIntoBlock(
      SeqThreadStatementBlock pBlock,
      CExpressionAssignmentStatement pCountIncrement,
      CExpressionAssignmentStatement pCountDecrement) {

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

  // last_thread update ============================================================================

  private static Optional<CExpressionAssignmentStatement> tryBuildLastThreadUpdate(
      MPOROptions pOptions, MPORThread pThread) {

    if (!pOptions.conflictReduction) {
      // without conflict reductions, there is no need for storing last_thread
      return Optional.empty();
    }
    CIntegerLiteralExpression integerLiteralExpression =
        SeqExpressionBuilder.buildIntegerLiteralExpression(pThread.id);
    return Optional.of(SeqStatementBuilder.buildLastThreadAssignment(integerLiteralExpression));
  }
}
