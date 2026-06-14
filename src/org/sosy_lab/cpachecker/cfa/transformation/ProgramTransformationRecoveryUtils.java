// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;

public class ProgramTransformationRecoveryUtils {

  /**
   * Copy all wrapped states from pARGState, with a new LocationState and parent ARGState.
   * @param pARGState the ARGState to copy from
   * @param pLocationState the new LocationState
   * @param pParentState the new parent ARGState
   * @return newly created ARGState
   */
  public static ARGState argStateWithLocation(
      ARGState pARGState, LocationState pLocationState, ARGState pParentState) {
    CompositeState currentCompositeState = ((CompositeState) pARGState.getWrappedState());
    List<AbstractState> newWrappedStates =
        new ArrayList<>(currentCompositeState.getWrappedStates().size());
    for (AbstractState wrappedState : currentCompositeState.getWrappedStates()) {
      if (wrappedState instanceof LocationState) {
        newWrappedStates.add(pLocationState);
      } else {
        newWrappedStates.add(wrappedState);
      }
    }
    return new ARGState(new CompositeState(newWrappedStates), pParentState);
  }

  /**
   * Removes the initial ARGState in a program transformation, after a dummy entry edge.
   * @param pPreviousARGState the ARGState before entering the program transformation
   * @param pCurrentARGState the first ARGState in a program transformation
   * @param reached the reached set
   * @return the previous ARGState now connected to the former children of the removed ARGState
   */
  public static ARGState handleEntry(
      ARGState pPreviousARGState,
      ARGState pCurrentARGState,
      ReachedSet reached) {
    assert(pPreviousARGState.getEdgeToChild(pCurrentARGState) instanceof DummyCFAEdge);
    List<ARGState> childStates = List.copyOf(pCurrentARGState.getChildren());
    for (int i = 0; i < childStates.size(); i++) {
      childStates.get(i).addParent(pPreviousARGState);
    }
    reached.remove(pCurrentARGState);
    pCurrentARGState.removeFromARG();
    return pPreviousARGState;
  }

  /**
   * Removes the last state before exiting a program transformation from the ARG and reached set.
   * @param pPreviousARGState previous ARGState
   * @param pCurrentARGState the last ARGState in this program transformation
   * @param reached the reached set
   */
  public static void handleExit(
      ARGState pPreviousARGState,
      ARGState pCurrentARGState,
      ReachedSet reached) {
    assert (pCurrentARGState.getChildren().size() == 1);
    ARGState childState = pCurrentARGState.getChildren().getFirst();
    childState.addParent(pPreviousARGState);
    reached.remove(pCurrentARGState);
    pCurrentARGState.removeFromARG();
  }
}
