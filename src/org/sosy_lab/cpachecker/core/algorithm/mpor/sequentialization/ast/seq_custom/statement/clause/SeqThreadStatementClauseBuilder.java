// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.AtomicBlockMerger;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.PartialOrderReducer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.pruning.SeqPruner;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadStatementClauseBuilder {

  public static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> buildClauses(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      Optional<MemoryModel> pMemoryModel,
      GhostElements pGhostElements,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    // initialize clauses from ThreadCFAs
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> initialClauses =
        initClauses(pOptions, pSubstitutions, pSubstituteEdges, pGhostElements);
    // if enabled, prune clauses so that no clause has only pc writes
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> prunedClauses =
        pOptions.pruneEmptyStatements ? SeqPruner.pruneClauses(initialClauses) : initialClauses;
    // ensure that atomic blocks are not interleaved by adding direct gotos
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> atomicBlocks =
        pOptions.atomicBlockMerge ? AtomicBlockMerger.merge(prunedClauses) : prunedClauses;
    // if enabled, apply partial order reduction and reduce number of clauses
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> reducedClauses =
        PartialOrderReducer.reduce(
            pOptions,
            atomicBlocks,
            pGhostElements.getBitVectorVariables(),
            pMemoryModel,
            pBinaryExpressionBuilder,
            pLogger);
    // if enabled, ensure that no backward goto exist
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> noBackwardGoto =
        pOptions.noBackwardGoto
            ? SeqThreadStatementClauseUtil.removeBackwardGoto(reducedClauses, pLogger)
            : reducedClauses;
    // ensure label numbers are consecutive (enforce start at 0, end at clauseNum - 1)
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> consecutiveLabels =
        pOptions.consecutiveLabels
            ? SeqThreadStatementClauseUtil.cloneWithConsecutiveLabelNumbers(noBackwardGoto)
            : noBackwardGoto;
    // if enabled, ensure that all label and target pc are valid
    return pOptions.validatePc
        ? SeqValidator.validateClauses(consecutiveLabels, pLogger)
        : consecutiveLabels;
  }

  /** Maps threads to the case clauses they potentially execute. */
  private static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> initClauses(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      GhostElements pGhostElements)
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rClauses =
        ImmutableListMultimap.builder();
    ImmutableList<MPORThread> allThreads = SubstituteUtil.extractThreads(pSubstitutions);
    for (MPORSubstitution substitution : pSubstitutions) {
      MPORThread thread = substitution.thread;
      ImmutableList.Builder<SeqThreadStatementClause> clauses = ImmutableList.builder();
      Set<ThreadNode> coveredNodes = new HashSet<>();
      clauses.addAll(
          initClauses(
              pOptions,
              thread,
              tryGetNextThread(thread, allThreads),
              allThreads,
              coveredNodes,
              pSubstituteEdges,
              pGhostElements));
      rClauses.putAll(thread, clauses.build());
    }
    // TODO add optional pc validation here
    return reorderClauses(rClauses.build());
  }

  /**
   * Reorders the given {@link SeqThreadStatementClause}s so that the first non-blank is at the
   * start at label {@code 1}. This may not be given by default if a start_routine starts with a
   * function call.
   */
  private static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> reorderClauses(
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses)
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rReordered =
        ImmutableListMultimap.builder();
    for (MPORThread thread : pClauses.keySet()) {
      ImmutableList<SeqThreadStatementClause> clauses = pClauses.get(thread);
      ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses);
      SeqThreadStatementClause first = clauses.getFirst();
      SeqThreadStatementClause nonBlank =
          SeqPruner.recursivelyFindNonBlankClause(Optional.empty(), first, labelClauseMap);
      if (SeqThreadStatementClauseUtil.isConsecutiveLabelPath(first, nonBlank, labelClauseMap)) {
        rReordered.putAll(thread, clauses); // put case clauses as they were
      } else {
        ImmutableList.Builder<SeqThreadStatementClause> reordered = ImmutableList.builder();
        // add nonBlank, then add all other case clauses as they were
        reordered.add(nonBlank);
        reordered.addAll(
            clauses.stream()
                .filter(c -> !c.equals(nonBlank))
                .collect(ImmutableList.toImmutableList()));
        rReordered.putAll(thread, reordered.build());
      }
    }
    return rReordered.build();
  }

  /** Builds the case clauses for the single thread {@code pThread}. */
  private static ImmutableList<SeqThreadStatementClause> initClauses(
      MPOROptions pOptions,
      MPORThread pThread,
      Optional<MPORThread> pNextThread,
      ImmutableList<MPORThread> pAllThreads,
      Set<ThreadNode> pCoveredNodes,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      GhostElements pGhostElements) {

    ImmutableList.Builder<SeqThreadStatementClause> rClauses = ImmutableList.builder();

    for (ThreadNode threadNode : pThread.cfa.threadNodes) {
      if (pCoveredNodes.add(threadNode)) {
        Optional<SeqThreadStatementClause> clause =
            buildClauseFromThreadNode(
                pOptions,
                pThread,
                pNextThread,
                pAllThreads,
                pCoveredNodes,
                threadNode,
                pSubstituteEdges,
                pGhostElements);
        if (clause.isPresent()) {
          rClauses.add(clause.orElseThrow());
        }
      }
    }
    return rClauses.build();
  }

  /**
   * Returns a {@link SeqThreadStatementClause} which represents case statements in the
   * sequentializations while loop. Returns {@link Optional#empty()} if pThreadNode has no leaving
   * edges i.e. its pc is -1.
   */
  private static Optional<SeqThreadStatementClause> buildClauseFromThreadNode(
      MPOROptions pOptions,
      final MPORThread pThread,
      final Optional<MPORThread> pNextThread,
      final ImmutableList<MPORThread> pAllThreads,
      Set<ThreadNode> pCoveredNodes,
      ThreadNode pThreadNode,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      GhostElements pGhostElements) {

    pCoveredNodes.add(pThreadNode);

    int labelPc = pThreadNode.pc;
    ImmutableList.Builder<SeqThreadStatement> statements = ImmutableList.builder();

    CLeftHandSide pcLeftHandSide =
        pGhostElements.getPcVariables().getPcLeftHandSide(pThread.getId());

    ImmutableList<ThreadEdge> leavingEdges = pThreadNode.leavingEdges();
    if (leavingEdges.isEmpty()) {
      // no edges -> exit node of thread reached -> no case because no edges with code
      assert pThreadNode.pc == Sequentialization.EXIT_PC;
      return Optional.empty();

    } else if (pThreadNode.cfaNode instanceof FunctionExitNode) {
      int targetPc = pThreadNode.firstLeavingEdge().getSuccessor().pc;
      statements.add(
          SeqThreadStatementBuilder.buildBlankStatement(pOptions, pcLeftHandSide, targetPc));

    } else {
      statements.addAll(
          SeqThreadStatementBuilder.buildStatementsFromThreadNode(
              pOptions,
              pThread,
              pAllThreads,
              pThreadNode,
              pcLeftHandSide,
              pCoveredNodes,
              pSubstituteEdges,
              pGhostElements));
    }
    SeqBlockLabelStatement blockLabelStatement =
        buildBlockLabelStatement(pOptions, pThread.getId(), labelPc);
    SeqThreadStatementBlock block =
        new SeqThreadStatementBlock(pOptions, pNextThread, blockLabelStatement, statements.build());
    SeqThreadStatementClause clause = new SeqThreadStatementClause(block);
    return Optional.of(clause);
  }

  // Helpers =====================================================================================

  public static SeqBlockLabelStatement buildBlockLabelStatement(
      MPOROptions pOptions, int pThreadId, int pLabelNumber) {

    String threadPrefix = SeqNameUtil.buildThreadPrefix(pOptions, pThreadId);
    return new SeqBlockLabelStatement(threadPrefix, pLabelNumber);
  }

  private static Optional<MPORThread> tryGetNextThread(
      MPORThread pCurrentThread, ImmutableList<MPORThread> pAllThreads) {

    if (pCurrentThread.getId() < pAllThreads.size() - 1) {
      return Optional.of(ThreadUtil.getThreadById(pAllThreads, pCurrentThread.getId() + 1));
    }
    return Optional.empty();
  }
}
