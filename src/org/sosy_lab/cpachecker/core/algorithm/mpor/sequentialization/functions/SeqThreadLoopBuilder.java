// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumption;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqSingleControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqSingleControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.function_call.SeqFunctionCallStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLoopLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadLoopBuilder {

  protected static ImmutableList<LineOfCode> buildThreadLoopsSwitchStatements(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableListMultimap<MPORThread, SeqAssumption> pThreadAssumptions,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoops = ImmutableList.builder();

    CFunctionCallAssignmentStatement kNondet =
        SeqStatementBuilder.buildFunctionCallAssignmentStatement(
            SeqIdExpression.K,
            pOptions.signedNondet
                ? SeqExpressionBuilder.buildVerifierNondetInt()
                : SeqExpressionBuilder.buildVerifierNondetUint());
    CBinaryExpression kGreaterZero =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.K, SeqIntegerLiteralExpression.INT_0, BinaryOperator.GREATER_THAN);
    CExpressionAssignmentStatement rReset =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            SeqIdExpression.R, SeqIntegerLiteralExpression.INT_0);
    CExpressionAssignmentStatement rIncrement =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            SeqIdExpression.R,
            pBinaryExpressionBuilder.buildBinaryExpression(
                SeqIdExpression.R, SeqIntegerLiteralExpression.INT_1, BinaryOperator.PLUS));

    rThreadLoops.addAll(
        SeqThreadLoopBuilder.buildThreadLoops(
            pOptions,
            pPcVariables,
            pThreadAssumptions,
            pCaseClauses,
            kNondet,
            kGreaterZero,
            rReset,
            rIncrement,
            pBinaryExpressionBuilder));

    return rThreadLoops.build();
  }

  private static ImmutableList<LineOfCode> buildThreadLoops(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableListMultimap<MPORThread, SeqAssumption> pThreadAssumptions,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses,
      CFunctionCallAssignmentStatement pKNondet,
      CBinaryExpression pKGreaterZero,
      CExpressionAssignmentStatement pRReset,
      CExpressionAssignmentStatement pRIncrement,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.threadLoopsNext) {
      return buildThreadLoopsWithNextThread(
          pOptions,
          pPcVariables,
          pThreadAssumptions,
          pCaseClauses,
          pKNondet,
          pKGreaterZero,
          pRReset,
          pRIncrement,
          pBinaryExpressionBuilder);
    } else {
      return buildThreadLoopsWithoutNextThread(
          pOptions,
          pPcVariables,
          pThreadAssumptions,
          pCaseClauses,
          pKNondet,
          pKGreaterZero,
          pRReset,
          pRIncrement,
          pBinaryExpressionBuilder);
    }
  }

  private static ImmutableList<LineOfCode> buildThreadLoopsWithNextThread(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableListMultimap<MPORThread, SeqAssumption> pThreadAssumptions,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses,
      CFunctionCallAssignmentStatement pKNondet,
      CBinaryExpression pKGreaterZero,
      CExpressionAssignmentStatement pRReset,
      CExpressionAssignmentStatement pRIncrement,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoops = ImmutableList.builder();

    SeqFunctionCallStatement assumeKGreaterZero =
        new SeqFunctionCallStatement(
            SeqAssumptionBuilder.buildAssumeCall(new CToSeqExpression(pKGreaterZero)));

    rThreadLoops.add(LineOfCode.of(2, pKNondet.toASTString()));
    rThreadLoops.add(LineOfCode.of(2, assumeKGreaterZero.toASTString()));
    rThreadLoops.add(LineOfCode.of(2, pRReset.toASTString()));

    int i = 0;
    for (var entry : pCaseClauses.entrySet()) {
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
          buildThreadLoop(
              pOptions,
              pPcVariables,
              thread,
              pThreadAssumptions.get(thread),
              pRIncrement,
              cases,
              pBinaryExpressionBuilder));
      i++;
    }
    rThreadLoops.add(LineOfCode.of(2, SeqSyntax.CURLY_BRACKET_RIGHT));

    return rThreadLoops.build();
  }

  private static ImmutableList<LineOfCode> buildThreadLoopsWithoutNextThread(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableListMultimap<MPORThread, SeqAssumption> pThreadAssumptions,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses,
      CFunctionCallAssignmentStatement pKNondet,
      CBinaryExpression pKGreaterZero,
      CExpressionAssignmentStatement pRReset,
      CExpressionAssignmentStatement pRIncrement,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoops = ImmutableList.builder();
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      ImmutableList<SeqThreadStatementClause> cases = entry.getValue();

      // choose nondet iterations and reset current iteration before each loop
      rThreadLoops.add(LineOfCode.of(2, pKNondet.toASTString()));
      rThreadLoops.add(LineOfCode.of(2, pRReset.toASTString()));

      // add condition if loop still active and K > 0
      CBinaryExpression pcUnequalExitPc =
          SeqExpressionBuilder.buildPcUnequalExitPc(
              pPcVariables, thread.id, pBinaryExpressionBuilder);
      SeqLogicalAndExpression loopCondition =
          new SeqLogicalAndExpression(pcUnequalExitPc, pKGreaterZero);
      SeqSingleControlFlowStatement ifStatement =
          new SeqSingleControlFlowStatement(loopCondition, SeqControlFlowStatementType.IF);
      rThreadLoops.add(
          LineOfCode.of(2, SeqStringUtil.appendOpeningCurly(ifStatement.toASTString())));

      // add the thread loop statements (assumptions and switch)
      rThreadLoops.addAll(
          buildThreadLoop(
              pOptions,
              pPcVariables,
              thread,
              pThreadAssumptions.get(thread),
              pRIncrement,
              cases,
              pBinaryExpressionBuilder));
      rThreadLoops.add(LineOfCode.of(2, SeqSyntax.CURLY_BRACKET_RIGHT));
    }
    return rThreadLoops.build();
  }

  private static ImmutableList<LineOfCode> buildThreadLoop(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      MPORThread pThread,
      ImmutableList<SeqAssumption> pThreadAssumptions,
      CExpressionAssignmentStatement pRIncrement,
      ImmutableList<SeqThreadStatementClause> pCaseClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rThreadLoop = ImmutableList.builder();

    // create assumption and switch labels
    // TODO maybe add per-variable labels so that even less assumes are evaluated?
    SeqThreadLoopLabelStatement assumeLabel =
        new SeqThreadLoopLabelStatement(
            SeqNameUtil.buildThreadAssumeLabelName(pOptions, pThread.id));

    ImmutableList<LineOfCode> switchStatement =
        buildThreadLoopSwitchStatement(
            pOptions,
            pPcVariables,
            pThread,
            assumeLabel,
            pCaseClauses,
            3,
            pBinaryExpressionBuilder);

    // add all lines of code: loop head, assumptions, iteration increment, switch statement
    rThreadLoop.add(LineOfCode.of(3, assumeLabel.toASTString()));
    rThreadLoop.addAll(buildThreadLoopAssumptions(pThreadAssumptions));
    rThreadLoop.add(LineOfCode.of(3, pRIncrement.toASTString()));
    rThreadLoop.addAll(switchStatement);

    return rThreadLoop.build();
  }

  private static ImmutableList<LineOfCode> buildThreadLoopSwitchStatement(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      MPORThread pThread,
      SeqThreadLoopLabelStatement pAssumeLabel,
      ImmutableList<SeqThreadStatementClause> pCaseClauses,
      int pTabs,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap<Integer, SeqThreadStatementClause> labelValueMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pCaseClauses);
    CExpression pcExpression = pPcVariables.get(pThread.id);
    CBinaryExpression iterationSmallerMax =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.R, SeqIdExpression.K, BinaryOperator.LESS_THAN);

    ImmutableList.Builder<SeqThreadStatementClause> pUpdatedCases = ImmutableList.builder();
    for (SeqThreadStatementClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
      for (SeqThreadStatement statement : caseClause.block.getStatements()) {
        SeqThreadStatement newStatement =
            SeqThreadStatementClauseUtil.recursivelyInjectGotoThreadLoopLabels(
                iterationSmallerMax, pAssumeLabel, statement, labelValueMap);
        newStatements.add(newStatement);
      }
      pUpdatedCases.add(caseClause.cloneWithBlockStatements(newStatements.build()));
    }
    SeqSwitchStatement switchStatement =
        new SeqSwitchStatement(pOptions, pcExpression, pUpdatedCases.build(), pTabs);
    return LineOfCodeUtil.buildLinesOfCode(switchStatement.toASTString());
  }

  private static ImmutableList<LineOfCode> buildThreadLoopAssumptions(
      ImmutableList<SeqAssumption> pAssumptions) throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rAssumptions = ImmutableList.builder();
    for (SeqAssumption assumption : pAssumptions) {
      // we only add antecedents for thread loops
      SeqFunctionCallExpression assumeCall =
          SeqAssumptionBuilder.buildAssumeCall(assumption.antecedent);
      rAssumptions.add(LineOfCode.of(3, assumeCall.toASTString() + SeqSyntax.SEMICOLON));
    }
    return rAssumptions.build();
  }
}
