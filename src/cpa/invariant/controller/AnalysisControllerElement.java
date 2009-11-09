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
import java.util.Iterator;
import java.util.List;

import cfa.objectmodel.CFANode;

import cpa.common.interfaces.AbstractElement;

/**
 * Class to represent the abstract element of the analysis controller.
 * @author g.theoduloz
 */
public class AnalysisControllerElement implements AbstractElement {
  
  private final List<StopHeuristicsData> data;
  
  /** Bottom */
  public static final AnalysisControllerElement getBottom(AnalysisControllerCPA cpa)
  {
    ArrayList<StopHeuristicsData> data = new ArrayList<StopHeuristicsData>(cpa.getEnabledHeuristics().size());
    for (StopHeuristics<? extends StopHeuristicsData> h : cpa.getEnabledHeuristics()) {
      data.add(h.getBottom());
    }
    return new AnalysisControllerElement(data);
  }
    
  /** Top */
  public static final AnalysisControllerElement getTop(AnalysisControllerCPA cpa)
  {
    ArrayList<StopHeuristicsData> data = new ArrayList<StopHeuristicsData>(cpa.getEnabledHeuristics().size());
    for (StopHeuristics<? extends StopHeuristicsData> h : cpa.getEnabledHeuristics()) {
      data.add(h.getTop());
    }
    return new AnalysisControllerElement(data);
  }
  
  /** Initial */
  public static final AnalysisControllerElement getInitial(AnalysisControllerCPA cpa, CFANode node)
  {
    return new AnalysisControllerElement(cpa, node);
  }
  
  @Override
  public boolean isError() {
    return false;
  }

  public AnalysisControllerElement(AnalysisControllerCPA a, CFANode node)
  {
    this(new ArrayList<StopHeuristicsData>(a.getEnabledHeuristics().size()));
    
    for (StopHeuristics<? extends StopHeuristicsData> h : a.getEnabledHeuristics()) {
      data.add(h.getInitialData(node));
    }
  }
  
  AnalysisControllerElement(List<StopHeuristicsData> d)
  {
    data = d;
  }
  
  public List<StopHeuristicsData> getComponents()
  {
    return data;
  }
  
  /** Is this element less than the given element? */
  public boolean isLessThan(AnalysisControllerElement other)
  {
    Iterator<StopHeuristicsData> it1 = data.iterator();
    Iterator<StopHeuristicsData> it2 = other.data.iterator();
    
    while (it1.hasNext()) {
      assert it2.hasNext();
      if (!it1.next().isLessThan(it2.next()))
        return false;
    }
    assert !it2.hasNext();
    
    return true;
  }
  
  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    
    if (other instanceof AnalysisControllerElement) {
      AnalysisControllerElement o = (AnalysisControllerElement) other;
      boolean bottom1 = false;
      boolean bottom2 = false;
      boolean mismatch = false;
      
      Iterator<StopHeuristicsData> it1 = data.iterator();
      Iterator<StopHeuristicsData> it2 = o.data.iterator();
      while (it1.hasNext()) {
        if (!it2.hasNext()) return false;
        
        StopHeuristicsData d1 = it1.next();
        StopHeuristicsData d2 = it2.next();
        
        if (!bottom1 && d1.isBottom()) {
          if (bottom2 || d2.isBottom()) {
            return true;
          } else {
            bottom1 = true;
            mismatch = true;
          }
        } else if (!bottom2 && d2.isBottom()) {
          if (bottom1) {
            return true;
          } else {
            bottom2 = true;
            mismatch = true;
          }
        } else {
          if ((!mismatch) && (!d1.equals(d2)))
            mismatch = true;
        }
      }
      if (it2.hasNext()) return false;
      return !mismatch;
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    boolean first = true;
    for (StopHeuristicsData d : data) {
      if (first)
        first = false;
      else
        buffer.append('\n');
      buffer.append(d.getClass().getSimpleName()).append(": ").append(d.toString());
    }
    return buffer.toString();
  }
 
}
