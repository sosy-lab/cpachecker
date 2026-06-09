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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;

public class ProgramTransformationRecoveryUtils {

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
}
