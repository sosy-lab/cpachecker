// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
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
  public static ImmutableList<MPORThread> createThreads(
      CFA pCfa, ImmutableMap<CFANode, CFANode> pFunctionCallMap) {

    currentThreadId = 0; // reset thread id (necessary only for unit tests)
    ImmutableList.Builder<MPORThread> rThreads = ImmutableList.builder();
    // add the main thread
    FunctionEntryNode mainEntryNode = pCfa.getMainFunction();
    rThreads.add(createThread(Optional.empty(), pFunctionCallMap, mainEntryNode));

    // search the CFA for pthread_create calls
    for (CFAEdge cfaEdge : CFAUtils.allUniqueEdges(pCfa)) {
      if (PthreadUtil.callsPthreadFunction(cfaEdge, PthreadFunctionType.PTHREAD_CREATE)) {
        // extract the first parameter of pthread_create, i.e. the pthread_t value
        CIdExpression pthreadT = PthreadUtil.extractPthreadT(cfaEdge);
        // extract the third parameter of pthread_create which points to the start routine function
        CFunctionType startRoutine = PthreadUtil.extractStartRoutine(cfaEdge);
        FunctionEntryNode entryNode =
            CFAUtils.getFunctionEntryNodeFromCFunctionType(pCfa, startRoutine);
        rThreads.add(createThread(Optional.ofNullable(pthreadT), pFunctionCallMap, entryNode));
      }
    }
    return rThreads.build();
  }

  /**
   * Initializes a MPORThread object with the corresponding CFANodes and CFAEdges and returns it.
   */
  private static MPORThread createThread(
      Optional<CIdExpression> pThreadObject,
      ImmutableMap<CFANode, CFANode> pFunctionCallMap,
      FunctionEntryNode pEntryNode) {

    // ensure so that we can cast to CFunctionType
    checkArgument(
        pEntryNode.getFunction().getType() instanceof CFunctionType,
        "pEntryNode function must be CFunctionType");
    currentPc = Sequentialization.INIT_PC; // reset pc for every thread created
    ThreadCFA threadCfa = buildThreadCfa(pEntryNode, pFunctionCallMap, Optional.empty());
    return new MPORThread(
        currentThreadId++,
        (CFunctionType) pEntryNode.getFunction().getType(),
        pThreadObject,
        getLocalVariableDeclarations(threadCfa.threadEdges),
        threadCfa);
  }

  private static ThreadCFA buildThreadCfa(
      FunctionEntryNode pEntryNode,
      ImmutableMap<CFANode, CFANode> pFunctionCallMap,
      Optional<CFANode> pFuncReturnNode) {

    Set<CFANode> visitedNodes = new HashSet<>(); // using set to check if node is present already
    ImmutableSet.Builder<ThreadNode> threadNodes = ImmutableSet.builder();
    ImmutableSet.Builder<ThreadEdge> threadEdges = ImmutableSet.builder();

    initThreadCfaVariables(
        visitedNodes, threadNodes, threadEdges, pEntryNode, pFunctionCallMap, pFuncReturnNode);
    return new ThreadCFA(pEntryNode, threadNodes.build(), threadEdges.build());
  }

  /**
   * Recursively searches the CFA of a thread specified by its entry node (the first pCurrentNode)
   * and pExitNode.
   */
  private static void initThreadCfaVariables(
      Set<CFANode> pVisitedNodes,
      ImmutableSet.Builder<ThreadNode> pThreadNodes,
      ImmutableSet.Builder<ThreadEdge> pThreadEdges,
      CFANode pCurrentNode,
      ImmutableMap<CFANode, CFANode> pFunctionCallMap,
      Optional<CFANode> pFuncReturnNode) {

    if (pVisitedNodes.add(pCurrentNode)) {
      FluentIterable<CFAEdge> leavingCfaEdges = CFAUtils.allLeavingEdges(pCurrentNode);
      List<ThreadEdge> threadEdges = createThreadEdgesFromCfaEdges(leavingCfaEdges);
      pThreadEdges.addAll(threadEdges);
      if (leavingCfaEdges.isEmpty()) {
        pThreadNodes.add(new ThreadNode(pCurrentNode, Sequentialization.EXIT_PC, threadEdges));
      } else {
        pThreadNodes.add(new ThreadNode(pCurrentNode, currentPc++, threadEdges));
        for (CFAEdge cfaEdge : leavingCfaEdges) {
          // exclude cFuncReturnEdges because their successors may be in other threads
          // the original, same-thread successor is included due to the FuncSummaryEdge
          if (!(cfaEdge instanceof CFunctionReturnEdge)) {
            initThreadCfaVariables(
                pVisitedNodes,
                pThreadNodes,
                pThreadEdges,
                cfaEdge.getSuccessor(),
                pFunctionCallMap,
                updateFuncReturnNode(pCurrentNode, pFunctionCallMap, pFuncReturnNode));
          }
        }
      }
    }
  }

  /** Extracts all local variable declarations from pThreadEdges. */
  private static ImmutableSet<CVariableDeclaration> getLocalVariableDeclarations(
      ImmutableSet<ThreadEdge> pThreadEdges) {

    ImmutableSet.Builder<CVariableDeclaration> rLocalVars = ImmutableSet.builder();
    for (ThreadEdge threadEdge : pThreadEdges) {
      CFAEdge edge = threadEdge.cfaEdge;
      if (edge instanceof CDeclarationEdge declarationEdge) {
        if (!declarationEdge.getDeclaration().isGlobal()) {
          AAstNode aAstNode = declarationEdge.getRawAST().orElseThrow();
          // exclude FunctionDeclarations
          if (aAstNode instanceof CVariableDeclaration cVarDec) {
            rLocalVars.add(cVarDec);
          }
        }
      }
    }
    return rLocalVars.build();
  }

  // TODO barriers see pthread-divine for examples
  //  SV benchmarks use their custom barrier objects and functions, e.g. pthread-divine/barrier_2t.i

  // (Private) Helpers =============================================================================

  private static Optional<CFANode> updateFuncReturnNode(
      CFANode pCurrentNode,
      ImmutableMap<CFANode, CFANode> pFunctionCallMap,
      Optional<CFANode> pPrevFuncReturnNode) {

    return MPORUtil.updateFunctionReturnNode(pFunctionCallMap, pCurrentNode, pPrevFuncReturnNode);
  }

  private static List<ThreadEdge> createThreadEdgesFromCfaEdges(FluentIterable<CFAEdge> pCfaEdges) {
    List<ThreadEdge> rThreadEdges = new ArrayList<>();
    for (CFAEdge cfaEdge : pCfaEdges) {
      rThreadEdges.add(new ThreadEdge(cfaEdge));
    }
    return rThreadEdges;
  }
}
