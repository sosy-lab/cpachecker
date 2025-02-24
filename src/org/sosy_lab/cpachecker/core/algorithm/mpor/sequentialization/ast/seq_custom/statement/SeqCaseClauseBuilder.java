// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock.Terminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.GhostVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionReturnPcRead;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.cpa.threading.GlobalAccessChecker;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqCaseClauseBuilder {

  public static ImmutableList<SeqCaseClause> buildCaseClauses(
      MPORThread pThread,
      ImmutableSet<MPORThread> pAllThreads,
      Set<ThreadNode> pCoveredNodes,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      GhostVariables pGhostVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqCaseClause> rCaseClauses = ImmutableList.builder();

    for (ThreadNode threadNode : pThread.cfa.threadNodes) {
      if (pCoveredNodes.add(threadNode)) {
        Optional<SeqCaseClause> caseClause =
            buildCaseClauseFromThreadNode(
                pThread,
                pAllThreads,
                pCoveredNodes,
                threadNode,
                pSubstituteEdges,
                pGhostVariables,
                pBinaryExpressionBuilder);
        if (caseClause.isPresent()) {
          rCaseClauses.add(caseClause.orElseThrow());
        }
      }
    }
    return rCaseClauses.build();
  }

  /**
   * Returns a {@link SeqCaseClause} which represents case statements in the sequentializations
   * while loop. Returns {@link Optional#empty()} if pThreadNode has no leaving edges i.e. its pc is
   * -1.
   */
  private static Optional<SeqCaseClause> buildCaseClauseFromThreadNode(
      final MPORThread pThread,
      final ImmutableSet<MPORThread> pAllThreads,
      Set<ThreadNode> pCoveredNodes,
      ThreadNode pThreadNode,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges,
      GhostVariables pGhostVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    pCoveredNodes.add(pThreadNode);

    int originPc = pThreadNode.pc;
    ImmutableList.Builder<SeqCaseBlockStatement> statements = ImmutableList.builder();

    CLeftHandSide pcLeftHandSide = pGhostVariables.pc.get(pThread.id);

    if (pThreadNode.leavingEdges().isEmpty()) {
      // no edges -> exit node of thread reached -> no case because no edges with code
      assert pThreadNode.pc == Sequentialization.EXIT_PC;
      return Optional.empty();

    } else if (pThreadNode.cfaNode instanceof FunctionExitNode) {
      // handle all CFunctionReturnEdges: exiting function -> pc not relevant, assign return pc
      assert pGhostVariables.function.returnPcReads.containsKey(pThreadNode);
      FunctionReturnPcRead read =
          Objects.requireNonNull(pGhostVariables.function.returnPcReads.get(pThreadNode));
      statements.add(
          SeqCaseBlockStatementBuilder.buildReturnPcReadStatement(
              pcLeftHandSide, read.returnPcVar));

    } else {
      statements.addAll(
          SeqCaseBlockStatementBuilder.buildStatementsFromThreadNode(
              pThread,
              pAllThreads,
              pThreadNode,
              pcLeftHandSide,
              pCoveredNodes,
              pSubEdges,
              pGhostVariables,
              pBinaryExpressionBuilder));
    }
    return Optional.of(
        new SeqCaseClause(
            anyGlobalAccess(pThreadNode.leavingEdges()),
            pThreadNode.cfaNode.isLoopStart(),
            originPc,
            new SeqCaseBlock(statements.build(), Terminator.CONTINUE)));
  }

  // Helpers =====================================================================================

  /**
   * Returns {@code true} if any {@link CFAEdge} of the given {@link ThreadEdge}s read or write a
   * global variable.
   */
  private static boolean anyGlobalAccess(List<ThreadEdge> pThreadEdges) {
    GlobalAccessChecker gac = new GlobalAccessChecker();
    for (ThreadEdge threadEdge : pThreadEdges) {
      if (!(threadEdge.cfaEdge instanceof CFunctionSummaryEdge)) {
        if (gac.hasGlobalAccess(threadEdge.cfaEdge)) {
          return true;
        }
      }
    }
    return false;
  }
}
