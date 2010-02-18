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
package cpa.assumptions.collector.progressobserver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

import common.Pair;

import cpa.common.UnmodifiableReachedElements;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;

/**
 * @author g.theoduloz
 */
public class ProgressObserverPrecisionAdjustment implements
    PrecisionAdjustment {

  private final ImmutableList<StopHeuristics<? extends StopHeuristicsData>> heuristics;
  
  public ProgressObserverPrecisionAdjustment(ProgressObserverCPA aCPA)
  {
    heuristics = aCPA.getEnabledHeuristics();
  }
  
  /** Projection function class over a selected data within an element */
  private static class ProjectionFunction
    implements Function<AbstractElement, StopHeuristicsData>
  {
    private final int dimension;
    
    public ProjectionFunction(int d) {
      dimension = d;
    }
    
    @Override
    public StopHeuristicsData apply(AbstractElement from) {
      ProgressObserverElement element = (ProgressObserverElement) from; 
      return element.getComponents().get(dimension);
    }
  }
  
  @Override
  public Pair<AbstractElement, Precision> prec(AbstractElement el,
      Precision p, UnmodifiableReachedElements reached) {
    ProgressObserverElement element = (ProgressObserverElement) el;
    
    List<StopHeuristicsData> preData = element.getComponents();
    List<StopHeuristicsData> postData = new ArrayList<StopHeuristicsData>(preData.size());
    
    Iterator<StopHeuristics<? extends StopHeuristicsData>> heuristicsIt = heuristics.iterator();
    Iterator<StopHeuristicsData> preIt = preData.iterator();
    int idx = 0;
    while (preIt.hasNext()) {
      StopHeuristics<? extends StopHeuristicsData> h = heuristicsIt.next();
      StopHeuristicsData d = preIt.next();
      ReachedHeuristicsDataSetView slice = new ReachedHeuristicsDataSetView(reached, new ProjectionFunction(idx)); 
      postData.add(h.collectData(d, slice));
      idx++;
    }
    
    return new Pair<AbstractElement, Precision>(new ProgressObserverElement(postData), p);
  }

}
