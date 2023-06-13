// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class UsagePrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecAdjustment;

  public UsagePrecisionAdjustment(PrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision oldPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState)
      throws CPAException, InterruptedException {

    Preconditions.checkArgument(pElement instanceof UsageState);
    UsageState element = (UsageState) pElement;

    UnmodifiableReachedSet elements =
        new UnmodifiableReachedSetView(
            pElements, (state) -> ((UsageState) state).getWrappedState(), Functions.identity());

    AbstractState oldElement = element.getWrappedState();

    Precision oldWrappedPrecision = ((UsagePrecision) oldPrecision).getWrappedPrecision();

    Optional<PrecisionAdjustmentResult> optionalUnwrappedResult =
        wrappedPrecAdjustment.prec(
            oldElement,
            oldWrappedPrecision,
            elements,
            Functions.compose((state) -> ((UsageState) state).getWrappedState(), stateProjection),
            fullState);

    if (!optionalUnwrappedResult.isPresent()) {
      return Optional.empty();
    }

    PrecisionAdjustmentResult unwrappedResult = optionalUnwrappedResult.orElseThrow();

    AbstractState newElement = unwrappedResult.abstractState();
    Precision newPrecision = unwrappedResult.precision();
    Action action = unwrappedResult.action();

    if ((oldElement == newElement) && (oldWrappedPrecision == newPrecision)) {
      // nothing has changed
      return Optional.of(new PrecisionAdjustmentResult(pElement, oldPrecision, action));
    }

    UsageState resultElement = element.copy(newElement);

    return Optional.of(
        new PrecisionAdjustmentResult(
            resultElement, ((UsagePrecision) oldPrecision).copy(newPrecision), action));
  }
}
