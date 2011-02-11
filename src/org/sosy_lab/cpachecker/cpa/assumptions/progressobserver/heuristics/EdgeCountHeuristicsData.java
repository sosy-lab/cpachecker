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
package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.heuristics;

import com.google.common.base.Function;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.ReachedHeuristicsDataSetView;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula.PreventingHeuristicType;

/**
 * @author g.theoduloz
 */
public class EdgeCountHeuristicsData
  implements StopHeuristicsData {

  private final int[] counters;
  private final CFANode node;
  private boolean untouched;

  private static int threshold = -1;
  
  private Pair<PreventingHeuristicType, Long> preventingCondition = null;

  public static void setBaseThreshold(int newThreshold)
  {
    threshold = newThreshold;
  }

  public static int getBaseThreshold()
  {
    return threshold;
  }

  public EdgeCountHeuristicsData(CFANode sourceNode)
  {
    assert sourceNode != null;
    node = sourceNode;
    counters = new int[sourceNode.getNumLeavingEdges()];
    untouched = true;
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
   * processEdge relies on side-effect to update the pre, because
   * prec has access only to pre-states, while the edge is only
   * known when computing the post state.
   */
  public EdgeCountHeuristicsData updateForEdge(CFAEdge edge, Function<? super CFAEdge, Integer> thresholds) {
    assert edge.getPredecessor() == node;

    for (int i=0; i < counters.length; i++)
      if (node.getLeavingEdge(i) == edge) {
        counters[i]++;
        // Threshold exceeded?
        Integer threshold = thresholds.apply(edge);
        if ((threshold != null) && (counters[i] >= threshold.intValue())) {
          preventingCondition = Pair.of(PreventingHeuristicType.EDGECOUNT, (long)threshold);
          return BOTTOM;
        }
      }

    return new EdgeCountHeuristicsData(edge.getSuccessor());
  }

  private boolean isInteresting()
  {
    if (node.isLoopStart())
      return true;
    for (int i = 0; i < node.getNumLeavingEdges(); i++)
    {
      CFAEdge edge = node.getLeavingEdge(i);
      if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge)
        return true;
    }
    return false;
  }

  public EdgeCountHeuristicsData collectData(ReachedHeuristicsDataSetView reached) {
    if (isTop() || isBottom() || !isInteresting()) return this;

    for (StopHeuristicsData d : reached.getHeuristicsDataForLocation(node)) {
      EdgeCountHeuristicsData other = (EdgeCountHeuristicsData) d;
      if (other.untouched && (other != this) && (other.node == node)) {
        for (int i=0; i < counters.length; i++) {
          // The 'untouched trick' computes the maximum more efficiently
          // counters[i] = Math.max(counters[i], other.counters[i]);
          counters[i] = other.counters[i];
        }
        other.untouched = false;
        break;
      }
    }
    return this;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
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
    public EdgeCountHeuristicsData updateForEdge(CFAEdge edge, Function<? super CFAEdge,Integer> thresholds) {
      return this;
    }

    @Override
    public EdgeCountHeuristicsData collectData(ReachedHeuristicsDataSetView reached) {
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
    public EdgeCountHeuristicsData updateForEdge(CFAEdge edge, Function<? super CFAEdge,Integer> thresholds) {
      return new EdgeCountHeuristicsData(edge.getSuccessor());
    }

    @Override
    public EdgeCountHeuristicsData collectData(ReachedHeuristicsDataSetView reached) {
      return this;
    }

    @Override
    public String toString() { return "TOP"; }

    @Override
    public boolean equals(Object obj) { return obj == this; }
  };

  @Override
  public boolean shouldTerminateAnalysis() {
    return false;
  }

  @Override
  public Pair<PreventingHeuristicType, Long> getPreventingCondition() {
    return preventingCondition;
  }

}
