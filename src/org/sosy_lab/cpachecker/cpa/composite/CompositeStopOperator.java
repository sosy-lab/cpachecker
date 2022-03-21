// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.CoveringStateSetProvider;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This abstract class contains the common methods for both {@link CompositeStopSepOperator} and
 * {@link CompositeStopJoinOperator}
 */
abstract class CompositeStopOperator
    implements ForcedCoveringStopOperator, CoveringStateSetProvider {
  final ImmutableList<StopOperator> stopOperators;

  CompositeStopOperator(ImmutableList<StopOperator> stopOperators) {
    this.stopOperators = stopOperators;
  }

  boolean isCoveredBy(
      AbstractState pElement, AbstractState pOtherElement, List<ConfigurableProgramAnalysis> cpas)
      throws CPAException, InterruptedException {
    CompositeState compositeState = (CompositeState) pElement;
    CompositeState compositeOtherElement = (CompositeState) pOtherElement;

    List<AbstractState> componentElements = compositeState.getWrappedStates();
    List<AbstractState> componentOtherElements = compositeOtherElement.getWrappedStates();

    if (componentElements.size() != cpas.size()) {
      return false;
    }

    for (int idx = 0; idx < componentElements.size(); idx++) {
      ProofChecker componentProofChecker = (ProofChecker) cpas.get(idx);

      AbstractState absElem1 = componentElements.get(idx);
      AbstractState absElem2 = componentOtherElements.get(idx);

      if (!componentProofChecker.isCoveredBy(absElem1, absElem2)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean isForcedCoveringPossible(
      AbstractState pElement, AbstractState pReachedState, Precision pPrecision)
      throws CPAException, InterruptedException {

    CompositeState compositeState = (CompositeState) pElement;
    CompositeState compositeReachedState = (CompositeState) pReachedState;
    CompositePrecision compositePrecision = (CompositePrecision) pPrecision;

    List<AbstractState> compositeElements = compositeState.getWrappedStates();
    List<AbstractState> compositeReachedStates = compositeReachedState.getWrappedStates();
    List<Precision> compositePrecisions = compositePrecision.getWrappedPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);

      AbstractState wrappedState = compositeElements.get(idx);
      AbstractState wrappedReachedState = compositeReachedStates.get(idx);
      Precision prec = compositePrecisions.get(idx);

      boolean possible;
      if (stopOp instanceof ForcedCoveringStopOperator) {

        possible =
            ((ForcedCoveringStopOperator) stopOp)
                .isForcedCoveringPossible(wrappedState, wrappedReachedState, prec);

      } else {
        possible = stopOp.stop(wrappedState, Collections.singleton(wrappedReachedState), prec);
      }

      if (!possible) {
        return false;
      }
    }

    return true;
  }
}
