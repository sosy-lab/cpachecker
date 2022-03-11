// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARGPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecAdjustment;
  private final ARGStatistics statistics;

  protected final boolean inCPAEnabledAnalysis;

  public ARGPrecisionAdjustment(
      PrecisionAdjustment pWrappedPrecAdjustment,
      boolean pInCPAEnabledAnalysis,
      ARGStatistics pStats) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
    inCPAEnabledAnalysis = pInCPAEnabledAnalysis;
    statistics = pStats;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision oldPrecision,
      UnmodifiableReachedSet pElements,
      final Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    Preconditions.checkArgument(pElement instanceof ARGState);
    //noinspection ConstantConditions
    ARGState element = (ARGState) pElement;

    // do precision adjustment
    Optional<PrecisionAdjustmentResult> result =
        prec(element, oldPrecision, pElements, projection, fullState);

    // print statistics for this algorithm iteration (if necessary)
    statistics.printIterationStatistics(pElements);

    return result;
  }

  private Optional<PrecisionAdjustmentResult> prec(
      final ARGState element,
      Precision oldPrecision,
      UnmodifiableReachedSet pElements,
      final Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    if (inCPAEnabledAnalysis && element.isTarget()) {
      if (elementHasSiblings(element)) {
        removeUnreachedSiblingsFromARG(element, pElements);
      }
      // strengthening of PredicateCPA already proved if path is infeasible and removed infeasible
      // element
      // thus path is feasible here
      throw new CPAEnabledAnalysisPropertyViolationException(
          "Property violated during successor computation", element, false);
    }

    AbstractState oldElement = element.getWrappedState();

    Optional<PrecisionAdjustmentResult> optionalUnwrappedResult =
        wrappedPrecAdjustment.prec(
            oldElement,
            oldPrecision,
            pElements,
            Functions.compose((state) -> ((ARGState) state).getWrappedState(), projection),
            fullState);

    if (!optionalUnwrappedResult.isPresent()) {
      element.removeFromARG();
      return Optional.empty();
    }

    PrecisionAdjustmentResult unwrappedResult = optionalUnwrappedResult.orElseThrow();

    // ensure that ARG and reached set are consistent if BREAK is signaled for a state with multiple
    // children
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
   * This method removes all siblings of the given element from the ARG, if they are not yet in the
   * reached set.
   *
   * <p>These measures are necessary in the cases where precision adjustment signals {@link
   * Action#BREAK} for a state whose parent has multiple children, and not all children have been
   * processed completely. In this case, not all children would be in the reached set, however, are
   * already in the ARG (as children of their parent). To avoid this inconsistency, all children not
   * yet contained in the reached set are removed from the ARG.
   *
   * @param element the element for which to remove the siblings
   * @param pReachedSet the current reached set
   */
  private void removeUnreachedSiblingsFromARG(
      ARGState element, UnmodifiableReachedSet pReachedSet) {
    ImmutableList.Builder<ARGState> scheduledForDeletion = ImmutableList.builder();

    for (ARGState sibling : Iterables.getOnlyElement(element.getParents()).getChildren()) {
      if (!Objects.equals(sibling, element) && !pReachedSet.contains(sibling)) {
        scheduledForDeletion.add(sibling);
      }
    }

    for (ARGState sibling : scheduledForDeletion.build()) {
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
