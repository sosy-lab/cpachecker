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

/**
 * @author g.theoduloz
 */
public abstract class ReachedSizeHeuristicsData implements StopHeuristicsData {

  private static long threshold = -1;
  
  public static void setThreshold(long newThreshold)
  {
    threshold = newThreshold;
  }
  
  public static long getThreshold()
  {
    return threshold;
  }
  
  @Override
  public StopHeuristicsData processEdge(CFAEdge pEdge) {
    return this;
  }
  
  @Override
  public boolean equals(Object other) {
    return other == this;
  }
  
  public static final ReachedSizeHeuristicsData TOP = new ReachedSizeHeuristicsData() {
    @Override
    public boolean isTop() { return true; }
    @Override
    public boolean isLessThan(StopHeuristicsData other) {
      return other == TOP;
    }
    @Override
    public boolean isBottom() { return false; }
    @Override
    public StopHeuristicsData collectData(ReachedHeuristicsDataSetView pReached) {
      if (pReached.getHeuristicsData().size() > threshold)
        return BOTTOM;
      else
        return this;
    }
  };

  public static final ReachedSizeHeuristicsData BOTTOM = new ReachedSizeHeuristicsData() {
    @Override
    public boolean isTop() { return false; }
    @Override
    public boolean isLessThan(StopHeuristicsData other) {
      return true;
    }
    @Override
    public boolean isBottom() { return true; }
    @Override
    public StopHeuristicsData collectData(ReachedHeuristicsDataSetView pReached) {
      return this;
    }
  };
  
}
