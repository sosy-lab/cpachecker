// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** Contains static methods that can perfectly be reused outside the MPOR context. */
public final class MPORUtil {

  /**
   * Searches pFunctionCallMap for pCurrentNode. If the key is present, the FunctionReturnNode is
   * returned. If not, we take the previous pPrevFuncReturnNode or reset it to {@link
   * Optional#empty()} if pCurrentNode is a FunctionExitNode, i.e. the previous pPrevFuncReturnNode
   * is not relevant anymore in the next iteration.
   */
  public static Optional<CFANode> updateFunctionReturnNode(
      ImmutableMap<CFANode, CFANode> pFunctionCallMap,
      CFANode pCurrentNode,
      Optional<CFANode> pPrevFuncReturnNode) {

    if (pFunctionCallMap.containsKey(pCurrentNode)) {
      return Optional.ofNullable(pFunctionCallMap.get(pCurrentNode));
    } else {
      if (pCurrentNode instanceof FunctionExitNode) {
        return Optional.empty();
      } else {
        return pPrevFuncReturnNode;
      }
    }
  }

  /**
   * Returns the successor PredicateAbstractState when executing pCfaEdge.
   *
   * @param pPtr the PredicateTransferRelation that handles creating a successor AbstractState
   * @param pCurrentState the current PredicateAbstractState in which we execute pCfaEdge
   * @param pCfaEdge the CFAEdge that is executed
   * @return the successor PredicateAbstractState when executing pCfaEdge
   */
  public static @NonNull PredicateAbstractState getNextPredicateAbstractState(
      @NonNull PredicateTransferRelation pPtr,
      @NonNull PredicateAbstractState pCurrentState,
      @NonNull CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    checkNotNull(pPtr);
    checkNotNull(pCurrentState);
    checkNotNull(pCfaEdge);

    // using no precision (null) is fine in the context of MPOR
    Collection<? extends AbstractState> abstractStates =
        pPtr.getAbstractSuccessorsForEdge(pCurrentState, null, pCfaEdge);
    checkState(abstractStates.size() == 1); // should always hold

    PredicateAbstractState rAbstractSuccessor =
        AbstractStates.extractStateByType(
            abstractStates.iterator().next(), PredicateAbstractState.class);
    checkNotNull(rAbstractSuccessor);
    return rAbstractSuccessor;
  }

  // TODO (not sure if important for our algorithm) PredicateAbstractState.abstractLocations
  //  contains all CFANodes visited so far
  /**
   * Checks whether two CFAEdges a and b <i>commute</i>, i.e. if, from the current pAbstractState,
   * executing a then b or b then a result in the same PathFormula.
   *
   * @param pPtr The PredicateTransferRelation instance used to find the next PathFormulas
   * @param pAbstractState the current PredicateAbstractState of the program, i.e. the state
   *     containing the current PathFormula with previous transitions
   * @param pEdgeA CFAEdge a
   * @param pEdgeB CFAEdge b
   * @return true if pEdgeA and pEdgeB commute
   */
  public static boolean doEdgesCommute(
      @NonNull PredicateTransferRelation pPtr,
      @NonNull PredicateAbstractState pAbstractState,
      @NonNull CFAEdge pEdgeA,
      @NonNull CFAEdge pEdgeB)
      throws CPATransferException, InterruptedException {

    checkNotNull(pPtr);
    checkNotNull(pAbstractState);
    checkNotNull(pEdgeA);
    checkNotNull(pEdgeB);

    // TODO this is very costly, leaving it out for now. in tests, the state was always sat
    /*checkArgument(
    !pPtr.unsatCheck(pAbstractState.getAbstractionFormula(), pAbstractState.getPathFormula()),
    "reached abstract state must be sat");*/

    // execute edgeA, then edgeB
    PredicateAbstractState aState = getNextPredicateAbstractState(pPtr, pAbstractState, pEdgeA);
    PredicateAbstractState abState = getNextPredicateAbstractState(pPtr, aState, pEdgeB);
    // execute edgeB, then edgeA
    PredicateAbstractState bState = getNextPredicateAbstractState(pPtr, pAbstractState, pEdgeB);
    PredicateAbstractState baState = getNextPredicateAbstractState(pPtr, bState, pEdgeA);

    return abState.getPathFormula().equals(baState.getPathFormula());
  }

  /**
   * Returns {@code true} if pOrigin can be reached through its successor {@link CFANode}. <br>
   * If pStop is encountered in a path, it is not explored further, even if pOrigin may be in the
   * path.
   */
  public static boolean isSelfReachable(
      final CFAEdge pOrigin,
      final Optional<CFAEdge> pStop,
      List<CFAEdge> pVisited,
      CFAEdge pCurrent) {

    pVisited.add(pCurrent);
    boolean foundPath = false;
    for (CFAEdge cfaEdge : CFAUtils.leavingEdges(pCurrent.getSuccessor())) {
      // ignore edges that lead to pStop
      if (!(pStop.isPresent() && cfaEdge.equals(pStop.orElseThrow()))) {
        if (cfaEdge.equals(pOrigin)) {
          // self reach found
          return true;
        } else if (!pVisited.contains(cfaEdge)) {
          // visit edges only once, otherwise we trigger a stack overflow
          foundPath = isSelfReachable(pOrigin, pStop, pVisited, cfaEdge);
          if (foundPath) {
            break;
          }
        }
      }
    }
    return foundPath;
  }

  /**
   * Returns {@code true} if pOrigin can be reached through its leaving edges. <br>
   * If pStop is encountered in a path, it is not explored further, even if pOrigin may be in the
   * path.
   */
  public static boolean isSelfReachable(
      final CFANode pOrigin,
      final Optional<CFANode> pStop,
      List<CFANode> pVisited,
      CFANode pCurrent) {

    pVisited.add(pCurrent);
    boolean foundPath = false;
    for (CFAEdge cfaEdge : CFAUtils.leavingEdges(pCurrent)) {
      CFANode successor = cfaEdge.getSuccessor();
      // ignore edges that lead to pStop
      if (!(pStop.isPresent() && successor.equals(pStop.orElseThrow()))) {
        if (successor.equals(pOrigin)) {
          // self reach found
          return true;
        } else if (!pVisited.contains(successor)) {
          // visit edges only once, otherwise we trigger a stack overflow
          foundPath = isSelfReachable(pOrigin, pStop, pVisited, cfaEdge.getSuccessor());
          if (foundPath) {
            break;
          }
        }
      }
    }
    return foundPath;
  }

  public static boolean isReachErrorCall(ThreadEdge pThreadEdge) {
    if (pThreadEdge.cfaEdge instanceof CFunctionSummaryEdge functionSummaryEdge) {
      return isReachErrorCall(functionSummaryEdge);
    } else if (pThreadEdge.cfaEdge instanceof CFunctionCallEdge functionCallEdge) {
      return isReachErrorCall(functionCallEdge);
    }
    return false;
  }

  public static boolean isReachErrorCall(CFunctionSummaryEdge pFunctionSummaryEdge) {
    return pFunctionSummaryEdge
        .getFunctionEntry()
        .getFunction()
        .getOrigName()
        .equals(SeqToken.reach_error);
  }

  public static boolean isReachErrorCall(CFunctionCallEdge pFunctionCallEdge) {
    return pFunctionCallEdge
        .getFunctionCallExpression()
        .getDeclaration()
        .getOrigName()
        .equals(SeqToken.reach_error);
  }

  public static boolean isConstCpaCheckerTmp(CVariableDeclaration pVarDec) {
    return pVarDec.getType().isConst()
        && !pVarDec.isGlobal()
        && pVarDec.getName().contains(SeqToken.__CPAchecker_TMP_)
        // in tests, const CPAchecker_TMP variables always had initializer
        && pVarDec.getInitializer() != null;
  }

  public static boolean isConstCpaCheckerTmpDeclaration(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof CDeclarationEdge declarationEdge) {
      if (declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
        return isConstCpaCheckerTmp(variableDeclaration);
      }
    }
    return false;
  }
}
