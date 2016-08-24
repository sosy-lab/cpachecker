/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import java.util.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.HashSet;
import java.util.Set;

public class ARGPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecAdjustment;
  private final ARGStatistics statistics;

  protected final boolean inCPAEnabledAnalysis;


  public ARGPrecisionAdjustment(PrecisionAdjustment pWrappedPrecAdjustment, boolean pInCPAEnabledAnalysis, ARGStatistics pStats) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
    inCPAEnabledAnalysis = pInCPAEnabledAnalysis;
    statistics = pStats;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(AbstractState pElement,
      Precision oldPrecision,
      UnmodifiableReachedSet pElements,
      final Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {

    Preconditions.checkArgument(pElement instanceof ARGState);
    //noinspection ConstantConditions
    ARGState element = (ARGState)pElement;

    // do precision adjustment
    Optional<PrecisionAdjustmentResult> result = prec(element, oldPrecision, pElements, projection, fullState);

    // print statistics for this algorithm iteration (if necessary)
    statistics.printIterationStatistics(pElements);

    return result;
  }

  private Optional<PrecisionAdjustmentResult> prec(final ARGState element,
      Precision oldPrecision,
      UnmodifiableReachedSet pElements,
      final Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException, InterruptedException {

    if (inCPAEnabledAnalysis && element.isTarget()) {
      if (elementHasSiblings(element)) {
        removeUnreachedSiblingsFromARG(element, pElements);
      }
      // strengthening of PredicateCPA already proved if path is infeasible and removed infeasible element
      // thus path is feasible here
      throw new CPAEnabledAnalysisPropertyViolationException("Property violated during successor computation", element, false);
    }

    AbstractState oldElement = element.getWrappedState();

    Optional<PrecisionAdjustmentResult> optionalUnwrappedResult =
        wrappedPrecAdjustment.prec(
            oldElement,
            oldPrecision,
            pElements,
            Functions.compose(
                ARGState.getUnwrapFunction(),
                projection),
            fullState
        );

    if (!optionalUnwrappedResult.isPresent()) {
      element.removeFromARG();
      return Optional.empty();
    }

    PrecisionAdjustmentResult unwrappedResult = optionalUnwrappedResult.get();

    // ensure that ARG and reached set are consistent if BREAK is signaled for a state with multiple children
    if (unwrappedResult.action() == Action.BREAK && elementHasSiblings(element)) {
      removeUnreachedSiblingsFromARG(element, pElements);
    }

    AbstractState newElement = unwrappedResult.abstractState();
    Precision newPrecision = unwrappedResult.precision();
    Action action = unwrappedResult.action();

    if ((oldElement == newElement) && (oldPrecision == newPrecision)) {
      // nothing has changed
      return Optional.of(PrecisionAdjustmentResult.create(element, oldPrecision, action));
    }

    ARGState resultElement = new ARGState(newElement, null);

    element.replaceInARGWith(resultElement); // this completely eliminates element

    return Optional.of(PrecisionAdjustmentResult.create(resultElement, newPrecision, action));
  }

  /**
   * This method removes all siblings of the given element from the ARG, if they are not yet in the reached set.
   *
   * These measures are necessary in the cases where precision adjustment signals {@link Action#BREAK} for a state
   * whose parent has multiple children, and not all children have been processed completely. In this case, not all
   * children would be in the reached set, however, are already in the ARG (as children of their parent). To avoid this
   * inconsistency, all children not yet contained in the reached set are removed from the ARG.
   *
   * @param element the element for which to remove the siblings
   * @param pReachedSet the current reached set
   */
  private void removeUnreachedSiblingsFromARG(ARGState element, UnmodifiableReachedSet pReachedSet) {
    Set<ARGState> scheduledForDeletion = new HashSet<>();

    for (ARGState sibling : Iterables.getOnlyElement(element.getParents()).getChildren()) {
      if (sibling != element && !pReachedSet.contains(sibling)) {
        scheduledForDeletion.add(sibling);
      }
    }

    for (ARGState sibling : scheduledForDeletion) {
      sibling.removeFromARG();
    }
  }

  /**
   * This method checks if the given element has a sibling in the ARG.
   *
   * @param element the element to check
   * @return true, if the element has a sibling in the ARG
   */
  private boolean elementHasSiblings(ARGState element) {
    return Iterables.getOnlyElement(element.getParents()).getChildren().size() > 1;
  }
}