// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.multi.SeqMultiControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single.SeqSingleControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single.SeqSingleControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqThreadLoopCountStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqThreadLoopGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadLoopBuilder {

  protected static ImmutableList<LineOfCode> buildThreadLoopsSwitchStatements(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoops = ImmutableList.builder();

    CFunctionCallAssignmentStatement kNondet =
        SeqStatementBuilder.buildFunctionCallAssignmentStatement(
            SeqIdExpression.K,
            pOptions.signedNondet
                ? VerifierNondetFunctionType.INT.getFunctionCallExpression()
                : VerifierNondetFunctionType.UINT.getFunctionCallExpression());
    CBinaryExpression kGreaterZero =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.K, SeqIntegerLiteralExpression.INT_0, BinaryOperator.GREATER_THAN);
    // r is set to 1, because we increment after the r < K check succeeds
    CExpressionAssignmentStatement rReset =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            SeqIdExpression.R, SeqIntegerLiteralExpression.INT_1);

    rThreadLoops.addAll(
        SeqThreadLoopBuilder.buildThreadLoops(
            pOptions,
            pPcVariables,
            pClauses,
            kNondet,
            kGreaterZero,
            rReset,
            pBinaryExpressionBuilder));

    return rThreadLoops.build();
  }

  private static ImmutableList<LineOfCode> buildThreadLoops(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CFunctionCallAssignmentStatement pKNondet,
      CBinaryExpression pKGreaterZero,
      CExpressionAssignmentStatement pRReset,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.nondeterminismSource.hasNextThread()) {
      return buildThreadLoopsWithNextThread(
          pOptions,
          pPcVariables,
          pClauses,
          pKNondet,
          pKGreaterZero,
          pRReset,
          pBinaryExpressionBuilder);
    } else {
      return buildThreadLoopsWithoutNextThread(
          pOptions,
          pPcVariables,
          pClauses,
          pKNondet,
          pKGreaterZero,
          pRReset,
          pBinaryExpressionBuilder);
    }
  }

  private static ImmutableList<LineOfCode> buildThreadLoopsWithNextThread(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CFunctionCallAssignmentStatement pKNondet,
      CBinaryExpression pKGreaterZero,
      CExpressionAssignmentStatement pRReset,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoops = ImmutableList.builder();

    CFunctionCallStatement assumeKGreaterZero = SeqAssumptionBuilder.buildAssumption(pKGreaterZero);

    rThreadLoops.add(LineOfCode.of(2, pKNondet.toASTString()));
    rThreadLoops.add(LineOfCode.of(2, assumeKGreaterZero.toASTString()));
    rThreadLoops.add(LineOfCode.of(2, pRReset.toASTString()));

    // TODO this code is redundant, use IfElseChain instead?
    int i = 0;
    for (var entry : pClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      CIntegerLiteralExpression threadId =
          SeqExpressionBuilder.buildIntegerLiteralExpression(thread.id);

      CBinaryExpression nextThreadEqualsThreadId =
          pBinaryExpressionBuilder.buildBinaryExpression(
              SeqIdExpression.NEXT_THREAD, threadId, BinaryOperator.EQUALS);
      // first switch case: use "if", otherwise "else if"
      SeqControlFlowStatementType statementType =
          i == 0 ? SeqControlFlowStatementType.IF : SeqControlFlowStatementType.ELSE_IF;
      SeqSingleControlFlowStatement statement =
          new SeqSingleControlFlowStatement(nextThreadEqualsThreadId, statementType);
      rThreadLoops.add(
          LineOfCode.of(
              2,
              i == 0
                  ? SeqStringUtil.appendOpeningCurly(statement.toASTString())
                  : SeqStringUtil.wrapInCurlyOutwards(statement.toASTString())));

      ImmutableList<SeqThreadStatementClause> cases = entry.getValue();
      rThreadLoops.addAll(
          buildThreadLoopWithoutCount(
              pOptions, pPcVariables, thread, cases, pBinaryExpressionBuilder));
      i++;
    }
    rThreadLoops.add(LineOfCode.of(2, SeqSyntax.CURLY_BRACKET_RIGHT));

    return rThreadLoops.build();
  }

  private static ImmutableList<LineOfCode> buildThreadLoopsWithoutNextThread(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CFunctionCallAssignmentStatement pKNondet,
      CBinaryExpression pKGreaterZero,
      CExpressionAssignmentStatement pRReset,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoops = ImmutableList.builder();
    for (var entry : pClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      ImmutableList<SeqThreadStatementClause> cases = entry.getValue();

      // choose nondet iterations and reset current iteration before each loop
      rThreadLoops.add(LineOfCode.of(2, pKNondet.toASTString()));
      rThreadLoops.add(LineOfCode.of(2, pRReset.toASTString()));

      // add condition if loop still active and K > 0
      CBinaryExpression pcUnequalExitPc =
          SeqExpressionBuilder.buildPcUnequalExitPc(
              pPcVariables.getPcLeftHandSide(thread.id), pBinaryExpressionBuilder);
      SeqLogicalAndExpression loopCondition =
          new SeqLogicalAndExpression(pcUnequalExitPc, pKGreaterZero);
      SeqSingleControlFlowStatement ifStatement =
          new SeqSingleControlFlowStatement(loopCondition, SeqControlFlowStatementType.IF);
      rThreadLoops.add(
          LineOfCode.of(2, SeqStringUtil.appendOpeningCurly(ifStatement.toASTString())));

      // add the thread loop statements (assumptions and switch)
      rThreadLoops.addAll(
          buildThreadLoopWithCount(
              pOptions, pPcVariables, thread, cases, pBinaryExpressionBuilder));
      rThreadLoops.add(LineOfCode.of(2, SeqSyntax.CURLY_BRACKET_RIGHT));
    }
    return rThreadLoops.build();
  }

  // With Count ====================================================================================

  private static ImmutableList<LineOfCode> buildThreadLoopWithCount(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoop = ImmutableList.builder();
    ImmutableList<SeqThreadStatementClause> threadLoopClauses =
        buildThreadLoopClausesWithCount(pClauses, pBinaryExpressionBuilder);
    SeqMultiControlFlowStatement multiControlFlowStatement =
        SeqMainFunctionBuilder.buildMultiControlFlowStatement(
            pOptions, pPcVariables, pThread, threadLoopClauses, 3, pBinaryExpressionBuilder);
    rThreadLoop.addAll(LineOfCodeUtil.buildLinesOfCode(multiControlFlowStatement.toASTString()));
    return rThreadLoop.build();
  }

  private static ImmutableList<SeqThreadStatementClause> buildThreadLoopClausesWithCount(
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
            SeqIdExpression.R, SeqIdExpression.K, BinaryOperator.LESS_THAN);
    CExpressionAssignmentStatement rIncrement =
        SeqStatementBuilder.buildIncrementStatement(SeqIdExpression.R, pBinaryExpressionBuilder);

    ImmutableList.Builder<SeqThreadStatementClause> updatedClauses = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      // first inject into block
      SeqThreadStatementBlock newBlock =
          injectCountAndGotoIntoBlock(
              clause.block, countIncrement, countDecrement, rSmallerK, rIncrement, labelClauseMap);
      // then inject into merged blocks
      ImmutableList.Builder<SeqThreadStatementBlock> newMergedBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock mergedBlock : clause.mergedBlocks) {
        newMergedBlocks.add(
            injectCountAndGotoIntoBlock(
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

  // Without Count
  // ====================================================================================

  private static ImmutableList<LineOfCode> buildThreadLoopWithoutCount(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoop = ImmutableList.builder();
    ImmutableList<SeqThreadStatementClause> threadLoopClauses =
        buildThreadLoopClausesWithoutCount(pClauses, pBinaryExpressionBuilder);
    SeqMultiControlFlowStatement multiControlFlowStatement =
        SeqMainFunctionBuilder.buildMultiControlFlowStatement(
            pOptions, pPcVariables, pThread, threadLoopClauses, 3, pBinaryExpressionBuilder);
    rThreadLoop.addAll(LineOfCodeUtil.buildLinesOfCode(multiControlFlowStatement.toASTString()));
    return rThreadLoop.build();
  }

  private static ImmutableList<SeqThreadStatementClause> buildThreadLoopClausesWithoutCount(
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);
    CBinaryExpression rSmallerK =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.R, SeqIdExpression.K, BinaryOperator.LESS_THAN);
    CExpressionAssignmentStatement rIncrement =
        SeqStatementBuilder.buildIncrementStatement(SeqIdExpression.R, pBinaryExpressionBuilder);

    ImmutableList.Builder<SeqThreadStatementClause> updatedClauses = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      // first inject into block
      SeqThreadStatementBlock newBlock =
          injectThreadLoopGotoIntoBlock(clause.block, rSmallerK, rIncrement, labelClauseMap);
      // then inject into merged blocks
      ImmutableList.Builder<SeqThreadStatementBlock> newMergedBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock mergedBlock : clause.mergedBlocks) {
        newMergedBlocks.add(
            injectThreadLoopGotoIntoBlock(mergedBlock, rSmallerK, rIncrement, labelClauseMap));
      }
      updatedClauses.add(
          clause.cloneWithBlock(newBlock).cloneWithMergedBlocks(newMergedBlocks.build()));
    }
    return updatedClauses.build();
  }

  // Injection Code ================================================================================

  private static SeqThreadStatementBlock injectCountAndGotoIntoBlock(
      SeqThreadStatementBlock pBlock,
      CExpressionAssignmentStatement pCountIncrement,
      CExpressionAssignmentStatement pCountDecrement,
      CBinaryExpression pRSmallerK,
      CExpressionAssignmentStatement pRIncrement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    return injectThreadLoopGotoIntoBlock(
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

  private static SeqThreadStatementBlock injectThreadLoopGotoIntoBlock(
      SeqThreadStatementBlock pBlock,
      CBinaryExpression pRSmallerK,
      CExpressionAssignmentStatement pRIncrement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      SeqThreadStatement withGoto =
          tryInjectGotoThreadLoopLabelIntoStatement(
              pRSmallerK, pRIncrement, statement, pLabelClauseMap);
      newStatements.add(withGoto);
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  // Count In/Decrement ============================================================================

  private static SeqThreadStatement tryInjectCountUpdatesIntoStatement(
      CExpressionAssignmentStatement pCountIncrement,
      CExpressionAssignmentStatement pCountDecrement,
      SeqThreadStatement pStatement) {

    if (pStatement instanceof SeqThreadCreationStatement) {
      ImmutableList.Builder<SeqInjectedStatement> newInjections = ImmutableList.builder();
      newInjections.addAll(pStatement.getInjectedStatements());
      newInjections.add(new SeqThreadLoopCountStatement(pCountIncrement));
      return pStatement.cloneWithInjectedStatements(newInjections.build());

    } else if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc == Sequentialization.EXIT_PC) {
        ImmutableList.Builder<SeqInjectedStatement> newInjections = ImmutableList.builder();
        newInjections.addAll(pStatement.getInjectedStatements());
        newInjections.add(new SeqThreadLoopCountStatement(pCountDecrement));
        return pStatement.cloneWithInjectedStatements(newInjections.build());
      }
    }
    return pStatement;
  }

  // Goto Injection ================================================================================

  private static SeqThreadStatement tryInjectGotoThreadLoopLabelIntoStatement(
      CBinaryExpression pRSmallerMax,
      CExpressionAssignmentStatement pRIncrement,
      SeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (pStatement.getTargetPc().isPresent()) {
      // int target is present -> retrieve label by pc from map
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc != Sequentialization.EXIT_PC) {
        return injectGotoThreadLoopLabelIntoStatementByTargetPc(
            targetPc, pRSmallerMax, pRIncrement, pStatement, pLabelClauseMap);
      }
    }
    if (pStatement.getTargetGoto().isPresent()) {
      // target goto present -> use goto label for injection
      return injectGotoThreadLoopLabelIntoStatementByTargetGoto(
          pStatement.getTargetGoto().orElseThrow(), pRSmallerMax, pRIncrement, pStatement);
    }
    // no int target pc -> no replacement
    return pStatement;
  }

  private static SeqThreadStatement injectGotoThreadLoopLabelIntoStatementByTargetPc(
      int pTargetPc,
      CBinaryExpression pRSmallerMax,
      CExpressionAssignmentStatement pRIncrement,
      SeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    ImmutableList.Builder<SeqInjectedStatement> newInjections = ImmutableList.builder();
    // add previous injections BEFORE (otherwise undefined behavior in seq!)
    newInjections.addAll(pStatement.getInjectedStatements());
    SeqThreadStatementClause target = Objects.requireNonNull(pLabelClauseMap.get(pTargetPc));
    newInjections.add(
        new SeqThreadLoopGotoStatement(
            pRSmallerMax, pRIncrement, Objects.requireNonNull(target).block.getGotoLabel()));
    return pStatement.cloneWithInjectedStatements(newInjections.build());
  }

  private static SeqThreadStatement injectGotoThreadLoopLabelIntoStatementByTargetGoto(
      SeqBlockGotoLabelStatement pTargetGoto,
      CBinaryExpression pRSmallerMax,
      CExpressionAssignmentStatement pRIncrement,
      SeqThreadStatement pStatement) {

    ImmutableList.Builder<SeqInjectedStatement> newInjections = ImmutableList.builder();
    // add previous injections BEFORE (otherwise undefined behavior in seq!)
    newInjections.addAll(pStatement.getInjectedStatements());
    newInjections.add(new SeqThreadLoopGotoStatement(pRSmallerMax, pRIncrement, pTargetGoto));
    return pStatement.cloneWithInjectedStatements(newInjections.build());
  }
}
