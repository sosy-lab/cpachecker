/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package compositeCPA;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import common.Pair;

import cpa.common.UnmodifiableReachedElements;
import cpa.common.UnmodifiableReachedElementsView;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;

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
   * @see cpa.common.interfaces.PrecisionAdjustment#prec(cpa.common.interfaces.AbstractElement, cpa.common.interfaces.Precision, java.util.Collection)
   */
  public Pair<AbstractElement, Precision> prec(AbstractElement pElement,
                                               Precision pPrecision,
                                               UnmodifiableReachedElements pElements) {
    CompositeElement comp = (CompositeElement) pElement;
    CompositePrecision prec = (CompositePrecision) pPrecision;
    assert (comp.getElements().size() == prec.getPrecisions().size());
    int dim = comp.getElements().size();
    
    List<AbstractElement> outElements = new ArrayList<AbstractElement>();
    List<Precision> outPrecisions = new ArrayList<Precision>();
    
    for (int i = 0; i < dim; ++i) {
      UnmodifiableReachedElements slice =
        new UnmodifiableReachedElementsView(pElements, elementProjectionFunctions.get(i), precisionProjectionFunctions.get(i));
      PrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i); 
      Pair<AbstractElement,Precision> out = precisionAdjustment.prec(comp.get(i), prec.get(i), slice);
      outElements.add(out.getFirst());
      outPrecisions.add(out.getSecond());
    }      
    
    // TODO for now we just take the input call stack, that may be wrong, but how to construct 
    // a proper one in case this _is_ wrong?
    return new Pair<AbstractElement, Precision>(new CompositeElement(outElements, comp.getCallStack()),
        new CompositePrecision(outPrecisions));
  }

}
