// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock.Terminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.GhostVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionReturnPcRead;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.cpa.threading.GlobalAccessChecker;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqUtil {

  public static final int INIT_PC = 0;

  public static final int EXIT_PC = -1;

  /**
   * Returns a {@link SeqCaseClause} which represents case statements in the sequentializations
   * while loop. Returns {@link Optional#empty()} if pThreadNode has no leaving edges i.e. its pc is
   * -1.
   */
  public static Optional<SeqCaseClause> buildCaseClauseFromThreadNode(
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

    // no edges -> exit node of thread reached -> no case because no edges with code
    if (pThreadNode.leavingEdges().isEmpty()) {
      assert pThreadNode.pc == EXIT_PC;
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
      boolean firstEdge = true;
      for (ThreadEdge threadEdge : pThreadNode.leavingEdges()) {
        if (isConstCpaCheckerTmpDeclaration(threadEdge.cfaEdge)) {
          // handle const CPAchecker_TMP first because it requires successor nodes and edges
          statements.add(
              SeqCaseBlockStatementBuilder.buildConstCpaCheckerTmpStatement(
                  threadEdge, pcLeftHandSide, pCoveredNodes, pSubEdges));
        } else {
          SubstituteEdge substitute = Objects.requireNonNull(pSubEdges.get(threadEdge));
          Optional<SeqCaseBlockStatement> statement =
              SeqCaseBlockStatementBuilder.tryBuildCaseBlockStatementFromEdge(
                  pThread,
                  pAllThreads,
                  firstEdge,
                  threadEdge,
                  substitute,
                  pGhostVariables,
                  pBinaryExpressionBuilder);
          if (statement.isPresent()) {
            statements.add(statement.orElseThrow());
          }
        }
        firstEdge = false;
      }
    }
    return Optional.of(
        new SeqCaseClause(
            anyGlobalAccess(pThreadNode.leavingEdges()),
            pThreadNode.cfaNode.isLoopStart(),
            originPc,
            new SeqCaseBlock(statements.build(), Terminator.CONTINUE)));
  }

  // Helpers =====================================================================================

  public static boolean isConstCpaCheckerTmp(CVariableDeclaration pVarDec) {
    return pVarDec.getType().isConst()
        && !pVarDec.isGlobal()
        && pVarDec.getName().contains(SeqToken.__CPAchecker_TMP_);
  }

  private static boolean isConstCpaCheckerTmpDeclaration(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof CDeclarationEdge declarationEdge) {
      if (declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
        return isConstCpaCheckerTmp(variableDeclaration);
      }
    }
    return false;
  }

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
