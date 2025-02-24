// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.util.CFAUtils;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class ThreadBuilder {

  private int currentId = 0;

  private int currentPc = SeqUtil.INIT_PC;

  /** A copy of the functionCallMap in {@link MPORAlgorithm}. */
  private final ImmutableMap<CFANode, CFANode> functionCallMap;

  public ThreadBuilder(ImmutableMap<CFANode, CFANode> pFunctionCallMap) {
    functionCallMap = pFunctionCallMap;
  }

  /**
   * Initializes a MPORThread object with the corresponding CFANodes and CFAEdges and returns it.
   *
   * @param pThreadObject the pthread_t object, set to empty for the main thread
   * @param pEntryNode the entry node of the start routine or main function of the thread
   * @return a MPORThread object with properly initialized variables
   */
  public MPORThread createThread(
      Optional<CIdExpression> pThreadObject, FunctionEntryNode pEntryNode) {

    currentPc = SeqUtil.INIT_PC; // reset pc for every thread created

    Set<CFANode> visitedNodes = new HashSet<>(); // using set so that we can use .contains()
    ImmutableSet.Builder<ThreadNode> threadNodes = ImmutableSet.builder();
    ImmutableSet.Builder<ThreadEdge> threadEdges = ImmutableSet.builder();
    ImmutableSet.Builder<CFunctionDeclaration> calledFuncs = ImmutableSet.builder();

    initThreadVariables(
        visitedNodes, threadNodes, threadEdges, calledFuncs, pEntryNode, Optional.empty());

    ThreadCFA threadCfa =
        new ThreadCFA(pEntryNode, threadNodes.build(), threadEdges.build(), calledFuncs.build());

    ImmutableSet<CVariableDeclaration> localVars = getLocalVars(threadEdges.build());

    assert pEntryNode.getFunction().getType() instanceof CFunctionType;

    return new MPORThread(
        currentId++,
        (CFunctionType) pEntryNode.getFunction().getType(),
        pThreadObject,
        localVars,
        threadCfa);
  }

  /**
   * Recursively searches the CFA of a thread specified by its entry node (the first pCurrentNode)
   * and pExitNode.
   *
   * @param pVisitedNodes the set of already visited CFANodes
   * @param pThreadNodes the set of ThreadNodes reachable by the thread
   * @param pCurrentNode the current CFANode whose leaving CFAEdges are analyzed
   * @param pFuncReturnNode pFuncReturnNode used to track the original context when entering the CFA
   *     of another function.
   */
  private void initThreadVariables(
      Set<CFANode> pVisitedNodes,
      ImmutableSet.Builder<ThreadNode> pThreadNodes,
      ImmutableSet.Builder<ThreadEdge> pThreadEdges,
      ImmutableSet.Builder<CFunctionDeclaration> pCalledFuncs,
      CFANode pCurrentNode,
      Optional<CFANode> pFuncReturnNode) {

    if (pVisitedNodes.add(pCurrentNode)) {
      FluentIterable<CFAEdge> leavingCfaEdges = CFAUtils.allLeavingEdges(pCurrentNode);
      List<ThreadEdge> threadEdges = createThreadEdgesFromCfaEdges(leavingCfaEdges);
      pThreadEdges.addAll(threadEdges);
      if (leavingCfaEdges.isEmpty()) {
        pThreadNodes.add(new ThreadNode(pCurrentNode, SeqUtil.EXIT_PC, threadEdges));
      } else {
        pThreadNodes.add(new ThreadNode(pCurrentNode, currentPc++, threadEdges));
        for (CFAEdge cfaEdge : leavingCfaEdges) {
          // exclude cFuncReturnEdges because their successors may be in other threads
          // the original, same-thread successor is included due to the FuncSummaryEdge
          if (!(cfaEdge instanceof CFunctionReturnEdge)) {
            if (cfaEdge instanceof CFunctionCallEdge funcCallEdge) {
              pCalledFuncs.add(funcCallEdge.getFunctionCallExpression().getDeclaration());
            }
            initThreadVariables(
                pVisitedNodes,
                pThreadNodes,
                pThreadEdges,
                pCalledFuncs,
                cfaEdge.getSuccessor(),
                updateFuncReturnNode(pCurrentNode, pFuncReturnNode));
          }
        }
      }
    }
  }

  /** Extracts all local variable declarations from pThreadEdges. */
  private ImmutableSet<CVariableDeclaration> getLocalVars(ImmutableSet<ThreadEdge> pThreadEdges) {
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

  private Optional<CFANode> updateFuncReturnNode(
      CFANode pCurrentNode, Optional<CFANode> pPrevFuncReturnNode) {
    return MPORUtil.updateFuncReturnNode(functionCallMap, pCurrentNode, pPrevFuncReturnNode);
  }

  private List<ThreadEdge> createThreadEdgesFromCfaEdges(FluentIterable<CFAEdge> pCfaEdges) {
    List<ThreadEdge> rThreadEdges = new ArrayList<>();
    for (CFAEdge cfaEdge : pCfaEdges) {
      rThreadEdges.add(new ThreadEdge(cfaEdge));
    }
    return rThreadEdges;
  }
}
