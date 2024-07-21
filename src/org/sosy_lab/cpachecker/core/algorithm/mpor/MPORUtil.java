// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

/** Contains static methods that can perfectly be reused outside the MPOR context. */
public final class MPORUtil {

  /**
   * Tries to extract the FunctionExitNode from the given FunctionEntryNode.
   *
   * @param pFunctionEntryNode the FunctionEntryNode
   * @return the FunctionExitNode of FunctionEntryNode if it is present
   * @throws IllegalArgumentException if pFunctionEntryNodes FunctionExitNode is empty
   */
  public static FunctionExitNode getFunctionExitNode(FunctionEntryNode pFunctionEntryNode) {
    checkArgument(
        pFunctionEntryNode.getExitNode().isPresent(),
        "pFunctionEntryNode must contain a FunctionExitNode");
    return pFunctionEntryNode.getExitNode().orElseThrow();
  }

  /**
   * TODO
   *
   * @param pPtr TODO
   * @param pCurrentState TODO
   * @param pCfaEdge TODO
   * @return TODO
   * @throws CPATransferException TODO
   * @throws InterruptedException TODO
   */
  public static @NonNull PredicateAbstractState getNextPredicateAbstractState(
      @NonNull PredicateTransferRelation pPtr,
      @NonNull PredicateAbstractState pCurrentState,
      @NonNull CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    checkNotNull(pPtr);
    checkNotNull(pCurrentState);
    checkNotNull(pCfaEdge);

    Collection<? extends AbstractState> abstractStates =
        pPtr.getAbstractSuccessorsForEdge(pCurrentState, null, pCfaEdge); // TODO precision?
    checkState(abstractStates.size() == 1); // should always hold

    PredicateAbstractState rAbstractSuccessor =
        AbstractStates.extractStateByType(
            abstractStates.iterator().next(), PredicateAbstractState.class);
    checkNotNull(rAbstractSuccessor);
    return rAbstractSuccessor;
  }

  /**
   * TODO
   *
   * @param pPtr TODO
   * @param pAbstractStateA TODO
   * @param pAbstractStateB TODO
   * @param pEdgeA TODO
   * @param pEdgeB TODO
   * @return TODO
   */
  public static boolean doEdgesCommute(
      @NonNull PredicateTransferRelation pPtr,
      @NonNull PredicateAbstractState pAbstractStateA,
      @NonNull PredicateAbstractState pAbstractStateB,
      @NonNull CFAEdge pEdgeA,
      @NonNull CFAEdge pEdgeB)
      throws CPATransferException, InterruptedException, SolverException {

    checkNotNull(pPtr);
    checkNotNull(pAbstractStateA);
    checkNotNull(pAbstractStateB);
    checkNotNull(pEdgeA);
    checkNotNull(pEdgeB);

    // TODO are the unsatChecks sufficient to check if the edges commute?
    //  can we check if the two states abstraction + pathFormulas are semantically equivalent?

    // execute edgeA, then edgeB
    PredicateAbstractState aState = getNextPredicateAbstractState(pPtr, pAbstractStateA, pEdgeA);
    PredicateAbstractState abState = getNextPredicateAbstractState(pPtr, aState, pEdgeB);
    if (pPtr.unsatCheck(abState.getAbstractionFormula(), abState.getPathFormula())) {
      return false;
    }
    // execute edgeB, then edgeA
    PredicateAbstractState bState = getNextPredicateAbstractState(pPtr, pAbstractStateB, pEdgeB);
    PredicateAbstractState baState = getNextPredicateAbstractState(pPtr, bState, pEdgeA);
    if (pPtr.unsatCheck(baState.getAbstractionFormula(), baState.getPathFormula())) {
      return false;
    }

    return true;
  }
}
