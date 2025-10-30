// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqThreadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqCondWaitStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqMutexUnlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.ThreadSyncFlags;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.AtomicBlockMerger;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.PartialOrderReducer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.pruning.SeqPruner;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFANodeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqThreadStatementClauseBuilder {

  public static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> buildClauses(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<CFAEdgeForThread, SubstituteEdge> pSubstituteEdges,
      Optional<MemoryModel> pMemoryModel,
      GhostElements pGhostElements,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    // initialize clauses from ThreadCFAs
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> initialClauses =
        initClauses(pOptions, pSubstitutions, pSubstituteEdges, pGhostElements, pUtils.logger());
    // if enabled, prune clauses so that no clause has only pc writes
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> prunedClauses =
        pOptions.pruneEmptyStatements()
            ? SeqPruner.pruneClauses(pOptions, initialClauses, pUtils.logger())
            : initialClauses;
    // ensure that atomic blocks are not interleaved by adding direct gotos
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> atomicBlocks =
        pOptions.atomicBlockMerge() ? AtomicBlockMerger.merge(prunedClauses) : prunedClauses;
    // if enabled, apply partial order reduction and reduce number of clauses
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> reducedClauses =
        PartialOrderReducer.reduce(
            pOptions, atomicBlocks, pGhostElements.bitVectorVariables(), pMemoryModel, pUtils);
    // if enabled, ensure that no backward goto exist
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> noBackwardGoto =
        pOptions.noBackwardGoto()
            ? SeqThreadStatementClauseUtil.removeBackwardGoto(reducedClauses, pUtils.logger())
            : reducedClauses;
    // ensure label numbers are consecutive (enforce start at 0, end at clauseNum - 1)
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> consecutiveLabels =
        pOptions.consecutiveLabels()
            ? SeqThreadStatementClauseUtil.cloneWithConsecutiveLabelNumbers(noBackwardGoto)
            : noBackwardGoto;
    // validate clauses based on pOptions
    SeqValidator.tryValidateClauses(pOptions, consecutiveLabels, pUtils.logger());
    return consecutiveLabels;
  }

  /** Maps threads to the case clauses they potentially execute. */
  private static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> initClauses(
      MPOROptions pOptions,
      ImmutableList<MPORSubstitution> pSubstitutions,
      ImmutableMap<CFAEdgeForThread, SubstituteEdge> pSubstituteEdges,
      GhostElements pGhostElements,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rClauses =
        ImmutableListMultimap.builder();
    ImmutableList<MPORThread> allThreads = SubstituteUtil.extractThreads(pSubstitutions);
    for (MPORSubstitution substitution : pSubstitutions) {
      MPORThread thread = substitution.thread;
      ImmutableList.Builder<SeqThreadStatementClause> clauses = ImmutableList.builder();
      Set<CFANodeForThread> coveredNodes = new HashSet<>();
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
    // only check pc validation, since clauses are not reordered at this point
    SeqValidator.tryValidateProgramCounters(pOptions, rClauses.build(), pLogger);
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
      Set<CFANodeForThread> pCoveredNodes,
      ImmutableMap<CFAEdgeForThread, SubstituteEdge> pSubstituteEdges,
      GhostElements pGhostElements) {

    ImmutableList.Builder<SeqThreadStatementClause> rClauses = ImmutableList.builder();

    for (CFANodeForThread threadNode : pThread.cfa().threadNodes) {
      if (pCoveredNodes.add(threadNode)) {
        rClauses.addAll(
            buildClausesFromThreadNode(
                pOptions,
                pThread,
                pNextThread,
                pAllThreads,
                pCoveredNodes,
                threadNode,
                pSubstituteEdges,
                pGhostElements));
      }
    }
    return rClauses.build();
  }

  /**
   * Returns a {@link SeqThreadStatementClause} which represents case statements in the
   * sequentializations while loop. Returns {@link Optional#empty()} if pThreadNode has no leaving
   * edges i.e. its pc is -1.
   */
  private static ImmutableList<SeqThreadStatementClause> buildClausesFromThreadNode(
      MPOROptions pOptions,
      MPORThread pThread,
      Optional<MPORThread> pNextThread,
      ImmutableList<MPORThread> pAllThreads,
      Set<CFANodeForThread> pCoveredNodes,
      CFANodeForThread pThreadNode,
      ImmutableMap<CFAEdgeForThread, SubstituteEdge> pSubstituteEdges,
      GhostElements pGhostElements) {

    pCoveredNodes.add(pThreadNode);

    // no edges -> exit node of thread reached -> no case because no edges with code
    if (pThreadNode.leavingEdges().isEmpty()) {
      assert pThreadNode.pc == Sequentialization.EXIT_PC;
      return ImmutableList.of();
    }

    int labelPc = pThreadNode.pc;
    ImmutableList.Builder<CSeqThreadStatement> statements = ImmutableList.builder();
    CLeftHandSide pcLeftHandSide = pGhostElements.getPcVariables().getPcLeftHandSide(pThread.id());
    CFAEdgeForThread firstThreadEdge = pThreadNode.firstLeavingEdge();
    int targetPc = firstThreadEdge.getSuccessor().pc;
    ImmutableList<SeqThreadStatementClause> multiClauseEdge =
        handleMultipleClauseEdge(
            pOptions,
            pThread,
            pNextThread,
            firstThreadEdge,
            pSubstituteEdges.get(firstThreadEdge),
            labelPc,
            targetPc,
            pGhostElements);

    // some edges require splitting into multiple clauses
    if (!multiClauseEdge.isEmpty()) {
      assert multiClauseEdge.size() > 1 : "multi clause list must have at least 2 elements";
      return multiClauseEdge;

    } else if (pThreadNode.cfaNode instanceof FunctionExitNode) {
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
    SeqThreadStatementClause clause =
        buildClause(
            pOptions,
            pThread,
            pNextThread,
            pGhostElements.threadLabels(),
            labelPc,
            statements.build());
    return ImmutableList.of(clause);
  }

  // Helpers =====================================================================================

  public static SeqBlockLabelStatement buildBlockLabelStatement(
      MPOROptions pOptions, int pThreadId, int pLabelNumber) {

    String threadPrefix = SeqNameUtil.buildThreadPrefix(pOptions, pThreadId);
    return new SeqBlockLabelStatement(threadPrefix, pLabelNumber);
  }

  private static Optional<MPORThread> tryGetNextThread(
      MPORThread pCurrentThread, ImmutableList<MPORThread> pAllThreads) {

    if (pCurrentThread.id() < pAllThreads.size() - 1) {
      return Optional.of(MPORThreadUtil.getThreadById(pAllThreads, pCurrentThread.id() + 1));
    }
    return Optional.empty();
  }

  private static ImmutableList<SeqThreadStatementClause> handleMultipleClauseEdge(
      MPOROptions pOptions,
      MPORThread pThread,
      Optional<MPORThread> pNextThread,
      CFAEdgeForThread pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pLabelPc,
      int pTargetPc,
      GhostElements pGhostElements) {

    if (PthreadUtil.isCallToPthreadFunction(
        pThreadEdge.cfaEdge, PthreadFunctionType.PTHREAD_COND_WAIT)) {

      return buildCondWaitClauses(
          pOptions,
          pThread,
          pNextThread,
          pThreadEdge,
          pSubstituteEdge,
          pLabelPc,
          pTargetPc,
          pGhostElements);
    }
    return ImmutableList.of();
  }

  /**
   * Returns the clauses associated with {@link PthreadFunctionType#PTHREAD_COND_WAIT}. This
   * function requires an interleaving between the locking of the mutex and the blocking on the cond
   * variable, forcing us two create two {@link CSeqThreadStatement} from a single {@link
   * CFAEdgeForThread}.
   */
  private static ImmutableList<SeqThreadStatementClause> buildCondWaitClauses(
      MPOROptions pOptions,
      MPORThread pThread,
      Optional<MPORThread> pNextThread,
      CFAEdgeForThread pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pLabelPc,
      int pTargetPc,
      GhostElements pGhostElements) {

    ImmutableList.Builder<SeqThreadStatementClause> rClauses = ImmutableList.builder();

    CLeftHandSide pcLeftHandSide = pGhostElements.getPcVariables().getPcLeftHandSide(pThread.id());
    ThreadSyncFlags threadSyncFlags = pGhostElements.threadSyncFlags();

    // step 1: reuse pthread_mutex_unlock statements for pthread_cond_wait
    int nextFreePc = pThread.cfa().getNextFreePc();
    SeqMutexUnlockStatement mutexUnlockStatement =
        SeqThreadStatementBuilder.buildMutexUnlockStatement(
            pOptions, pSubstituteEdge, nextFreePc, pcLeftHandSide, threadSyncFlags);
    rClauses.add(
        buildClause(
            pOptions,
            pThread,
            pNextThread,
            pGhostElements.threadLabels(),
            pLabelPc,
            ImmutableList.of(mutexUnlockStatement)));

    // step 2: build pthread_cond_t handling statement
    SeqCondWaitStatement condWaitStatement =
        SeqThreadStatementBuilder.buildCondWaitStatement(
            pOptions, pThreadEdge, pSubstituteEdge, pTargetPc, pcLeftHandSide, threadSyncFlags);
    rClauses.add(
        buildClause(
            pOptions,
            pThread,
            pNextThread,
            pGhostElements.threadLabels(),
            nextFreePc,
            ImmutableList.of(condWaitStatement)));

    return rClauses.build();
  }

  private static SeqThreadStatementClause buildClause(
      MPOROptions pOptions,
      MPORThread pThread,
      Optional<MPORThread> pNextThread,
      ImmutableMap<MPORThread, SeqThreadLabelStatement> pThreadLabels,
      int pLabelPc,
      ImmutableList<CSeqThreadStatement> pStatements) {

    SeqBlockLabelStatement blockLabelStatement =
        buildBlockLabelStatement(pOptions, pThread.id(), pLabelPc);
    SeqThreadStatementBlock block =
        new SeqThreadStatementBlock(
            pOptions, pNextThread, pThreadLabels, blockLabelStatement, pStatements);
    return new SeqThreadStatementClause(block);
  }
}
