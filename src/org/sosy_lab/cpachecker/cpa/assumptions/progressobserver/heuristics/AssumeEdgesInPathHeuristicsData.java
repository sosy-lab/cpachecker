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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula.PreventingHeuristicType;

public class AssumeEdgesInPathHeuristicsData implements StopHeuristicsData {

  protected static long threshold = -1;
  protected static long maxNoOfAssumeEdges;

  private final int noOfAssumeEdges;

  public AssumeEdgesInPathHeuristicsData(int noOfAssumeEdges){
    this.noOfAssumeEdges = noOfAssumeEdges;
  }

  public AssumeEdgesInPathHeuristicsData()
  {
    noOfAssumeEdges = -1;
  }

  public AssumeEdgesInPathHeuristicsData updateForEdge(StopHeuristicsData pData, int pThreshold, CFAEdge pEdge){

    int newValue = (((AssumeEdgesInPathHeuristicsData)pData).noOfAssumeEdges);

    if(pEdge.getEdgeType() == CFAEdgeType.AssumeEdge){

      newValue++;
      maxNoOfAssumeEdges = Math.max(maxNoOfAssumeEdges, newValue);
      if ((pThreshold > 0) && (newValue > pThreshold)){
        setThreshold(pThreshold);
        return BOTTOM;
      }
    }

    return new AssumeEdgesInPathHeuristicsData(newValue);
  }

  public static final AssumeEdgesInPathHeuristicsData BOTTOM = new AssumeEdgesInPathHeuristicsData() {
    @Override
    public boolean isBottom() { return true; }
    @Override
    public AssumeEdgesInPathHeuristicsData updateForEdge(StopHeuristicsData pData, int pThreshold, CFAEdge pEdge) { return this; }
    @Override
    public String toString() { return "BOTTOM"; }
  };

  @Override
  public PreventingHeuristicType getHeuristicType() {
    return PreventingHeuristicType.ASSUMEEDGESINPATH;
  }

  public void setThreshold(long newThreshold)
  {
    threshold = newThreshold;
  }

  @Override
  public long getThreshold()
  {
    return threshold;
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
  public String toString() {
    return "Number of assume edges: " + noOfAssumeEdges;
  }

}
