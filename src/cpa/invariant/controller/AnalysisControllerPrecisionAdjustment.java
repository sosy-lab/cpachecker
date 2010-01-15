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
package cpa.invariant.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import common.Pair;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;

/**
 * @author g.theoduloz
 */
public class AnalysisControllerPrecisionAdjustment implements
    PrecisionAdjustment {

  public AnalysisControllerPrecisionAdjustment()
  {
  }
  
  /** Iterable class over a selected data within an element */
  private static class ProjectionIterable<AE extends AbstractElement>
    implements Iterable<StopHeuristicsData>
  {
    private final Iterable<Pair<AE, Precision>> elements;
    private final int selectedData;
    
    /** Iterator class */
    private class ProjectionIterator
      implements Iterator<StopHeuristicsData>
    {
      private final Iterator<Pair<AE, Precision>> innerIterator;
      
      public ProjectionIterator() {
        innerIterator = elements.iterator();
      }
      
      @Override
      public boolean hasNext() {
        return innerIterator.hasNext();
      }
      
      @Override
      public StopHeuristicsData next() {
        AnalysisControllerElement nextElement = (AnalysisControllerElement) innerIterator.next().getFirst(); 
        return nextElement.getComponents().get(selectedData);
      }
      
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
    
    public ProjectionIterable(Iterable<Pair<AE, Precision>> base, int select)
    {
      elements = base;
      selectedData = select;
    }
    
    @Override
    public Iterator<StopHeuristicsData> iterator() {
      return new ProjectionIterator();
    }
  }
  
  @Override
  public Pair<AbstractElement, Precision> prec(AbstractElement el,
      Precision p, Collection<Pair<AbstractElement, Precision>> reached) {
    AnalysisControllerElement element = (AnalysisControllerElement) el;
    
    List<StopHeuristicsData> preData = element.getComponents();
    List<StopHeuristicsData> postData = new ArrayList<StopHeuristicsData>(preData.size());
    
    int idx = 0;
    for (StopHeuristicsData d : preData) {
      postData.add(d.collectData(new ProjectionIterable<AbstractElement>(reached, idx)));
      idx++;
    }
    
    return new Pair<AbstractElement, Precision>(new AnalysisControllerElement(postData), p);
  }

}
