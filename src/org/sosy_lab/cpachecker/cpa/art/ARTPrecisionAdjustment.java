/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.core.UnmodifiableReachedElements;
import org.sosy_lab.cpachecker.core.UnmodifiableReachedElementsView;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;

public class ARTPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecAdjustment;

  public ARTPrecisionAdjustment(PrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }

  @Override
  public Pair<AbstractElement, Precision> prec(AbstractElement pElement,
      Precision oldPrecision, UnmodifiableReachedElements pElements) {

    Preconditions.checkArgument(pElement instanceof ARTElement);
    ARTElement element = (ARTElement)pElement;

    UnmodifiableReachedElements elements = new UnmodifiableReachedElementsView(
        pElements,  ARTElement.getUnwrapFunction(), Functions.<Precision>identity());

    AbstractElement oldElement = element.getWrappedElement();
    
    Pair<AbstractElement, Precision> unwrappedResult = wrappedPrecAdjustment.prec(oldElement, oldPrecision, elements);

    if (unwrappedResult == null) {
      // element is not reachable
      return null;
    }

    AbstractElement newElement = unwrappedResult.getFirst();
    Precision newPrecision = unwrappedResult.getSecond();

    if ((oldElement == newElement) && (oldPrecision == newPrecision)) {
      // nothing has changed
      return new Pair<AbstractElement, Precision>(pElement, oldPrecision);
    }
      
    ARTElement resultElement = new ARTElement(newElement, null);

    for (ARTElement parent : element.getParents()) {
      resultElement.addParent(parent);
    }
    for (ARTElement child : element.getChildren()) {
      resultElement.addParent(child);
    }

    // first copy list of covered elements, then remove element from ART, then set elements covered by new element
    ImmutableList<ARTElement> coveredElements = ImmutableList.copyOf(element.getCoveredByThis());
    element.removeFromART();

    for (ARTElement covered : coveredElements) {
      covered.setCovered(resultElement);
    }

    return new Pair<AbstractElement, Precision>(resultElement, newPrecision);
  }
}