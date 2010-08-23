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
package org.sosy_lab.cpachecker.cpa.composite;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CompositePrecisionAdjustment implements PrecisionAdjustment {

  private final ImmutableList<PrecisionAdjustment> precisionAdjustments;
  private final ImmutableList<ElementProjectionFunction> elementProjectionFunctions;
  private final ImmutableList<PrecisionProjectionFunction> precisionProjectionFunctions;


  public CompositePrecisionAdjustment(ImmutableList<PrecisionAdjustment> precisionAdjustments) {
    this.precisionAdjustments = precisionAdjustments;

    ImmutableList.Builder<ElementProjectionFunction> elementProjectionFunctions = ImmutableList.builder();
    ImmutableList.Builder<PrecisionProjectionFunction> precisionProjectionFunctions = ImmutableList.builder();

    for (int i = 0; i < precisionAdjustments.size(); i++) {
      elementProjectionFunctions.add(new ElementProjectionFunction(i));
      precisionProjectionFunctions.add(new PrecisionProjectionFunction(i));
    }
    this.elementProjectionFunctions = elementProjectionFunctions.build();
    this.precisionProjectionFunctions = precisionProjectionFunctions.build();
  }

  private static class ElementProjectionFunction
    implements Function<AbstractElement, AbstractElement>
  {
    private final int dimension;

    public ElementProjectionFunction(int d) {
      dimension = d;
    }

    @Override
    public AbstractElement apply(AbstractElement from) {
      return ((CompositeElement)from).get(dimension);
    }
  }

  private static class PrecisionProjectionFunction
  implements Function<Precision, Precision>
  {
    private final int dimension;

    public PrecisionProjectionFunction(int d) {
      dimension = d;
    }

    @Override
    public Precision apply(Precision from) {
      return ((CompositePrecision)from).get(dimension);
    }
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment#prec(org.sosy_lab.cpachecker.core.interfaces.AbstractElement, org.sosy_lab.cpachecker.core.interfaces.Precision, java.util.Collection)
   */
  @Override
  public Pair<AbstractElement, Precision> prec(AbstractElement pElement,
                                               Precision pPrecision,
                                               UnmodifiableReachedSet pElements) {
    CompositeElement comp = (CompositeElement) pElement;
    CompositePrecision prec = (CompositePrecision) pPrecision;
    assert (comp.getElements().size() == prec.getPrecisions().size());
    int dim = comp.getElements().size();

    ImmutableList.Builder<AbstractElement> outElements = ImmutableList.builder();
    ImmutableList.Builder<Precision> outPrecisions = ImmutableList.builder();

    boolean modified = false;

    for (int i = 0; i < dim; ++i) {
      UnmodifiableReachedSet slice =
        new UnmodifiableReachedSetView(pElements, elementProjectionFunctions.get(i), precisionProjectionFunctions.get(i));
      PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      AbstractElement oldElement = comp.get(i);
      Precision oldPrecision = prec.get(i);
      Pair<AbstractElement,Precision> out = precisionAdjustment.prec(oldElement, oldPrecision, slice);
      if (out == null) {
        // element is not reachable
        return null;
      }
      AbstractElement newElement = out.getFirst();
      Precision newPrecision = out.getSecond();
      if ((newElement != oldElement) || (newPrecision != oldPrecision)) {
        // something has changed
        modified = true;
      }
      outElements.add(newElement);
      outPrecisions.add(newPrecision);
    }

    if (modified) {
      // TODO for now we just take the input call stack, that may be wrong, but how to construct
      // a proper one in case this _is_ wrong?
      return new Pair<AbstractElement, Precision>(new CompositeElement(outElements.build()),
          new CompositePrecision(outPrecisions.build()));
    } else {
      return new Pair<AbstractElement, Precision>(pElement, pPrecision);
    }
  }

}
