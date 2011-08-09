/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula.PreventingHeuristicType;

import com.google.common.base.Function;

public class RepetitionsInPathHeuristicsData implements StopHeuristicsData {

  private static long threshold = -1;
  private static int maxNoOfRepetitions = 0;

  public void setThreshold(long newThreshold)
  {
    threshold = newThreshold;
  }

  @Override
  public long getThreshold()
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

  public RepetitionsInPathHeuristicsData updateForEdge(CFAEdge edge, Function<? super CFAEdge, Integer> thresholds) {
    if (!isInteresting(edge)) return this;

    Integer oldValueInTable = frequencyMap.get(edge);
    int newValue = (oldValueInTable == null) ? 1 : (oldValueInTable.intValue() + 1);
    Integer threshold = thresholds.apply(edge);
    if ((threshold != null) && (newValue > threshold.intValue())){
      setThreshold(threshold.intValue());
      return BOTTOM;
    }
    else {
      RepetitionsInPathHeuristicsData result = copy();
      maxNoOfRepetitions = Math.max(maxNoOfRepetitions, newValue);
      result.frequencyMap.put(edge, newValue);
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
    public RepetitionsInPathHeuristicsData updateForEdge(CFAEdge edge, Function<? super CFAEdge, Integer> thresholds) { return this; }
    @Override
    public String toString() { return "TOP"; }
  };

  public static final RepetitionsInPathHeuristicsData BOTTOM = new RepetitionsInPathHeuristicsData() {
    @Override
    public boolean isBottom() { return true; }
    @Override
    public RepetitionsInPathHeuristicsData updateForEdge(CFAEdge edge, Function<? super CFAEdge, Integer> thresholds) { return this; }
    @Override
    public String toString() { return "BOTTOM"; }
  };

  protected static int getMaxNumberOfRepetitions(){
    return maxNoOfRepetitions;
  }

  @Override
  public boolean shouldTerminateAnalysis() {
    return false;
  }

  @Override
  public PreventingHeuristicType getHeuristicType() {
    return PreventingHeuristicType.REPETITIONSINPATH;
  }

}
