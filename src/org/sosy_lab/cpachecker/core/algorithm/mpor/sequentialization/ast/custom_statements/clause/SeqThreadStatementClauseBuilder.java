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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqThreadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqCondWaitStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqMutexUnlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.AtomicBlockMerger;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.PartialOrderReducer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.pruning.SeqPruner;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFANodeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record SeqThreadStatementClauseBuilder(
    MPOROptions options,
    ImmutableList<MPORThread> allThreads,
    ImmutableList<MPORSubstitution> substitutions,
    ImmutableMap<CFAEdgeForThread, SubstituteEdge> substituteEdges,
    Optional<MemoryModel> memoryModel,
    GhostElements ghostElements,
    SequentializationUtils utils) {

  public ImmutableListMultimap<MPORThread, SeqThreadStatementClause> buildClauses()
      throws UnrecognizedCodeException {

    // initialize clauses from ThreadCFAs
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> initialClauses = initClauses();

    // if enabled, prune clauses so that no clause has only pc writes
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> prunedClauses =
        options.pruneEmptyStatements()
            ? SeqPruner.pruneClauses(options, initialClauses)
            : initialClauses;

    // ensure that atomic blocks are not interleaved by adding direct gotos
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> atomicBlocks =
        options.atomicBlockMerge() ? AtomicBlockMerger.merge(prunedClauses) : prunedClauses;

    // if enabled, apply partial order reduction and reduce number of clauses
    PartialOrderReducer partialOrderReducer =
        new PartialOrderReducer(
            options, atomicBlocks, ghostElements.bitVectorVariables(), memoryModel, utils);
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> reducedClauses =
        partialOrderReducer.reduce();

    // if enabled, ensure that no backward goto exist
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> noBackwardGoto =
        options.noBackwardGoto()
            ? SeqThreadStatementClauseUtil.removeBackwardGoto(reducedClauses)
            : reducedClauses;

    // ensure label numbers are consecutive (enforce start at 0, end at clauseNum - 1)
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> consecutiveLabels =
        options.consecutiveLabels()
            ? SeqThreadStatementClauseUtil.cloneWithConsecutiveLabelNumbers(noBackwardGoto)
            : noBackwardGoto;

    // validate clauses based on pOptions
    SeqValidator.tryValidateClauses(options, consecutiveLabels);
    return consecutiveLabels;
  }

  /** Maps threads to the case clauses they potentially execute. */
  private ImmutableListMultimap<MPORThread, SeqThreadStatementClause> initClauses()
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rClauses =
        ImmutableListMultimap.builder();
    for (MPORSubstitution substitution : substitutions) {
      MPORThread thread = substitution.thread;
      ImmutableList.Builder<SeqThreadStatementClause> clauses = ImmutableList.builder();
      Set<CFANodeForThread> coveredNodes = new HashSet<>();
      clauses.addAll(initClauses(thread, coveredNodes));
      rClauses.putAll(thread, clauses.build());
    }
    // only check pc validation, since clauses are not reordered at this point
    SeqValidator.tryValidateProgramCounters(options, rClauses.build());
    return reorderClauses(rClauses.build());
  }

  /**
   * Reorders the given {@link SeqThreadStatementClause}s so that the first non-blank is at the
   * start at label {@code 1}. This may not be given by default if a start_routine starts with a
   * function call.
   */
  private ImmutableListMultimap<MPORThread, SeqThreadStatementClause> reorderClauses(
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
  private ImmutableList<SeqThreadStatementClause> initClauses(
      MPORThread pThread, Set<CFANodeForThread> pCoveredNodes) {

    ImmutableList.Builder<SeqThreadStatementClause> rClauses = ImmutableList.builder();
    SeqThreadStatementBuilder statementBuilder =
        new SeqThreadStatementBuilder(
            options,
            pThread,
            allThreads,
            substituteEdges,
            ghostElements.getFunctionStatementsByThread(pThread),
            ghostElements.threadSyncFlags(),
            ghostElements.getPcVariables().getPcLeftHandSide(pThread.id()),
            ghostElements.getPcVariables());
    for (CFANodeForThread threadNode : pThread.cfa().threadNodes) {
      if (pCoveredNodes.add(threadNode)) {
        rClauses.addAll(
            buildClausesFromThreadNode(pThread, pCoveredNodes, threadNode, statementBuilder));
      }
    }
    return rClauses.build();
  }

  /**
   * Returns a {@link SeqThreadStatementClause} which represents case statements in the
   * sequentializations while loop. Returns {@link Optional#empty()} if pThreadNode has no leaving
   * edges i.e. its pc is -1.
   */
  private ImmutableList<SeqThreadStatementClause> buildClausesFromThreadNode(
      MPORThread pThread,
      Set<CFANodeForThread> pCoveredNodes,
      CFANodeForThread pThreadNode,
      SeqThreadStatementBuilder pStatementBuilder) {

    pCoveredNodes.add(pThreadNode);

    // no edges -> exit node of thread reached -> no case because no edges with code
    if (pThreadNode.leavingEdges().isEmpty()) {
      assert pThreadNode.pc == Sequentialization.EXIT_PC;
      return ImmutableList.of();
    }

    Optional<SeqThreadLabelStatement> nextThreadLabel = tryGetNextThreadLabel(pThread);
    CFAEdgeForThread firstThreadEdge = pThreadNode.firstLeavingEdge();
    int labelPc = pThreadNode.pc;
    int targetPc = firstThreadEdge.getSuccessor().pc;
    ImmutableList<SeqThreadStatementClause> multiClauseEdge =
        handleMultipleClauseEdge(
            pThread,
            nextThreadLabel,
            firstThreadEdge,
            Objects.requireNonNull(substituteEdges.get(firstThreadEdge)),
            labelPc,
            targetPc,
            pStatementBuilder);

    // some edges require splitting into multiple clauses
    if (!multiClauseEdge.isEmpty()) {
      assert multiClauseEdge.size() > 1 : "multi clause list must have at least 2 elements";
      return multiClauseEdge;

    } else {
      CLeftHandSide pcLeftHandSide = ghostElements.getPcVariables().getPcLeftHandSide(pThread.id());
      ImmutableList.Builder<CSeqThreadStatement> statements = ImmutableList.builder();
      if (pThreadNode.cfaNode instanceof FunctionExitNode) {
        statements.add(new SeqBlankStatement(options, pcLeftHandSide, targetPc));
      } else {
        statements.addAll(
            pStatementBuilder.buildStatementsFromThreadNode(pThreadNode, pCoveredNodes));
      }
      SeqThreadStatementClause clause =
          buildClause(pThread, nextThreadLabel, labelPc, statements.build());
      return ImmutableList.of(clause);
    }
  }

  // Helpers =====================================================================================

  private Optional<SeqThreadLabelStatement> tryGetNextThreadLabel(MPORThread pThread) {
    Optional<MPORThread> nextThread = tryGetNextThread(pThread, allThreads);
    return nextThread.isPresent()
        ? Optional.ofNullable(ghostElements.threadLabels().get(nextThread.orElseThrow()))
        : Optional.empty();
  }

  private SeqBlockLabelStatement buildBlockLabelStatement(int pThreadId, int pLabelNumber) {
    String threadPrefix = SeqNameUtil.buildThreadPrefix(options, pThreadId);
    return new SeqBlockLabelStatement(threadPrefix, pLabelNumber);
  }

  private static Optional<MPORThread> tryGetNextThread(
      MPORThread pCurrentThread, ImmutableList<MPORThread> pAllThreads) {

    if (pCurrentThread.id() < pAllThreads.size() - 1) {
      return Optional.of(MPORThreadUtil.getThreadById(pAllThreads, pCurrentThread.id() + 1));
    }
    return Optional.empty();
  }

  private ImmutableList<SeqThreadStatementClause> handleMultipleClauseEdge(
      MPORThread pThread,
      Optional<SeqThreadLabelStatement> pNextThreadLabel,
      CFAEdgeForThread pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pLabelPc,
      int pTargetPc,
      SeqThreadStatementBuilder pStatementBuilder) {

    return PthreadUtil.tryGetFunctionCallFromCfaEdge(pThreadEdge.cfaEdge)
        .filter(
            functionCall ->
                PthreadUtil.isCallToPthreadFunction(
                    functionCall, PthreadFunctionType.PTHREAD_COND_WAIT))
        .map(
            functionCall ->
                buildCondWaitClauses(
                    pThread,
                    pNextThreadLabel,
                    functionCall,
                    pSubstituteEdge,
                    pLabelPc,
                    pTargetPc,
                    pStatementBuilder))
        .orElse(ImmutableList.of());
  }

  /**
   * Returns the clauses associated with {@link PthreadFunctionType#PTHREAD_COND_WAIT}. This
   * function requires an interleaving between the locking of the mutex and the blocking on the cond
   * variable, forcing us two create two {@link CSeqThreadStatement} from a single {@link
   * CFAEdgeForThread}.
   */
  private ImmutableList<SeqThreadStatementClause> buildCondWaitClauses(
      MPORThread pThread,
      Optional<SeqThreadLabelStatement> pNextThreadLabel,
      CFunctionCall pFunctionCall,
      SubstituteEdge pSubstituteEdge,
      int pLabelPc,
      int pTargetPc,
      SeqThreadStatementBuilder pStatementBuilder) {

    ImmutableList.Builder<SeqThreadStatementClause> rClauses = ImmutableList.builder();

    // step 1: reuse pthread_mutex_unlock statements for pthread_cond_wait
    int nextFreePc = pThread.cfa().getNextFreePc();
    SeqMutexUnlockStatement mutexUnlockStatement =
        pStatementBuilder.buildMutexUnlockStatement(pFunctionCall, pSubstituteEdge, nextFreePc);
    rClauses.add(
        buildClause(pThread, pNextThreadLabel, pLabelPc, ImmutableList.of(mutexUnlockStatement)));

    // step 2: build pthread_cond_t handling statement
    SeqCondWaitStatement condWaitStatement =
        pStatementBuilder.buildCondWaitStatement(pFunctionCall, pSubstituteEdge, pTargetPc);
    rClauses.add(
        buildClause(pThread, pNextThreadLabel, nextFreePc, ImmutableList.of(condWaitStatement)));

    return rClauses.build();
  }

  private SeqThreadStatementClause buildClause(
      MPORThread pThread,
      Optional<SeqThreadLabelStatement> pNextThreadLabel,
      int pLabelPc,
      ImmutableList<CSeqThreadStatement> pStatements) {

    SeqBlockLabelStatement blockLabelStatement = buildBlockLabelStatement(pThread.id(), pLabelPc);
    SeqThreadStatementBlock block =
        new SeqThreadStatementBlock(options, pNextThreadLabel, blockLabelStatement, pStatements);
    return new SeqThreadStatementClause(block);
  }
}
