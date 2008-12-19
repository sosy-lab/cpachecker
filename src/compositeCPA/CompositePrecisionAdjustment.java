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
/**
 * 
 */
package compositeCPA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import common.Pair;

import cpa.common.CompositeElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CompositePrecisionAdjustment implements PrecisionAdjustment {

  private List<PrecisionAdjustment> precisionAdjustments;
  
  public CompositePrecisionAdjustment (List<PrecisionAdjustment> precisionAdjustments) {
    this.precisionAdjustments = precisionAdjustments;
  }
  
  /* (non-Javadoc)
   * @see cpa.common.interfaces.PrecisionAdjustment#prec(cpa.common.interfaces.AbstractElement, cpa.common.interfaces.Precision, java.util.Collection)
   */
  public <AE extends AbstractElement> Pair<AE, Precision> prec(
                                                               AE pElement,
                                                               Precision pPrecision,
                                                               Collection<Pair<AE, Precision>> pElements) {
    CompositeElement comp = (CompositeElement) pElement;
    CompositePrecision prec = (CompositePrecision) pPrecision;
    assert (comp.getElements().size() == prec.getPrecisions().size());
    int dim = comp.getElements().size();
    
    List<AbstractElement> outElements = new ArrayList<AbstractElement>();
    List<Precision> outPrecisions = new ArrayList<Precision>();
    
    for (int i = 0; i < dim; ++i) {
      HashSet<Pair<AbstractElement,Precision>> slice = new HashSet<Pair<AbstractElement,Precision>>();
      for (Pair<AE,Precision> entry : pElements) {
        slice.add(new Pair<AbstractElement,Precision>(((CompositeElement)entry.getFirst()).get(i),
            ((CompositePrecision)entry.getSecond()).get(i)));
      }
      Pair<AbstractElement,Precision> out = precisionAdjustments.get(i).prec(comp.get(i), prec.get(i), slice);
      outElements.add(out.getFirst());
      outPrecisions.add(out.getSecond());
    }
    
    // TODO for now we just take the input call stack, that may be wrong, but how to construct 
    // a proper one in case this _is_ wrong?
    return new Pair<AE,Precision>((AE) new CompositeElement(outElements, comp.getCallStack()),
        new CompositePrecision(outPrecisions));
  }

}
