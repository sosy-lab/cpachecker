// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public record MPORThreadBuilder(MPOROptions options, CFA cfa) {

  @VisibleForTesting public static final int MAIN_THREAD_ID = 0;

  private static int currentThreadId = MAIN_THREAD_ID;

  public static void resetThreadId() {
    currentThreadId = MAIN_THREAD_ID;
  }

  /** Track the currentPc, static so that it is consistent across recursive function calls. */
  private static int currentPc = ProgramCounterVariables.INIT_PC;

  public static void resetPc() {
    currentPc = ProgramCounterVariables.INIT_PC;
  }

  /**
   * Extracts all threads (main and pthreads) and the FunctionEntry / ExitNodes of their start
   * routines from the given CFA.
   *
   * <p>This functions needs to be called after functionCallMap was initialized so that we can track
   * the calling context of each thread.
   */
  public ImmutableList<MPORThread> extractThreadsFromCfa() throws UnsupportedCodeException {
    ImmutableList.Builder<MPORThread> rThreads = ImmutableList.builder();
    // add the main thread
    FunctionEntryNode mainEntryNode = cfa.getMainFunction();
    MPORThread mainThread = extractThreadFromCfa(Optional.empty(), Optional.empty(), mainEntryNode);
    rThreads.add(mainThread);
    // recursively search the thread CFAs for pthread_create calls, and store in rThreads
    List<MPORThread> createdThreads = new ArrayList<>();
    recursivelyFindThreadCreations(mainThread, createdThreads);
    // sort threads by ID, otherwise the program could have backward jumps
    createdThreads.sort(Comparator.comparingInt(MPORThread::id));
    rThreads.addAll(createdThreads);
    return rThreads.build();
  }

  private void recursivelyFindThreadCreations(
      MPORThread pCurrentThread, List<MPORThread> pFoundThreads) throws UnsupportedCodeException {

    for (CFAEdgeForThread threadEdge : pCurrentThread.cfa().threadEdges) {
      CFAEdge cfaEdge = threadEdge.cfaEdge;
      Optional<CFunctionCall> optionalFunctionCall =
          PthreadUtil.tryGetFunctionCallFromCfaEdge(cfaEdge);
      if (optionalFunctionCall.isPresent()) {
        CFunctionCall functionCall = optionalFunctionCall.orElseThrow();
        if (PthreadUtil.isCallToPthreadFunction(functionCall, PthreadFunctionType.PTHREAD_CREATE)) {
          assert cfaEdge instanceof CStatementEdge : "pthread_create must be CStatementEdge";
          // extract the first parameter of pthread_create, i.e. the pthread_t value
          CIdExpression pthreadT =
              PthreadUtil.extractPthreadObject(functionCall, PthreadObjectType.PTHREAD_T);
          // extract the 3rd parameter of pthread_create which points to the start_routine function
          CFunctionDeclaration startRoutineDeclaration =
              PthreadUtil.extractStartRoutineDeclaration(functionCall);
          FunctionEntryNode entryNode =
              getStartRoutineFunctionEntryNode(startRoutineDeclaration, cfaEdge);
          MPORThread newThread =
              extractThreadFromCfa(Optional.of(pthreadT), Optional.of(threadEdge), entryNode);
          recursivelyFindThreadCreations(newThread, pFoundThreads);
          pFoundThreads.add(newThread);
        }
      }
    }
  }

  /**
   * Initializes a MPORThread object with the corresponding CFANodes and CFAEdges and returns it.
   */
  private MPORThread extractThreadFromCfa(
      Optional<CIdExpression> pThreadObject,
      Optional<CFAEdgeForThread> pStartRoutineCall,
      FunctionEntryNode pEntryNode) {

    resetPc(); // reset pc for every thread created
    int newThreadId = currentThreadId++;
    CFAForThread threadCfa = buildThreadCfa(newThreadId, pEntryNode, pStartRoutineCall);
    Optional<CIdExpression> startRoutineExitVariable = tryCreateStartRoutineExitVariable(threadCfa);
    ImmutableListMultimap<CVariableDeclaration, Optional<CFAEdgeForThread>> localVariables =
        getLocalVariableDeclarations(threadCfa.threadEdges);
    return new MPORThread(
        newThreadId,
        pThreadObject,
        (CFunctionDeclaration) pEntryNode.getFunction(),
        pStartRoutineCall,
        startRoutineExitVariable,
        localVariables,
        threadCfa);
  }

  private CFAForThread buildThreadCfa(
      int pThreadId, FunctionEntryNode pEntryNode, Optional<CFAEdgeForThread> pStartRoutineCall) {

    // check if node was visited already in a specific calling context
    Multimap<CFANode, Optional<CFAEdgeForThread>> visitedNodes = ArrayListMultimap.create();
    ImmutableList.Builder<CFANodeForThread> threadNodes = ImmutableList.builder();
    // we use an immutable map to preserve ordering (important when declaring types)
    ImmutableMap.Builder<CFAEdgeForThread, CFAEdge> threadEdges = ImmutableMap.builder();

    // recursively build thread CFA, starting in pEntryNode
    buildThreadCfaVariables(
        pThreadId,
        visitedNodes,
        threadNodes,
        threadEdges,
        pEntryNode,
        false, // at start, no atomic block
        Optional.empty(),
        pStartRoutineCall);

    return new CFAForThread(
        pThreadId,
        pEntryNode,
        threadNodes.build(),
        ImmutableList.copyOf(threadEdges.buildOrThrow().keySet()));
  }

  /**
   * Recursively searches the CFA of a thread specified by its entry node (the first pCurrentNode)
   * and pExitNode.
   */
  private void buildThreadCfaVariables(
      final int pThreadId,
      Multimap<CFANode, Optional<CFAEdgeForThread>> pVisitedCfaNodes,
      ImmutableList.Builder<CFANodeForThread> pThreadNodes,
      ImmutableMap.Builder<CFAEdgeForThread, CFAEdge> pThreadEdges,
      CFANode pCurrentNode,
      boolean pIsInAtomicBlock,
      Optional<CFAEdgeForThread> pCallContext,
      final Optional<CFAEdgeForThread> pStartRoutineCall) {

    Optional<CFAEdgeForThread> callContext =
        MPORThreadUtil.getCallContextOrStartRoutineCall(pCallContext, pStartRoutineCall);
    // if node was visited in this context already, return
    if (pVisitedCfaNodes.containsKey(pCurrentNode)) {
      if (pVisitedCfaNodes.get(pCurrentNode).contains(callContext)) {
        return;
      }
    }
    pVisitedCfaNodes.put(pCurrentNode, callContext);

    FluentIterable<CFAEdge> leavingCfaEdges = pCurrentNode.getAllLeavingEdges();
    // all leaving edges of a node are in the atomic block
    ImmutableMultimap<CFAEdgeForThread, CFAEdge> threadEdges =
        buildThreadEdgesFromCfaEdges(pThreadId, leavingCfaEdges, callContext);
    pThreadEdges.putAll(threadEdges.entries());
    List<CFAEdgeForThread> edgeList = new ArrayList<>(threadEdges.keySet());

    // recursively build cfa nodes and edges
    if (leavingCfaEdges.isEmpty()) {
      pThreadNodes.add(
          new CFANodeForThread(
              // thread exits are never in atomic blocks
              pThreadId,
              pCurrentNode,
              ProgramCounterVariables.EXIT_PC,
              callContext,
              edgeList,
              false));
    } else {
      pThreadNodes.add(
          new CFANodeForThread(
              pThreadId, pCurrentNode, currentPc++, callContext, edgeList, pIsInAtomicBlock));
      for (CFAEdge cfaEdge : leavingCfaEdges) {
        // exclude CFunctionReturnEdges in the search, their successors may be in other threads.
        // the original, same-thread successor is included due to the CFunctionSummaryEdge.
        if (!(cfaEdge instanceof CFunctionReturnEdge)) {
          // update the calling context, if a function call is encountered
          if (cfaEdge instanceof CFunctionCallEdge) {
            callContext =
                Optional.ofNullable(
                    Iterables.getOnlyElement(
                        threadEdges.entries().stream()
                            .filter(entry -> entry.getValue().equals(cfaEdge))
                            .map(Map.Entry::getKey)
                            .toList()));
          }
          buildThreadCfaVariables(
              pThreadId,
              pVisitedCfaNodes,
              pThreadNodes,
              pThreadEdges,
              cfaEdge.getSuccessor(),
              updateIsInAtomicBlock(cfaEdge, pIsInAtomicBlock),
              callContext,
              pStartRoutineCall);
        }
      }
    }
  }

  /** Checks if, after calling {@code pCfaEdge}, the thread is still/yet in an atomic block. */
  private boolean updateIsInAtomicBlock(CFAEdge pCfaEdge, boolean pPreviousIsInAtomicBlock) {
    Optional<CFunctionCall> functionCall = PthreadUtil.tryGetFunctionCallFromCfaEdge(pCfaEdge);
    if (functionCall.isPresent()) {
      if (PthreadUtil.isCallToPthreadFunction(
          functionCall.orElseThrow(), PthreadFunctionType.VERIFIER_ATOMIC_BEGIN)) {
        return true;
      } else if (PthreadUtil.isCallToPthreadFunction(
          functionCall.orElseThrow(), PthreadFunctionType.VERIFIER_ATOMIC_END)) {
        return false;
      }
    }
    return pPreviousIsInAtomicBlock;
  }

  /**
   * Returns the intermediate exit variable that stores the {@code retval} given to {@code
   * pthread_exit}, or {@link Optional#empty()} if the thread does not call {@code pthread_exit} at
   * all.
   */
  private Optional<CIdExpression> tryCreateStartRoutineExitVariable(CFAForThread pThreadCFA) {
    for (CFAEdgeForThread threadEdge : pThreadCFA.threadEdges) {
      Optional<CFunctionCall> functionCall =
          PthreadUtil.tryGetFunctionCallFromCfaEdge(threadEdge.cfaEdge);
      if (functionCall.isPresent()) {
        if (PthreadUtil.isCallToPthreadFunction(
            functionCall.orElseThrow(), PthreadFunctionType.PTHREAD_EXIT)) {
          String name = SeqNameUtil.buildStartRoutineExitVariableName(options, pThreadCFA.threadId);
          CVariableDeclaration declaration =
              SeqDeclarationBuilder.buildVariableDeclaration(
                  true, CPointerType.POINTER_TO_VOID, name, null);
          CIdExpression startRoutineExitVariable =
              SeqExpressionBuilder.buildIdExpression(declaration);
          return Optional.of(startRoutineExitVariable);
        }
      }
    }
    return Optional.empty();
  }

  /** Extracts all local variable declarations from pThreadEdges. */
  private ImmutableListMultimap<CVariableDeclaration, Optional<CFAEdgeForThread>>
      getLocalVariableDeclarations(ImmutableList<CFAEdgeForThread> pThreadEdges) {

    ImmutableListMultimap.Builder<CVariableDeclaration, Optional<CFAEdgeForThread>> rLocalVars =
        ImmutableListMultimap.builder();
    for (CFAEdgeForThread threadEdge : pThreadEdges) {
      CFAEdge edge = threadEdge.cfaEdge;
      if (edge instanceof CDeclarationEdge declarationEdge) {
        if (!declarationEdge.getDeclaration().isGlobal()) {
          AAstNode aAstNode = declarationEdge.getRawAST().orElseThrow();
          // exclude FunctionDeclarations
          if (aAstNode instanceof CVariableDeclaration variableDeclaration) {
            rLocalVars.put(variableDeclaration, threadEdge.callContext);
          }
        }
      }
    }
    return rLocalVars.build();
  }

  // (Private) Helpers =============================================================================

  private ImmutableListMultimap<CFAEdgeForThread, CFAEdge> buildThreadEdgesFromCfaEdges(
      int pThreadId, Iterable<CFAEdge> pCfaEdges, Optional<CFAEdgeForThread> pCallContext) {

    // use ImmutableMap to retain insertion order
    ImmutableListMultimap.Builder<CFAEdgeForThread, CFAEdge> rThreadEdges =
        ImmutableListMultimap.builder();
    for (CFAEdge cfaEdge : pCfaEdges) {
      rThreadEdges.put(new CFAEdgeForThread(pThreadId, cfaEdge, pCallContext), cfaEdge);
    }
    return rThreadEdges.build();
  }

  private FunctionEntryNode getStartRoutineFunctionEntryNode(
      CFunctionDeclaration pStartRoutineDeclaration, CFAEdge pPthreadCreateEdge)
      throws UnsupportedCodeException {

    FluentIterable<FunctionEntryNode> functionEntryNodes =
        FluentIterable.from(cfa.getAllFunctions().values())
            .filter(
                functionEntryNode ->
                    Objects.requireNonNull(functionEntryNode)
                        .getFunctionDefinition()
                        .equals(pStartRoutineDeclaration));
    if (functionEntryNodes.isEmpty()) {
      throw new UnsupportedCodeException(
          "Could not find a FunctionEntryNode for start_routine "
              + pStartRoutineDeclaration.getName(),
          pPthreadCreateEdge);
    }
    if (functionEntryNodes.size() > 1) {
      throw new UnsupportedCodeException(
          "Multiple FunctionEntryNodes found for start_routine "
              + pStartRoutineDeclaration.getName(),
          pPthreadCreateEdge);
    }
    return Objects.requireNonNull(Iterables.getOnlyElement(functionEntryNodes));
  }
}
