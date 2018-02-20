/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class UsagePrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecAdjustment;

  public UsagePrecisionAdjustment(PrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(AbstractState pElement,
      Precision oldPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState) throws CPAException, InterruptedException {

    Preconditions.checkArgument(pElement instanceof UsageState);
    UsageState element = (UsageState)pElement;

    UnmodifiableReachedSet elements = new UnmodifiableReachedSetView(
        pElements,  ARGState.getUnwrapFunction(), Functions.<Precision>identity());

    AbstractState oldElement = element.getWrappedState();

    Precision oldWrappedPrecision = ((UsagePrecision)oldPrecision).getWrappedPrecision();

    Optional<PrecisionAdjustmentResult> optionalUnwrappedResult =
        wrappedPrecAdjustment.prec(oldElement, oldWrappedPrecision, elements,
        Functions.compose(
          ARGState.getUnwrapFunction(),
          stateProjection),
        fullState);

    if (!optionalUnwrappedResult.isPresent()) {
      return Optional.empty();
    }

    PrecisionAdjustmentResult unwrappedResult = optionalUnwrappedResult.get();

    AbstractState newElement = unwrappedResult.abstractState();
    Precision newPrecision = unwrappedResult.precision();
    Action action = unwrappedResult.action();

    if ((oldElement == newElement) && (oldWrappedPrecision == newPrecision)) {
      // nothing has changed
      return Optional.of(PrecisionAdjustmentResult.create(pElement, oldPrecision, action));
    }

    UsageState resultElement = element.clone(newElement);

    return Optional.of(PrecisionAdjustmentResult.create(resultElement, ((UsagePrecision)oldPrecision).clone(newPrecision), action));
  }
}
