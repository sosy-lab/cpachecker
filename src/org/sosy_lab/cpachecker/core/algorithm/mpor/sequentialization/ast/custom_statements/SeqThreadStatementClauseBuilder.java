// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingMap;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_statements.SeqFunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.SeqGhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.SeqProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.AtomicBlockMerger;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.PartialOrderReducer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.StatementLinker;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.StatementPruner;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFANodeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.cwriter.export.CLabelStatement;

public record SeqThreadStatementClauseBuilder(
    MPOROptions options,
    ImmutableList<MPORThread> allThreads,
    ImmutableList<MPORSubstitution> substitutions,
    ImmutableMap<CFAEdgeForThread, SubstituteEdge> substituteEdges,
    ImmutableMap<MPORThread, SeqFunctionStatements> functionStatements,
    MachineModel machineModel,
    SeqPointerAliasingMap pointerAliasingMap,
    SeqGhostElements ghostElements,
    AstCfaRelation astCfaRelation,
    SequentializationUtils utils) {

  public ImmutableListMultimap<MPORThread, SeqThreadStatementClause> buildClauses()
      throws UnrecognizedCodeException {

    // initialize clauses from ThreadCFAs
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> initialClauses =
        initClausesForAllThreads();

    // if enabled, prune clauses so that no clause has only pc writes
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> prunedClauses =
        options.pruneEmptyStatements()
            ? StatementPruner.pruneClauses(options, initialClauses)
            : initialClauses;

    // ensure that atomic blocks are not interleaved by adding direct gotos
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> atomicBlocks =
        options.mergeAtomicBlocks() ? AtomicBlockMerger.merge(prunedClauses) : prunedClauses;

    // if enabled, link statements that are guaranteed to commute via gotos
    StatementLinker statementLinker = new StatementLinker(options, pointerAliasingMap);
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
        options.mergeCommutingStatements()
            ? statementLinker.linkClauses(atomicBlocks)
            : atomicBlocks;

    // if enabled, ensure that no backward goto exist. this should be done after all pc writes were
    // replaced with goto statements. in addition, the statements are possibly reordered, and it
    // should therefore be done before making labels consecutive.
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> noBackwardGoto =
        options.noBackwardGoto()
            ? SeqThreadStatementClauseUtil.removeBackwardGoto(options, linked)
            : linked;

    // ensure label numbers are consecutive (start at 0, end at clauseNum - 1). this must be done
    // before adding any instrumentation statements, otherwise the instrumentation statements may
    // have to be adjusted too, e.g., to adjust a 'goto' label.
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> consecutiveLabels =
        options.consecutiveLabels()
            ? SeqThreadStatementClauseUtil.cloneWithConsecutiveLabelNumbers(noBackwardGoto)
            : noBackwardGoto;

    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> finalClauses;
    // if the input program is concurrent, apply the partial order reduction options
    if (allThreads.size() > 1) {
      PartialOrderReducer partialOrderReducer =
          new PartialOrderReducer(
              options, consecutiveLabels, ghostElements, machineModel, pointerAliasingMap, utils);
      finalClauses = partialOrderReducer.reduceClauses();
    } else {
      finalClauses = consecutiveLabels;
    }

    // validate clauses based on pOptions
    SequentializationValidator.tryValidateClauses(options, finalClauses);
    return finalClauses;
  }

  /** Maps threads to the case clauses they potentially execute. */
  private ImmutableListMultimap<MPORThread, SeqThreadStatementClause> initClausesForAllThreads()
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rClauses =
        ImmutableListMultimap.builder();
    for (MPORSubstitution substitution : substitutions) {
      MPORThread thread = substitution.getThread();
      rClauses.putAll(thread, initClausesForSingleThread(thread, new HashSet<>()));
    }
    // only check pc validation, since clauses are not reordered at this point
    SequentializationValidator.tryValidateProgramCounters(options, rClauses.build());
    return reorderClauses(rClauses.build());
  }

  /**
   * Reorders the given {@link SeqThreadStatementClause}s so that the first non-blank is at the
   * start at label {@code 1}. This may not be given by default if a start_routine starts with a
   * function call.
   */
  private ImmutableListMultimap<MPORThread, SeqThreadStatementClause> reorderClauses(
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rReordered =
        ImmutableListMultimap.builder();
    for (MPORThread thread : pClauses.keySet()) {
      ImmutableList<SeqThreadStatementClause> clauses = pClauses.get(thread);
      ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses);
      SeqThreadStatementClause first = clauses.getFirst();
      SeqThreadStatementClause nonBlank =
          StatementPruner.recursivelyFindNonBlankClause(Optional.empty(), first, labelClauseMap);
      if (SeqThreadStatementClauseUtil.isConsecutiveLabelPath(first, nonBlank, labelClauseMap)) {
        rReordered.putAll(thread, clauses); // put case clauses as they were
      } else {
        ImmutableList.Builder<SeqThreadStatementClause> reordered = ImmutableList.builder();
        // add nonBlank, then add all other case clauses as they were
        reordered.add(nonBlank);
        reordered.addAll(
            clauses.stream()
                .filter(clause -> !clause.equals(nonBlank))
                .collect(ImmutableList.toImmutableList()));
        rReordered.putAll(thread, reordered.build());
      }
    }
    return rReordered.build();
  }

  /**
   * Builds the case clauses for the single thread {@code pThread}. Visits {@link CFANode}s only
   * once via {@code pVisitedNodes}.
   */
  private ImmutableList<SeqThreadStatementClause> initClausesForSingleThread(
      MPORThread pThread, Set<CFANodeForThread> pVisitedNodes) throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatementClause> rClauses = ImmutableList.builder();
    SeqThreadStatementBuilder statementBuilder =
        new SeqThreadStatementBuilder(
            options,
            pThread,
            allThreads,
            substituteEdges,
            pointerAliasingMap,
            Objects.requireNonNull(functionStatements.get(pThread)),
            ghostElements.threadSyncFlags(),
            ghostElements.programCounterVariables().getPcLeftHandSide(pThread.id()),
            ghostElements.programCounterVariables(),
            utils);
    for (CFANodeForThread threadNode : pThread.cfa().threadNodes) {
      if (pVisitedNodes.add(threadNode)) {
        rClauses.addAll(buildClausesFromThreadNode(pThread, threadNode, statementBuilder));
      }
    }
    return rClauses.build();
  }

  /**
   * Returns a {@link SeqThreadStatementClause} which represents case statements in the
   * sequentializations while loop. Returns {@link Optional#empty()} if pThreadNode has no leaving
   * edges i.e. its {@code pc} is {@link SeqProgramCounterVariables#EXIT_PC}.
   */
  private ImmutableList<SeqThreadStatementClause> buildClausesFromThreadNode(
      MPORThread pThread, CFANodeForThread pThreadNode, SeqThreadStatementBuilder pStatementBuilder)
      throws UnrecognizedCodeException {

    if (isExcludedNode(pThreadNode)) {
      return ImmutableList.of();
    }

    Optional<CLabelStatement> nextThreadLabel = ghostElements.tryGetNextThreadLabel(pThread);
    int labelPc = pThreadNode.pc;

    // some functions require multiple clauses built from a single edge
    if (!pThreadNode.leavingEdges().isEmpty()) {
      CFAEdgeForThread firstThreadEdge = pThreadNode.firstLeavingEdge();
      int targetPc = firstThreadEdge.getSuccessor().pc;

      if (PthreadUtil.isCallToPthreadFunction(
          firstThreadEdge.cfaEdge, PthreadFunctionType.PTHREAD_COND_WAIT)) {
        return buildCondWaitClauses(
            pThread,
            nextThreadLabel,
            pThreadNode,
            Objects.requireNonNull(substituteEdges.get(firstThreadEdge)),
            labelPc,
            targetPc,
            pStatementBuilder);
      }

      if (PthreadUtil.isCallToPthreadFunction(
          firstThreadEdge.cfaEdge, PthreadFunctionType.PTHREAD_BARRIER_WAIT)) {
        return buildBarrierWaitClauses(
            pThread,
            nextThreadLabel,
            pThreadNode,
            Objects.requireNonNull(substituteEdges.get(firstThreadEdge)),
            labelPc,
            targetPc,
            pStatementBuilder);
      }
    }

    ImmutableList<SeqThreadStatement> statements =
        pStatementBuilder.buildStatementsFromThreadNode(pThreadNode);
    return ImmutableList.of(
        buildClause(pThread, nextThreadLabel, pThreadNode, labelPc, statements));
  }

  private boolean isExcludedNode(CFANodeForThread pThreadNode) {
    // no leaving edges -> exit node of thread reached -> no clause because no edges with code
    if (pThreadNode.leavingEdges().isEmpty()) {
      assert pThreadNode.pc == SeqProgramCounterVariables.EXIT_PC
          : "A CFANodeForThread without any leaving edges must have EXIT_PC.";
      return true;
    }
    FluentIterable<CFAEdge> enteringEdges = pThreadNode.getCfaNode().getEnteringEdges();
    if (enteringEdges.size() == 1) {
      if (Iterables.getOnlyElement(enteringEdges) instanceof CFunctionSummaryStatementEdge) {
        return true;
      }
    }
    return false;
  }

  // Helpers =====================================================================================

  /**
   * Returns the clauses associated with {@link PthreadFunctionType#PTHREAD_BARRIER_WAIT}. This
   * function requires an interleaving between the increment of the variable storing the number of
   * waiters and the assumption that the count was reached, forcing us two create two {@link
   * SeqThreadStatement} from a single {@link CFAEdgeForThread}.
   */
  private ImmutableList<SeqThreadStatementClause> buildBarrierWaitClauses(
      MPORThread pThread,
      Optional<CLabelStatement> pNextThreadLabel,
      CFANodeForThread pThreadNode,
      SubstituteEdge pSubstituteEdge,
      int pLabelPc,
      int pTargetPc,
      SeqThreadStatementBuilder pStatementBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatementClause> rClauses = ImmutableList.builder();

    // step 1: increment the num_waiters variable
    int nextFreePc = pThread.cfa().getNextFreePc();
    SeqThreadStatement numWaitersIncrement =
        pStatementBuilder.buildBarrierStatement(
            SeqThreadStatementType.BARRIER_WAIT_INCREMENT, pSubstituteEdge, nextFreePc);
    rClauses.add(
        buildClause(
            pThread,
            pNextThreadLabel,
            pThreadNode,
            pLabelPc,
            ImmutableList.of(numWaitersIncrement)));

    // step 2: assume that num_waiters equals the count specified on barrier initialization
    SeqThreadStatement countAssumption =
        pStatementBuilder.buildBarrierStatement(
            SeqThreadStatementType.BARRIER_WAIT_ASSUMPTION, pSubstituteEdge, pTargetPc);
    rClauses.add(
        buildClause(
            pThread, pNextThreadLabel, pThreadNode, nextFreePc, ImmutableList.of(countAssumption)));

    return rClauses.build();
  }

  /**
   * Returns the clauses associated with {@link PthreadFunctionType#PTHREAD_COND_WAIT}. This
   * function requires an interleaving between the locking of the mutex and the blocking on the cond
   * variable, forcing us two create two {@link SeqThreadStatement} from a single {@link
   * CFAEdgeForThread}.
   */
  private ImmutableList<SeqThreadStatementClause> buildCondWaitClauses(
      MPORThread pThread,
      Optional<CLabelStatement> pNextThreadLabel,
      CFANodeForThread pThreadNode,
      SubstituteEdge pSubstituteEdge,
      int pLabelPc,
      int pTargetPc,
      SeqThreadStatementBuilder pStatementBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatementClause> rClauses = ImmutableList.builder();

    // step 1: reuse pthread_mutex_unlock statements for pthread_cond_wait
    int nextFreePc = pThread.cfa().getNextFreePc();
    SeqThreadStatement mutexUnlockStatement =
        pStatementBuilder.buildCondStatement(
            SeqThreadStatementType.COND_WAIT_UNLOCK, pSubstituteEdge, nextFreePc);
    rClauses.add(
        buildClause(
            pThread,
            pNextThreadLabel,
            pThreadNode,
            pLabelPc,
            ImmutableList.of(mutexUnlockStatement)));

    // step 2: build pthread_cond_t handling statement
    SeqThreadStatement condWaitStatement =
        pStatementBuilder.buildCondStatement(
            SeqThreadStatementType.COND_WAIT_LOCK, pSubstituteEdge, pTargetPc);
    rClauses.add(
        buildClause(
            pThread,
            pNextThreadLabel,
            pThreadNode,
            nextFreePc,
            ImmutableList.of(condWaitStatement)));

    return rClauses.build();
  }

  private SeqThreadStatementClause buildClause(
      MPORThread pThread,
      Optional<CLabelStatement> pNextThreadLabel,
      CFANodeForThread pThreadNode,
      int pLabelPc,
      ImmutableList<SeqThreadStatement> pStatements)
      throws UnrecognizedCodeException {

    ImmutableSet<CFANodeForThread> loopHeads = pThread.cfa().getLoopHeads();
    boolean isLoopHead =
        pStatements.stream()
            .allMatch(
                s ->
                    s.data().getSubstituteEdges().stream()
                        .allMatch(e -> loopHeads.contains(e.getThreadEdge().getPredecessor())));
    SeqThreadStatementBlock block =
        new SeqThreadStatementBlock(
            pThread.id(), pLabelPc, isLoopHead, pStatements, pNextThreadLabel);
    return new SeqThreadStatementClause(
        options,
        ImmutableList.of(
            block.withRequiredInstrumentation(
                substitutions.get(pThread.id()),
                substituteEdges,
                pThreadNode,
                pointerAliasingMap,
                astCfaRelation,
                utils.binaryExpressionBuilder())));
  }
}
