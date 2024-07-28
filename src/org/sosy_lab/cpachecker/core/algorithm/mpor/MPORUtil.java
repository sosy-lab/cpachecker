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
   * @param pAbstractState TODO
   * @param pEdgeA TODO
   * @param pEdgeB TODO
   * @return TODO
   */
  public static boolean doEdgesCommute(
      @NonNull PredicateTransferRelation pPtr,
      @NonNull PredicateAbstractState pAbstractState,
      @NonNull CFAEdge pEdgeA,
      @NonNull CFAEdge pEdgeB)
      throws CPATransferException, InterruptedException, SolverException {

    checkNotNull(pPtr);
    checkNotNull(pAbstractState);
    checkNotNull(pEdgeA);
    checkNotNull(pEdgeB);

    // TODO this is very costly, leaving it out for now. in tests, the state was always sat
    /*checkArgument(
    !pPtr.unsatCheck(pAbstractState.getAbstractionFormula(), pAbstractState.getPathFormula()),
    "reached abstract must be sat");*/

    // execute edgeA, then edgeB
    PredicateAbstractState aState = getNextPredicateAbstractState(pPtr, pAbstractState, pEdgeA);
    PredicateAbstractState abState = getNextPredicateAbstractState(pPtr, aState, pEdgeB);
    // execute edgeB, then edgeA
    PredicateAbstractState bState = getNextPredicateAbstractState(pPtr, pAbstractState, pEdgeB);
    PredicateAbstractState baState = getNextPredicateAbstractState(pPtr, bState, pEdgeA);

    return abState.getPathFormula().equals(baState.getPathFormula());
  }
}
