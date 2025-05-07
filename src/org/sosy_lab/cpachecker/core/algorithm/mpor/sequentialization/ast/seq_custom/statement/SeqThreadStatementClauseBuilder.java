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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.GhostVariableUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.GhostVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.AtomicBlockBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.PartialOrderReducer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.pruning.SeqPruner;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.cpa.threading.GlobalAccessChecker;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadStatementClauseBuilder {

  public static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> buildCaseClauses(
      MPOROptions pOptions,
      ImmutableList.Builder<CIdExpression> pUpdatedVariables,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      Optional<BitVectorVariables> pBitVectorVariables,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    // initialize case clauses from ThreadCFAs
    ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> initialCaseClauses =
        initCaseClauses(
            pOptions, pSubstitutions, pSubstituteEdges, pPcVariables, pThreadSimulationVariables);
    // if enabled, prune case clauses so that no case clause has only pc writes
    ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> prunedCases =
        pOptions.pruneEmptyStatements
            ? SeqPruner.pruneCaseClauses(initialCaseClauses)
            : initialCaseClauses;
    // if enabled, apply partial order reduction and reduce number of cases
    ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> reducedCases =
        PartialOrderReducer.reduce(
            pOptions,
            pUpdatedVariables,
            pBitVectorVariables,
            prunedCases,
            pBinaryExpressionBuilder,
            pLogger);
    // ensure case labels are consecutive (enforce start at 0, end at casesNum - 1)
    ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> consecutiveLabelCases =
        pOptions.consecutiveLabels
            ? SeqThreadStatementClauseUtil.cloneWithConsecutiveLabels(reducedCases)
            : reducedCases;
    // ensure that atomic blocks are not interleaved by adding direct gotos
    ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> atomicBlocks =
        AtomicBlockBuilder.build(consecutiveLabelCases);
    // if enabled, ensure that all label and target pc are valid
    return pOptions.validatePc
        ? SeqValidator.validateCaseClauses(atomicBlocks, pLogger)
        : atomicBlocks;
  }

  /** Maps threads to the case clauses they potentially execute. */
  private static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> initCaseClauses(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rCaseClauses =
        ImmutableMap.builder();
    for (MPORSubstitution substitution : pSubstitutions) {
      MPORThread thread = substitution.thread;
      ImmutableList.Builder<SeqThreadStatementClause> clauses = ImmutableList.builder();
      Set<ThreadNode> coveredNodes = new HashSet<>();

      FunctionStatements functionVariables =
          GhostVariableUtil.buildFunctionVariables(thread, substitution, pSubstituteEdges);
      GhostVariables ghostVariables =
          new GhostVariables(functionVariables, pPcVariables, pThreadSimulationVariables);

      clauses.addAll(
          initCaseClauses(
              pOptions,
              thread,
              SubstituteUtil.extractThreads(pSubstitutions),
              coveredNodes,
              pSubstituteEdges,
              ghostVariables));
      rCaseClauses.put(thread, clauses.build());
    }
    // TODO add optional pc validation here
    return reorderClauses(rCaseClauses.buildOrThrow());
  }

  /**
   * Reorders the given {@link SeqThreadStatementClause}s so that the first non-blank is at the
   * start at label {@code 0}. This may not be given by default if a start_routine starts with a
   * function call.
   */
  private static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> reorderClauses(
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rReordered =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      ImmutableList<SeqThreadStatementClause> caseClauses = entry.getValue();
      ImmutableMap<Integer, SeqThreadStatementClause> labelCaseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(caseClauses);
      SeqThreadStatementClause first = caseClauses.get(0);
      SeqThreadStatementClause nonBlank = SeqPruner.findNonBlankCaseClause(first, labelCaseMap);
      if (SeqThreadStatementClauseUtil.isConsecutiveLabelPath(first, nonBlank, labelCaseMap)) {
        rReordered.put(entry); // put case clauses as they were
      } else {
        ImmutableList.Builder<SeqThreadStatementClause> reordered = ImmutableList.builder();
        // add nonBlank, then add all other case clauses as they were
        reordered.add(nonBlank);
        reordered.addAll(
            caseClauses.stream()
                .filter(c -> !c.equals(nonBlank))
                .collect(ImmutableList.toImmutableList()));
        rReordered.put(entry.getKey(), reordered.build());
      }
    }
    return rReordered.buildOrThrow();
  }

  /** Builds the case clauses for the single thread {@code pThread}. */
  private static ImmutableList<SeqThreadStatementClause> initCaseClauses(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableList<MPORThread> pAllThreads,
      Set<ThreadNode> pCoveredNodes,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      GhostVariables pGhostVariables) {

    ImmutableList.Builder<SeqThreadStatementClause> rCaseClauses = ImmutableList.builder();

    for (ThreadNode threadNode : pThread.cfa.threadNodes) {
      if (pCoveredNodes.add(threadNode)) {
        Optional<SeqThreadStatementClause> caseClause =
            buildCaseClauseFromThreadNode(
                pOptions,
                pThread,
                pAllThreads,
                pCoveredNodes,
                threadNode,
                pSubstituteEdges,
                pGhostVariables);
        if (caseClause.isPresent()) {
          rCaseClauses.add(caseClause.orElseThrow());
        }
      }
    }
    return injectThreadSimulationGhosts(rCaseClauses.build());
  }

  /**
   * Returns a {@link SeqThreadStatementClause} which represents case statements in the
   * sequentializations while loop. Returns {@link Optional#empty()} if pThreadNode has no leaving
   * edges i.e. its pc is -1.
   */
  private static Optional<SeqThreadStatementClause> buildCaseClauseFromThreadNode(
      MPOROptions pOptions,
      final MPORThread pThread,
      final ImmutableList<MPORThread> pAllThreads,
      Set<ThreadNode> pCoveredNodes,
      ThreadNode pThreadNode,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      GhostVariables pGhostVariables) {

    pCoveredNodes.add(pThreadNode);

    int labelPc = pThreadNode.pc;
    ImmutableList.Builder<SeqThreadStatement> statements = ImmutableList.builder();

    CLeftHandSide pcLeftHandSide = pGhostVariables.pc.get(pThread.id);

    ImmutableList<ThreadEdge> leavingEdges = pThreadNode.leavingEdges();
    if (leavingEdges.isEmpty()) {
      // no edges -> exit node of thread reached -> no case because no edges with code
      assert pThreadNode.pc == Sequentialization.EXIT_PC;
      return Optional.empty();

    } else if (pThreadNode.cfaNode instanceof FunctionExitNode) {
      int targetPc = pThreadNode.firstLeavingEdge().getSuccessor().pc;
      statements.add(SeqThreadStatementBuilder.buildBlankStatement(pcLeftHandSide, targetPc));

    } else {
      boolean isAssume = leavingEdges.get(0).cfaEdge instanceof CAssumeEdge;
      // TODO these assertions can be removed later, just checking if the CFA behaves as expected
      assert !isAssume || leavingEdges.size() == 2
          : "if there is an assume edge, the node must have exactly two assume edges";
      statements.addAll(
          SeqThreadStatementBuilder.buildStatementsFromThreadNode(
              pThread,
              pAllThreads,
              pThreadNode,
              pcLeftHandSide,
              pCoveredNodes,
              pSubstituteEdges,
              pGhostVariables));
    }
    SeqBlockGotoLabelStatement gotoLabel = buildBlockLabel(pOptions, pThread.id, labelPc);
    return Optional.of(
        new SeqThreadStatementClause(
            anyGlobalAccess(leavingEdges),
            pThreadNode.cfaNode.isLoopStart(),
            labelPc,
            new SeqThreadStatementBlock(gotoLabel, statements.build())));
  }

  private static ImmutableList<SeqThreadStatementClause> injectThreadSimulationGhosts(
      ImmutableList<SeqThreadStatementClause> pCaseClauses) {

    ImmutableList.Builder<SeqThreadStatementClause> rCaseClauses = ImmutableList.builder();
    ImmutableMap<Integer, SeqThreadStatementClause> labelValueMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pCaseClauses);

    for (SeqThreadStatementClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();

      for (SeqThreadStatement statement : caseClause.block.getStatements()) {
        ImmutableList.Builder<SeqInjectedStatement> injectedStatements = ImmutableList.builder();

        if (statement.getTargetPc().isPresent()) {
          int targetPc = statement.getTargetPc().orElseThrow();
          if (targetPc != Sequentialization.EXIT_PC) {
            SeqThreadStatementClause target = Objects.requireNonNull(labelValueMap.get(targetPc));
            // validation enforces that total strict order entries are direct targets (= first stmt)
            SeqThreadStatement firstStatement = target.block.getFirstStatement();
            Optional<SeqInjectedStatement> injectedStatement =
                SeqThreadStatementBuilder.tryBuildInjectedStatement(firstStatement);
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
      rCaseClauses.add(caseClause.cloneWithBlockStatements(newStatements.build()));
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

  public static SeqBlockGotoLabelStatement buildBlockLabel(
      MPOROptions pOptions, int pThreadId, int pLabelNumber) {
    return new SeqBlockGotoLabelStatement(
        SeqNameUtil.buildSwitchCaseGotoLabelPrefix(pOptions, pThreadId), pLabelNumber);
  }
}
