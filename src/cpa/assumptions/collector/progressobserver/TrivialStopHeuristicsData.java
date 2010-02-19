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

/**
 * Trivial heuristics data to represent only top and bottom.
 * @author g.theoduloz
 */
public class TrivialStopHeuristicsData implements StopHeuristicsData {

  private final boolean bottom;
  
  /** Instances are only accessible via TOP/BOTTOM */
  private TrivialStopHeuristicsData(boolean isBottom) {
    bottom = isBottom;
  }
  
  public static final TrivialStopHeuristicsData TOP = new TrivialStopHeuristicsData(false);
  public static final TrivialStopHeuristicsData BOTTOM = new TrivialStopHeuristicsData(true);
  
  @Override
  public boolean isBottom() {
    return bottom;
  }
  
  @Override
  public boolean isTop() {
    return !bottom;
  }

  @Override
  public boolean isLessThan(StopHeuristicsData other) {
    if (this == BOTTOM) return true;
    if (other == TOP) return true;
    return (this == other);
  }

  @Override
  public String toString() {
    if (bottom)
      return "BOTTOM";
    else
      return "TOP";
  }
  
}
