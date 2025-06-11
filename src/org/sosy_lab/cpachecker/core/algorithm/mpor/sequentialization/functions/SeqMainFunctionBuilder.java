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
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.multi.SeqBinaryIfTreeStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.multi.SeqIfElseChainStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.multi.SeqMultiControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.multi.SeqSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqMainFunctionBuilder {

  public static SeqMainFunction buildMainFunction(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      Optional<BitVectorVariables> pBitVectorVariables,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    // create clauses in main method
    ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> clauses =
        SeqThreadStatementClauseBuilder.buildClauses(
            pOptions,
            pSubstitutions,
            pSubstituteEdges,
            pBitVectorVariables,
            pPcVariables,
            pThreadSimulationVariables,
            pBinaryExpressionBuilder,
            pLogger);
    return new SeqMainFunction(
        pOptions,
        pSubstitutions,
        clauses,
        pBitVectorVariables,
        pPcVariables,
        pBinaryExpressionBuilder,
        pLogger);
  }

  static Optional<CFunctionCallStatement> buildThreadActiveAssumption(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      MPORThread pThread,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.nondeterminismSource.equals(NondeterminismSource.NUM_STATEMENTS)) {
      // without next_thread, no assumption is required due to if (pc != -1) ... check
      return Optional.empty();
    }
    if (pOptions.scalarPc) {
      CBinaryExpression threadActiveExpression =
          SeqExpressionBuilder.buildPcUnequalExitPc(
              pPcVariables.getPcLeftHandSide(pThread.id), pBinaryExpressionBuilder);
      CFunctionCallStatement assumeCall =
          SeqAssumptionBuilder.buildAssumption(threadActiveExpression);
      return Optional.of(assumeCall);
    }
    return Optional.empty();
  }

  /** Creates the {@link SeqMultiControlFlowStatement} for {@code pThread}. */
  static SeqMultiControlFlowStatement buildMultiControlFlowStatement(
      MPOROptions pOptions,
      PcVariables pPcVariables,
      MPORThread pThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      int pTabs,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CLeftHandSide pcExpression = pPcVariables.getPcLeftHandSide(pThread.id);
    Optional<CFunctionCallStatement> assumption =
        SeqMainFunctionBuilder.buildThreadActiveAssumption(
            pOptions, pPcVariables, pThread, pBinaryExpressionBuilder);
    return switch (pOptions.controlEncodingStatement) {
      case BINARY_IF_TREE ->
          new SeqBinaryIfTreeStatement(
              pcExpression, assumption, pClauses, pTabs, pBinaryExpressionBuilder);
      case IF_ELSE_CHAIN ->
          new SeqIfElseChainStatement(
              pcExpression,
              Sequentialization.INIT_PC,
              assumption,
              pClauses,
              pTabs,
              pBinaryExpressionBuilder);
      case SWITCH_CASE ->
          new SeqSwitchStatement(pOptions, pcExpression, assumption, pClauses, pTabs);
    };
  }
}
