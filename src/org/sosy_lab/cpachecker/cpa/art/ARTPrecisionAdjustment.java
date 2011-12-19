/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.art;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;

public class ARTPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecAdjustment;

  public ARTPrecisionAdjustment(PrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }

  @Override
  public Triple<AbstractElement, Precision, Action> prec(AbstractElement pElement,
      Precision oldPrecision, UnmodifiableReachedSet pElements) throws CPAException {

    Preconditions.checkArgument(pElement instanceof ARTElement);
    ARTElement element = (ARTElement)pElement;

    UnmodifiableReachedSet elements = new UnmodifiableReachedSetView(
        pElements,  ARTElement.getUnwrapFunction(), Functions.<Precision>identity());

    AbstractElement oldElement = element.getWrappedElement();

    Triple<AbstractElement, Precision, Action> unwrappedResult = wrappedPrecAdjustment.prec(oldElement, oldPrecision, elements);

    AbstractElement newElement = unwrappedResult.getFirst();
    Precision newPrecision = unwrappedResult.getSecond();
    Action action = unwrappedResult.getThird();

    if ((oldElement == newElement) && (oldPrecision == newPrecision)) {
      // nothing has changed
      return new Triple<AbstractElement, Precision, Action>(pElement, oldPrecision, action);
    }

    ARTElement resultElement = new ARTElement(newElement, null);

    element.replaceInARTWith(resultElement); // this completely eliminates element

    return new Triple<AbstractElement, Precision, Action>(resultElement, newPrecision, action);
  }
}