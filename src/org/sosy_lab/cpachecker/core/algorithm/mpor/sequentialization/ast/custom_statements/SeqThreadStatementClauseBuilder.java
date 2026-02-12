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
import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.AtomicBlockMerger;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.PartialOrderReducer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.StatementLinker;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.pruning.SeqPruner;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.validation.SeqValidator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFANodeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CLabelStatement;

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
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> initialClauses =
        initClausesForAllThreads();

    // if enabled, prune clauses so that no clause has only pc writes
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> prunedClauses =
        options.pruneEmptyStatements()
            ? SeqPruner.pruneClauses(options, initialClauses)
            : initialClauses;

    // ensure that atomic blocks are not interleaved by adding direct gotos
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> atomicBlocks =
        options.atomicBlockMerge() ? AtomicBlockMerger.merge(prunedClauses) : prunedClauses;

    // if enabled, link statements that are guaranteed to commute via gotos
    StatementLinker statementLinker = new StatementLinker(options, memoryModel);
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
        options.linkReduction() ? statementLinker.linkClauses(atomicBlocks) : atomicBlocks;

    // if enabled, ensure that no backward goto exist. this should be done after all pc writes were
    // replaced with goto statements. in addition, the statements are possibly reordered, and it
    // should therefore be done before making labels consecutive.
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> noBackwardGoto =
        options.noBackwardGoto()
            ? SeqThreadStatementClauseUtil.removeBackwardGoto(
                options.validateNoBackwardGoto(), linked)
            : linked;

    // ensure label numbers are consecutive (start at 0, end at clauseNum - 1). this must be done
    // before adding any injected statements, otherwise the injected statements may have to be
    // adjusted too, e.g., to adjust a 'goto' label in a partial order reduction instrumentation.
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> consecutiveLabels =
        options.consecutiveLabels()
            ? SeqThreadStatementClauseUtil.cloneWithConsecutiveLabelNumbers(noBackwardGoto)
            : noBackwardGoto;

    // if enabled, apply partial order reduction and reduce number of clauses
    PartialOrderReducer partialOrderReducer =
        new PartialOrderReducer(
            options, consecutiveLabels, ghostElements.bitVectorVariables(), memoryModel, utils);
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> reducedClauses =
        partialOrderReducer.reduceClauses();

    // validate clauses based on pOptions
    SeqValidator.tryValidateClauses(options, reducedClauses);
    return reducedClauses;
  }

  /** Maps threads to the case clauses they potentially execute. */
  private ImmutableListMultimap<MPORThread, SeqThreadStatementClause> initClausesForAllThreads()
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rClauses =
        ImmutableListMultimap.builder();
    for (MPORSubstitution substitution : substitutions) {
      MPORThread thread = substitution.thread;
      rClauses.putAll(thread, initClausesForSingleThread(thread, new HashSet<>()));
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
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses) {

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
      MPORThread pThread, Set<CFANodeForThread> pVisitedNodes) throws UnsupportedCodeException {

    ImmutableList.Builder<SeqThreadStatementClause> rClauses = ImmutableList.builder();
    SeqThreadStatementBuilder statementBuilder =
        new SeqThreadStatementBuilder(
            pThread,
            allThreads,
            substituteEdges,
            ghostElements.getFunctionStatementsByThread(pThread),
            ghostElements.threadSyncFlags(),
            ghostElements.getPcVariables().getPcLeftHandSide(pThread.id()),
            ghostElements.getPcVariables());
    for (CFANodeForThread threadNode : pThread.cfa().threadNodes) {
      if (pVisitedNodes.add(threadNode)) {
        rClauses.addAll(
            buildClausesFromThreadNode(pThread, pVisitedNodes, threadNode, statementBuilder));
      }
    }
    return rClauses.build();
  }

  /**
   * Returns a {@link SeqThreadStatementClause} which represents case statements in the
   * sequentializations while loop. Returns {@link Optional#empty()} if pThreadNode has no leaving
   * edges i.e. its {@code pc} is {@link ProgramCounterVariables#EXIT_PC}.
   */
  private ImmutableList<SeqThreadStatementClause> buildClausesFromThreadNode(
      MPORThread pThread,
      Set<CFANodeForThread> pCoveredNodes,
      CFANodeForThread pThreadNode,
      SeqThreadStatementBuilder pStatementBuilder)
      throws UnsupportedCodeException {

    pCoveredNodes.add(pThreadNode);

    if (isExcludedNode(pThreadNode)) {
      return ImmutableList.of();
    }

    Optional<CLabelStatement> nextThreadLabel = ghostElements.tryGetNextThreadLabel(pThread);
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
      ImmutableList.Builder<SeqThreadStatement> statements = ImmutableList.builder();
      if (pThreadNode.cfaNode instanceof FunctionExitNode) {
        statements.add(SeqThreadStatementBuilder.buildGhostOnlyStatement(pcLeftHandSide, targetPc));
      } else {
        statements.addAll(
            pStatementBuilder.buildStatementsFromThreadNode(pThreadNode, pCoveredNodes));
      }
      SeqThreadStatementClause clause =
          buildClause(pThread, nextThreadLabel, labelPc, statements.build());
      return ImmutableList.of(clause);
    }
  }

  private boolean isExcludedNode(CFANodeForThread pThreadNode) {
    // no leaving edges -> exit node of thread reached -> no clause because no edges with code
    if (pThreadNode.leavingEdges().isEmpty()) {
      assert pThreadNode.pc == ProgramCounterVariables.EXIT_PC
          : "A CFANodeForThread without any leaving edges must have EXIT_PC.";
      return true;
    }
    FluentIterable<CFAEdge> enteringEdges = pThreadNode.cfaNode.getEnteringEdges();
    if (enteringEdges.size() == 1) {
      if (Iterables.getOnlyElement(enteringEdges) instanceof CFunctionSummaryStatementEdge) {
        return true;
      }
    }
    return false;
  }

  // Helpers =====================================================================================

  private ImmutableList<SeqThreadStatementClause> handleMultipleClauseEdge(
      MPORThread pThread,
      Optional<CLabelStatement> pNextThreadLabel,
      CFAEdgeForThread pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pLabelPc,
      int pTargetPc,
      SeqThreadStatementBuilder pStatementBuilder)
      throws UnsupportedCodeException {

    Optional<CFunctionCall> optionalFunctionCall =
        PthreadUtil.tryGetFunctionCallFromCfaEdge(pThreadEdge.cfaEdge);
    if (optionalFunctionCall.isPresent()) {
      CFunctionCall functionCall = optionalFunctionCall.orElseThrow();
      if (PthreadUtil.isCallToPthreadFunction(
          functionCall, PthreadFunctionType.PTHREAD_COND_WAIT)) {
        return buildCondWaitClauses(
            pThread,
            pNextThreadLabel,
            functionCall,
            pSubstituteEdge,
            pLabelPc,
            pTargetPc,
            pStatementBuilder);
      }
    }
    return ImmutableList.of();
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
      CFunctionCall pFunctionCall,
      SubstituteEdge pSubstituteEdge,
      int pLabelPc,
      int pTargetPc,
      SeqThreadStatementBuilder pStatementBuilder)
      throws UnsupportedCodeException {

    ImmutableList.Builder<SeqThreadStatementClause> rClauses = ImmutableList.builder();

    // step 1: reuse pthread_mutex_unlock statements for pthread_cond_wait
    int nextFreePc = pThread.cfa().getNextFreePc();
    SeqThreadStatement mutexUnlockStatement =
        pStatementBuilder.buildMutexUnlockStatement(pFunctionCall, pSubstituteEdge, nextFreePc);
    rClauses.add(
        buildClause(pThread, pNextThreadLabel, pLabelPc, ImmutableList.of(mutexUnlockStatement)));

    // step 2: build pthread_cond_t handling statement
    SeqThreadStatement condWaitStatement =
        pStatementBuilder.buildCondWaitStatement(pFunctionCall, pSubstituteEdge, pTargetPc);
    rClauses.add(
        buildClause(pThread, pNextThreadLabel, nextFreePc, ImmutableList.of(condWaitStatement)));

    return rClauses.build();
  }

  private SeqThreadStatementClause buildClause(
      MPORThread pThread,
      Optional<CLabelStatement> pNextThreadLabel,
      int pLabelPc,
      ImmutableList<SeqThreadStatement> pStatements) {

    String threadPrefix = SeqNameUtil.buildThreadPrefix(options, pThread.id());
    SeqBlockLabelStatement blockLabelStatement = new SeqBlockLabelStatement(threadPrefix, pLabelPc);
    SeqThreadStatementBlock block =
        new SeqThreadStatementBlock(options, pNextThreadLabel, blockLabelStatement, pStatements);
    return new SeqThreadStatementClause(block);
  }
}
