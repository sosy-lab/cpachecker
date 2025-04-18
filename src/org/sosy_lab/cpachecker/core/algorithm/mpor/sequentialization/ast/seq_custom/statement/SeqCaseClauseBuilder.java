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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.GhostVariableUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.GhostVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.PartialOrderReducer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.pruning.SeqPruner;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.cpa.threading.GlobalAccessChecker;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqCaseClauseBuilder {

  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> buildCaseClauses(
      MPOROptions pOptions,
      ImmutableList.Builder<CIdExpression> pUpdatedVariables,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      ImmutableSet<CVariableDeclaration> pAllGlobalVariables,
      BitVectorVariables pBitVectorVariables,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    // initialize case clauses from ThreadCFAs
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> initialCaseClauses =
        initCaseClauses(pSubstitutions, pSubstituteEdges, pPcVariables, pThreadSimulationVariables);
    // prune case clauses so that no case clause has only pc writes
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> prunedCases =
        SeqPruner.pruneCaseClauses(initialCaseClauses);
    // if enabled, apply partial order reduction and reduce number of cases
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> reducedCases =
        PartialOrderReducer.reduce(
            pOptions,
            pUpdatedVariables,
            pAllGlobalVariables,
            pBitVectorVariables,
            prunedCases,
            pBinaryExpressionBuilder);
    // ensure case labels are consecutive (enforce start at 0, end at casesNum - 1)
    ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> consecutiveLabelCases =
        SeqCaseClauseUtil.cloneWithConsecutiveLabels(reducedCases);
    // if enabled, ensure that all label and target pc are valid
    if (pOptions.validatePc) {
      return SeqValidator.validateCaseClauses(consecutiveLabelCases, pLogger);
    } else {
      return consecutiveLabelCases;
    }
  }

  /** Maps threads to the case clauses they potentially execute. */
  private static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> initCaseClauses(
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rCaseClauses =
        ImmutableMap.builder();
    for (MPORSubstitution substitution : pSubstitutions) {
      MPORThread thread = substitution.thread;
      ImmutableList.Builder<SeqCaseClause> caseClauses = ImmutableList.builder();
      Set<ThreadNode> coveredNodes = new HashSet<>();

      FunctionStatements functionVariables =
          GhostVariableUtil.buildFunctionVariables(thread, substitution, pSubstituteEdges);
      GhostVariables ghostVariables =
          new GhostVariables(functionVariables, pPcVariables, pThreadSimulationVariables);

      caseClauses.addAll(
          initCaseClauses(
              thread,
              SubstituteUtil.extractThreads(pSubstitutions),
              coveredNodes,
              pSubstituteEdges,
              ghostVariables));
      rCaseClauses.put(thread, caseClauses.build());
    }
    // TODO add optional pc validation here
    return rCaseClauses.buildOrThrow();
  }

  /** Builds the case clauses for the single thread {@code pThread}. */
  private static ImmutableList<SeqCaseClause> initCaseClauses(
      MPORThread pThread,
      ImmutableSet<MPORThread> pAllThreads,
      Set<ThreadNode> pCoveredNodes,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      GhostVariables pGhostVariables) {

    ImmutableList.Builder<SeqCaseClause> rCaseClauses = ImmutableList.builder();

    for (ThreadNode threadNode : pThread.cfa.threadNodes) {
      if (pCoveredNodes.add(threadNode)) {
        Optional<SeqCaseClause> caseClause =
            buildCaseClauseFromThreadNode(
                pThread, pAllThreads, pCoveredNodes, threadNode, pSubstituteEdges, pGhostVariables);
        if (caseClause.isPresent()) {
          rCaseClauses.add(caseClause.orElseThrow());
        }
      }
    }
    return injectThreadSimulationGhosts(rCaseClauses.build());
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
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      GhostVariables pGhostVariables) {

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
      int targetPc = pThreadNode.leavingEdges().get(0).getSuccessor().pc;
      statements.add(SeqCaseBlockStatementBuilder.buildBlankStatement(pcLeftHandSide, targetPc));

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
              pSubstituteEdges,
              pGhostVariables));
    }
    return Optional.of(
        new SeqCaseClause(
            anyGlobalAccess(leavingEdges),
            pThreadNode.cfaNode.isLoopStart(),
            labelPc,
            new SeqCaseBlock(statements.build())));
  }

  private static ImmutableList<SeqCaseClause> injectThreadSimulationGhosts(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableList.Builder<SeqCaseClause> rCaseClauses = ImmutableList.builder();
    ImmutableMap<Integer, SeqCaseClause> labelValueMap =
        SeqCaseClauseUtil.mapCaseLabelValueToCaseClause(pCaseClauses);

    for (SeqCaseClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();

      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        ImmutableList.Builder<SeqInjectedStatement> injectedStatements = ImmutableList.builder();

        if (statement.getTargetPc().isPresent()) {
          int targetPc = statement.getTargetPc().orElseThrow();
          if (targetPc != Sequentialization.EXIT_PC) {
            SeqCaseClause target = Objects.requireNonNull(labelValueMap.get(targetPc));
            // validation enforces that total strict order entries are direct targets (= first stmt)
            SeqCaseBlockStatement firstStatement = target.block.statements.get(0);
            Optional<SeqInjectedStatement> injectedStatement =
                SeqCaseBlockStatementBuilder.tryBuildInjectedStatement(firstStatement);
            if (injectedStatement.isPresent()) {
              injectedStatements.add(injectedStatement.orElseThrow());
            }
          }
        }
        ImmutableList<SeqInjectedStatement> injected = injectedStatements.build();
        if (injected.isEmpty()) {
          // no injections -> add all previous statements (we only inject, no replacing)
          newStatements.add(statement);
        } else {
          // injections -> add cloned statement with injections
          newStatements.add(statement.cloneWithInjectedStatements(injected));
        }
      }
      SeqCaseBlock newBlock = new SeqCaseBlock(newStatements.build());
      rCaseClauses.add(caseClause.cloneWithBlock(newBlock));
    }
    return rCaseClauses.build();
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
