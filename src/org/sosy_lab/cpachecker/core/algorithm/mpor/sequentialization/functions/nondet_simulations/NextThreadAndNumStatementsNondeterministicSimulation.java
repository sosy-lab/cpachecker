// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.nondet_simulations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCodeUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class NextThreadAndNumStatementsNondeterministicSimulation {

  static ImmutableList<LineOfCode> buildThreadSimulations(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CFunctionCallAssignmentStatement kNondet =
        NondeterministicSimulationUtil.buildKNondetAssignment(pOptions);
    CBinaryExpression kGreaterZero =
        NondeterministicSimulationUtil.buildKGreaterZero(pBinaryExpressionBuilder);
    CFunctionCallStatement kGreaterZeroAssumption =
        SeqAssumptionBuilder.buildAssumption(kGreaterZero);
    CExpressionAssignmentStatement rReset = NondeterministicSimulationUtil.buildRReset();
    return buildThreadSimulations(
        pOptions,
        pPcVariables,
        pClauses,
        kNondet,
        kGreaterZeroAssumption,
        rReset,
        pBinaryExpressionBuilder);
  }

  private static ImmutableList<LineOfCode> buildThreadSimulations(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CFunctionCallAssignmentStatement pKNondet,
      CFunctionCallStatement pKGreaterZeroAssumption,
      CExpressionAssignmentStatement pRReset,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<LineOfCode> rLines = ImmutableList.builder();

    // assigning K and r is necessary only once since we use next_thread
    rLines.add(LineOfCode.of(2, pKNondet.toASTString()));
    rLines.add(LineOfCode.of(2, pKGreaterZeroAssumption.toASTString()));
    rLines.add(LineOfCode.of(2, pRReset.toASTString()));

    ImmutableList<SeqMultiControlStatement> innerMultiControlStatements =
        buildInnerMultiControlStatements(
            pOptions, pPcVariables, pClauses, pBinaryExpressionBuilder);
    SeqMultiControlStatement outerMultiControlStatement =
        NondeterministicSimulationUtil.buildOuterMultiControlStatement(
            pOptions, innerMultiControlStatements, pBinaryExpressionBuilder);
    rLines.addAll(LineOfCodeUtil.buildLinesOfCode(outerMultiControlStatement.toASTString()));

    return rLines.build();
  }

  private static ImmutableList<SeqMultiControlStatement> buildInnerMultiControlStatements(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqMultiControlStatement> rStatements = ImmutableList.builder();
    for (var entry : pClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      ImmutableList<SeqThreadStatementClause> clauses = entry.getValue();
      rStatements.add(
          buildSingleThreadMultiControlStatementWithoutCount(
              pOptions, pPcVariables, thread, clauses, pBinaryExpressionBuilder));
    }
    return rStatements.build();
  }

  private static SeqMultiControlStatement buildSingleThreadMultiControlStatementWithoutCount(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList<SeqThreadStatementClause> clauses =
        buildSingleThreadClausesWithoutCount(pClauses, pBinaryExpressionBuilder);
    CLeftHandSide expression = pPcVariables.getPcLeftHandSide(pThread.id);
    Optional<CFunctionCallStatement> assumption =
        NondeterministicSimulationUtil.buildNextThreadActiveAssumption(
            pOptions, pPcVariables, pThread, pBinaryExpressionBuilder);
    return MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
        pOptions,
        pOptions.controlEncodingStatement,
        expression,
        assumption,
        clauses,
        3,
        pBinaryExpressionBuilder);
  }

  private static ImmutableList<SeqThreadStatementClause> buildSingleThreadClausesWithoutCount(
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
          NondeterministicSimulationUtil.injectRoundGotoIntoBlock(
              clause.block, rSmallerK, rIncrement, labelClauseMap);
      // then inject into merged blocks
      ImmutableList.Builder<SeqThreadStatementBlock> newMergedBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock mergedBlock : clause.mergedBlocks) {
        newMergedBlocks.add(
            NondeterministicSimulationUtil.injectRoundGotoIntoBlock(
                mergedBlock, rSmallerK, rIncrement, labelClauseMap));
      }
      updatedClauses.add(
          clause.cloneWithBlock(newBlock).cloneWithMergedBlocks(newMergedBlocks.build()));
    }
    return updatedClauses.build();
  }
}
