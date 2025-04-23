// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class ThreadBuilder {

  private static int currentThreadId;

  /** Track the currentPc, static so that it is consistent across recursive function calls. */
  private static int currentPc = Sequentialization.INIT_PC;

  // TODO pthread_create calls in loops can be considered by loop unrolling
  /**
   * Extracts all threads (main and pthreads) and the FunctionEntry / ExitNodes of their start
   * routines from the given CFA.
   *
   * <p>This functions needs to be called after functionCallMap was initialized so that we can track
   * the calling context of each thread.
   */
  public static ImmutableList<MPORThread> createThreads(CFA pCfa) {
    currentThreadId = 0; // reset thread id (necessary only for unit tests)
    ImmutableList.Builder<MPORThread> rThreads = ImmutableList.builder();
    // add the main thread
    FunctionEntryNode mainEntryNode = pCfa.getMainFunction();
    MPORThread mainThread = createThread(Optional.empty(), Optional.empty(), mainEntryNode);
    rThreads.add(mainThread);
    // recursively search the thread CFAs for pthread_create calls, and store in rThreads
    rThreads.addAll(recursivelyFindThreadCreations(pCfa, mainThread, ImmutableList.builder()));
    return rThreads.build();
  }

  private static ImmutableList<MPORThread> recursivelyFindThreadCreations(
      final CFA pCfa,
      MPORThread pCurrentThread,
      ImmutableList.Builder<MPORThread> pSearchedThreads) {

    for (ThreadEdge threadEdge : pCurrentThread.cfa.threadEdges) {
      CFAEdge cfaEdge = threadEdge.cfaEdge;
      if (PthreadUtil.callsPthreadFunction(cfaEdge, PthreadFunctionType.PTHREAD_CREATE)) {
        assert cfaEdge instanceof CStatementEdge : "pthread_create must be CStatementEdge";
        // extract the first parameter of pthread_create, i.e. the pthread_t value
        CIdExpression pthreadT = PthreadUtil.extractPthreadT(cfaEdge);
        // extract the third parameter of pthread_create which points to the start routine function
        CFunctionType startRoutine = PthreadUtil.extractStartRoutine(cfaEdge);
        FunctionEntryNode entryNode =
            CFAUtils.getFunctionEntryNodeFromCFunctionType(pCfa, startRoutine);
        MPORThread newThread =
            createThread(Optional.ofNullable(pthreadT), Optional.of(threadEdge), entryNode);
        recursivelyFindThreadCreations(pCfa, newThread, pSearchedThreads);
        pSearchedThreads.add(newThread);
      }
    }
    return pSearchedThreads.build();
  }

  /**
   * Initializes a MPORThread object with the corresponding CFANodes and CFAEdges and returns it.
   */
  private static MPORThread createThread(
      Optional<CIdExpression> pThreadObject,
      Optional<ThreadEdge> pStartRoutineCall,
      FunctionEntryNode pEntryNode) {

    // ensure so that we can cast to CFunctionType
    checkArgument(
        pEntryNode.getFunction().getType() instanceof CFunctionType,
        "pEntryNode function must be CFunctionType");
    currentPc = Sequentialization.INIT_PC; // reset pc for every thread created
    ThreadCFA threadCfa = buildThreadCfa(pEntryNode, pStartRoutineCall);
    return new MPORThread(
        currentThreadId++,
        pThreadObject,
        (CFunctionType) pEntryNode.getFunction().getType(),
        pStartRoutineCall,
        getLocalVariableDeclarations(threadCfa.threadEdges),
        threadCfa);
  }

  private static ThreadCFA buildThreadCfa(
      FunctionEntryNode pEntryNode, Optional<ThreadEdge> pStartRoutineCall) {

    // check if node is present already in a specific calling context
    Multimap<CFANode, Optional<ThreadEdge>> visitedNodes = ArrayListMultimap.create();
    ImmutableSet.Builder<ThreadNode> threadNodes = ImmutableSet.builder();
    // we use an immutable map to preserve ordering (important when declaring types)
    ImmutableMap.Builder<ThreadEdge, CFAEdge> threadEdges = ImmutableMap.builder();

    initThreadCfaVariables(
        visitedNodes, threadNodes, threadEdges, pEntryNode, Optional.empty(), pStartRoutineCall);
    return new ThreadCFA(
        pEntryNode, threadNodes.build(), ImmutableList.copyOf(threadEdges.buildOrThrow().keySet()));
  }

  /**
   * Recursively searches the CFA of a thread specified by its entry node (the first pCurrentNode)
   * and pExitNode.
   */
  private static void initThreadCfaVariables(
      Multimap<CFANode, Optional<ThreadEdge>> pVisitedCfaNodes,
      ImmutableSet.Builder<ThreadNode> pThreadNodes,
      ImmutableMap.Builder<ThreadEdge, CFAEdge> pThreadEdges,
      final CFANode pCurrentNode,
      Optional<ThreadEdge> pCallContext,
      final Optional<ThreadEdge> pStartRoutineCall) {

    Optional<ThreadEdge> callContext =
        ThreadUtil.getCallContextOrStartRoutineCall(pCallContext, pStartRoutineCall);
    // if node was visited in this context already, return
    if (pVisitedCfaNodes.containsKey(pCurrentNode)) {
      if (pVisitedCfaNodes.get(pCurrentNode).contains(callContext)) {
        return;
      }
    }
    pVisitedCfaNodes.put(pCurrentNode, callContext);

    FluentIterable<CFAEdge> leavingCfaEdges = CFAUtils.allLeavingEdges(pCurrentNode);
    BiMap<ThreadEdge, CFAEdge> threadEdges =
        createThreadEdgesFromCfaEdges(leavingCfaEdges, callContext);
    pThreadEdges.putAll(threadEdges);
    List<ThreadEdge> edgeList = new ArrayList<>(threadEdges.keySet());

    // recursively build cfa nodes and edges
    if (leavingCfaEdges.isEmpty()) {
      pThreadNodes.add(
          new ThreadNode(pCurrentNode, Sequentialization.EXIT_PC, callContext, edgeList));
    } else {
      pThreadNodes.add(new ThreadNode(pCurrentNode, currentPc++, callContext, edgeList));
      for (CFAEdge cfaEdge : leavingCfaEdges) {
        // exclude function returns, their successors may be in other threads.
        // the original, same-thread successor is included due to the function summary edge.
        if (!(cfaEdge instanceof CFunctionReturnEdge)) {
          // update the calling context, if a function call is encountered
          if (cfaEdge instanceof CFunctionCallEdge) {
            callContext = Optional.of(threadEdges.inverse().get(cfaEdge));
          }
          initThreadCfaVariables(
              pVisitedCfaNodes,
              pThreadNodes,
              pThreadEdges,
              cfaEdge.getSuccessor(),
              callContext,
              pStartRoutineCall);
        }
      }
    }
  }

  /** Extracts all local variable declarations from pThreadEdges. */
  private static ImmutableListMultimap<CVariableDeclaration, Optional<ThreadEdge>>
      getLocalVariableDeclarations(ImmutableList<ThreadEdge> pThreadEdges) {

    ImmutableListMultimap.Builder<CVariableDeclaration, Optional<ThreadEdge>> rLocalVars =
        ImmutableListMultimap.builder();
    for (ThreadEdge threadEdge : pThreadEdges) {
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

  // TODO barriers see pthread-divine for examples
  //  SV benchmarks use their custom barrier objects and functions, e.g. pthread-divine/barrier_2t.i

  // (Private) Helpers =============================================================================

  private static BiMap<ThreadEdge, CFAEdge> createThreadEdgesFromCfaEdges(
      FluentIterable<CFAEdge> pCfaEdges, Optional<ThreadEdge> pCallContext) {

    BiMap<ThreadEdge, CFAEdge> rThreadEdges = HashBiMap.create();
    for (CFAEdge cfaEdge : pCfaEdges) {
      rThreadEdges.put(new ThreadEdge(cfaEdge, pCallContext), cfaEdge);
    }
    return rThreadEdges;
  }
}
