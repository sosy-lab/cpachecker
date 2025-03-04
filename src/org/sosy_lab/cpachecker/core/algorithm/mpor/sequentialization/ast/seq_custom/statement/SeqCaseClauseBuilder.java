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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock.Terminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.GhostVariableUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.GhostVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.function_statements.FunctionReturnPcRead;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.PartialOrderReducer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.pruning.SeqPruner;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.cpa.threading.GlobalAccessChecker;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqCaseClauseBuilder {

  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> buildCaseClauses(
      MPOROptions pOptions,
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>>
          pReturnPcVariables,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    // initialize case clauses from ThreadCFAs
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> initialCaseClauses =
        initCaseClauses(
            pSubstitutions,
            pSubstituteEdges,
            pReturnPcVariables,
            pPcVariables,
            pThreadSimulationVariables,
            pBinaryExpressionBuilder);
    // prune case clauses so that no case clause has only pc writes
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> prunedCaseClauses =
        SeqPruner.pruneCaseClauses(initialCaseClauses);
    // update initial case labels to INIT_PC
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> updatedInitialLabelCaseClauses =
        updateInitialLabels(prunedCaseClauses);
    // if enabled, apply partial order reduction by concatenating case clauses
    if (pOptions.partialOrderReduction) {
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> concatenatedClauses =
          PartialOrderReducer.concatenateCommutingClauses(updatedInitialLabelCaseClauses);
      return SeqValidator.validateCaseClauses(concatenatedClauses, pLogger);
    } else {
      return SeqValidator.validateCaseClauses(updatedInitialLabelCaseClauses, pLogger);
    }
  }

  /** Maps threads to the case clauses they potentially execute. */
  private static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> initCaseClauses(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      ImmutableMap<MPORThread, ImmutableMap<CFunctionDeclaration, CIdExpression>>
          pReturnPcVariables,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rCaseClauses =
        ImmutableMap.builder();
    for (var entry : pSubstitutions.entrySet()) {
      MPORThread thread = entry.getKey();
      CSimpleDeclarationSubstitution substitution = entry.getValue();
      ImmutableList.Builder<SeqCaseClause> caseClauses = ImmutableList.builder();
      Set<ThreadNode> coveredNodes = new HashSet<>();

      FunctionStatements functionVariables =
          GhostVariableUtil.buildFunctionVariables(
              thread, substitution, pSubstituteEdges, pReturnPcVariables);
      GhostVariables ghostVariables =
          new GhostVariables(functionVariables, pPcVariables, pThreadSimulationVariables);

      caseClauses.addAll(
          initCaseClauses(
              thread,
              pSubstitutions.keySet(),
              coveredNodes,
              pSubstituteEdges,
              ghostVariables,
              pBinaryExpressionBuilder));
      rCaseClauses.put(thread, caseClauses.build());
    }
    // modified reach_error result in unreachable statements of that function
    //  -> no validation of case clauses here
    return rCaseClauses.buildOrThrow();
  }

  /** Builds the case clauses for the single thread {@code pThread}. */
  private static ImmutableList<SeqCaseClause> initCaseClauses(
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

    int labelPc = pThreadNode.pc;
    ImmutableList.Builder<SeqCaseBlockStatement> statements = ImmutableList.builder();

    CLeftHandSide pcLeftHandSide = pGhostVariables.pc.get(pThread.id);

    List<ThreadEdge> leavingEdges = pThreadNode.leavingEdges();
    if (leavingEdges.isEmpty()) {
      // no edges -> exit node of thread reached -> no case because no edges with code
      assert pThreadNode.pc == Sequentialization.EXIT_PC;
      return Optional.empty();

    } else if (pThreadNode.cfaNode instanceof FunctionExitNode) {
      if (pGhostVariables.function.returnPcReads.containsKey(pThreadNode)) {
        // if there is a returnPcRead (pc = RETURN_PC) then the function is called numerous times
        FunctionReturnPcRead read =
            Objects.requireNonNull(pGhostVariables.function.returnPcReads.get(pThreadNode));
        statements.add(
            SeqCaseBlockStatementBuilder.buildReturnPcReadStatement(
                pcLeftHandSide, read.returnPcVariable));
      } else {
        // if there is no returnPcRead, then the function is called only once -> use target pc
        assert pThreadNode.leavingEdges().size() == 1;
        int targetPc = pThreadNode.leavingEdges().get(0).getSuccessor().pc;
        statements.add(SeqCaseBlockStatementBuilder.buildBlankStatement(pcLeftHandSide, targetPc));
      }

    } else {
      boolean isAssume = leavingEdges.get(0).cfaEdge instanceof CAssumeEdge;
      // TODO these assertions can be removed later, just checking if the CFA behaves as expected
      assert !isAssume || leavingEdges.size() == 2
          : "if there is an assume edge, the node must have exactly two assume edges";
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
            anyGlobalAccess(leavingEdges),
            pThreadNode.cfaNode.isLoopStart(),
            labelPc,
            new SeqCaseBlock(statements.build(), Terminator.CONTINUE)));
  }

  /**
   * Ensures that the initial label {@code pc} for all threads is {@link Sequentialization#INIT_PC}.
   */
  private static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> updateInitialLabels(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rUpdated =
        ImmutableMap.builder();
    boolean singleCaseClause = pCaseClauses.size() == 1;
    for (var entry : pCaseClauses.entrySet()) {
      boolean firstCase = true;
      ImmutableList.Builder<SeqCaseClause> updatedCases = ImmutableList.builder();
      // this approach (just taking the first case) is sound because the path up to the first
      //  non-blank case is deterministic (i.e. only 1 leaving edge)
      for (SeqCaseClause caseClause : entry.getValue()) {
        assert !singleCaseClause || !caseClause.onlyWritesPc()
            : "case clause is still prunable. did you use the pruned case clauses?";
        if (firstCase) {
          updatedCases.add(caseClause.cloneWithLabel(new SeqCaseLabel(Sequentialization.INIT_PC)));
          firstCase = false;
        } else {
          updatedCases.add(caseClause);
        }
      }
      rUpdated.put(entry.getKey(), updatedCases.build());
    }
    return rUpdated.buildOrThrow();
  }

  // Helpers =====================================================================================

  /**
   * Returns {@code true} if any {@link CFAEdge} of the given {@link ThreadEdge}s read or write a
   * global variable.
   */
  private static boolean anyGlobalAccess(List<ThreadEdge> pThreadEdges) {
    // TODO refactor global access checker so that CParameterDeclarations that are pointers
    //  are considered global (or create 3 verdicts TRUE, FALSE, POSSIBLY)
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
