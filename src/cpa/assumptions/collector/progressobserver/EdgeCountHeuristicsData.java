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

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;

/**
 * @author g.theoduloz
 */
public class EdgeCountHeuristicsData
  implements StopHeuristicsData {

  private final int[] counters;
  private final CFANode node; 

  public EdgeCountHeuristicsData(CFANode sourceNode)
  {
    assert sourceNode != null;
    node = sourceNode;
    counters = new int[sourceNode.getNumLeavingEdges()];
  }
  
  private EdgeCountHeuristicsData() {
    node = null;
    counters = null;
  }
  
  /** Copy constructor */
  public EdgeCountHeuristicsData(EdgeCountHeuristicsData d)
  {
    node = d.node;
    counters = d.counters.clone();
  }
  
  public EdgeCountHeuristicsData copy()
  {
    return new EdgeCountHeuristicsData(this);
  }
  
  @Override
  public boolean isBottom() {
    return false;
  }

  @Override
  public boolean isLessThan(StopHeuristicsData d) {
    return d == TOP;
  }

  @Override
  public boolean isTop() {
    return false;
  }
  
  /**
   * Function that returns the threshold for a given edge,
   * or -1 if there is no threshold for the given edge
   */
  private int getThreshold(CFAEdge edge)
  {
    return 100;
  }
  
  /**
   * processEdge relies on side-effect to update the pre, because
   * prec has access only to pre-states, while the edge is only
   * known when computing the post state.
   */
  @Override
  public StopHeuristicsData processEdge(CFAEdge edge) {
    assert edge.getPredecessor() == node;
    
    for (int i=0; i < counters.length; i++)
      if (node.getLeavingEdge(i) == edge) {
        counters[i]++;
        // Threshold exceeded!
        if (counters[i] >= getThreshold(edge)) {
          // TODO Output invariant, instead of simply setting to bottom
          return BOTTOM;
        }
      }
    
    return new EdgeCountHeuristicsData(edge.getSuccessor());
  }
  
  @Override
  public StopHeuristicsData collectData(Iterable<StopHeuristicsData> reached) {
    if (isTop() || isBottom()) return this;
    
    for (StopHeuristicsData d : reached) {
      EdgeCountHeuristicsData other = (EdgeCountHeuristicsData) d;
      if (other.node == node)
        for (int i=0; i < counters.length; i++)
          counters[i] = Math.max(counters[i], other.counters[i]);
    }
    return this;
  }
  
  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    for (int i=0; i < counters.length; i++)
      buffer.append(counters[i]).append("x(").append(node).append(',')
        .append(node.getLeavingEdge(i).getSuccessor()).append(") ");
    return buffer.toString();
  }
    
  /** Bottom singleton */
  public static final EdgeCountHeuristicsData BOTTOM = new EdgeCountHeuristicsData()
  {
    @Override
    public boolean isBottom() { return true; }
    
    @Override
    public boolean isLessThan(StopHeuristicsData d) { return true; }
    
    @Override
    public StopHeuristicsData processEdge(CFAEdge edge) {
      return this;
    }
    
    @Override
    public StopHeuristicsData collectData(Iterable<StopHeuristicsData> reached) {
      return this;
    }
    
    @Override
    public String toString() { return "BOTTOM"; }
    
    @Override
    public boolean equals(Object obj) { return obj == this; }
    
  };
  
  /** Top singleton */
  public static final EdgeCountHeuristicsData TOP = new EdgeCountHeuristicsData()
  {
    @Override
    public boolean isTop() { return true; }
    
    @Override
    public boolean isLessThan(StopHeuristicsData d) { return d == this; }
    
    @Override
    public StopHeuristicsData processEdge(CFAEdge edge) {
      return new EdgeCountHeuristicsData(edge.getSuccessor());
    }
    
    @Override
    public StopHeuristicsData collectData(Iterable<StopHeuristicsData> reached) {
      return this;
    }
    
    @Override
    public String toString() { return "TOP"; }
    
    @Override
    public boolean equals(Object obj) { return obj == this; }
  };

}
