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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;

/**
 * @author g.theoduloz
 */
public class RepetitionsInPathHeuristicsData implements StopHeuristicsData {
  
  private static long threshold = -1;
  
  public static void setThreshold(long newThreshold)
  {
    threshold = newThreshold;
  }
  
  public static long getThreshold()
  {
    return threshold;
  }
  
  private final Map<CFAEdge, Integer> frequencyMap;
  
  public RepetitionsInPathHeuristicsData()
  {
    frequencyMap = new HashMap<CFAEdge, Integer>();
  }
  
  /** The given map is copied to the new object's map */
  public RepetitionsInPathHeuristicsData(Map<CFAEdge, Integer> map)
  {
    frequencyMap = new HashMap<CFAEdge, Integer>(map);
  }
  
  @Override
  public StopHeuristicsData collectData(ReachedHeuristicsDataSetView pReached) {
    return this;
  }

  @Override
  public boolean isBottom() {
    return false;
  }

  @Override
  public boolean isLessThan(StopHeuristicsData pD) {
    return false;
  }

  @Override
  public boolean isTop() {
    return frequencyMap.isEmpty();
  }

  private boolean isInteresting(CFAEdge edge)
  {
    return (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge)
        || (edge.getPredecessor().isLoopStart());
  }
  
  @Override
  public StopHeuristicsData processEdge(CFAEdge edge) {
    if (!isInteresting(edge)) return this;
    
    Integer newValueInTable = frequencyMap.get(edge);
    int newValue = (newValueInTable == null) ? 1 : newValueInTable.intValue();
    if (newValue > threshold)
      return BOTTOM;
    else {
      RepetitionsInPathHeuristicsData result = copy();
      result.frequencyMap.put(edge, newValue+1);
      return result;
    }
  }
  
  private RepetitionsInPathHeuristicsData copy() {
    return new RepetitionsInPathHeuristicsData(frequencyMap);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (Entry<CFAEdge, Integer> entry : frequencyMap.entrySet()) {
      int count = entry.getValue();
      CFAEdge edge = entry.getKey();
      builder.append(count).append("x(").append(edge).append(") ");
    }
    return builder.toString();
  }
  
  public static final RepetitionsInPathHeuristicsData TOP = new RepetitionsInPathHeuristicsData() {
    @Override
    public boolean isTop() { return true; }
    @Override
    public StopHeuristicsData processEdge(CFAEdge edge) { return this; }
    @Override
    public String toString() { return "TOP"; }
  };
  
  public static final RepetitionsInPathHeuristicsData BOTTOM = new RepetitionsInPathHeuristicsData() {
    @Override
    public boolean isBottom() { return true; }
    @Override
    public StopHeuristicsData processEdge(CFAEdge edge) { return this; }
    @Override
    public String toString() { return "BOTTOM"; }
  };
  
}
