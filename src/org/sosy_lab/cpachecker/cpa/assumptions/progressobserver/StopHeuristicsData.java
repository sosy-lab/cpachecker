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
package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver;

import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula.PreventingHeuristicType;


/**
 * Data that needs to be tracked by a stopping heuristics
 * @author g.theoduloz
 */
public interface StopHeuristicsData {
  public boolean isBottom();
  public boolean isTop();
  public boolean isLessThan(StopHeuristicsData d);
  // should we stop analysis when we hit bottom
  // this returns true when time or memory limit for analysis
  // is hit
  public boolean shouldTerminateAnalysis();
  public long getThreshold();
  public PreventingHeuristicType getHeuristicType();
}
