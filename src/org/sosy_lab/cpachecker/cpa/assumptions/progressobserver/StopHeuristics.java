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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.heuristics.HeuristicPrecision;

/**
 * A stopping heuristics for the controller CPA. A class implementing
 * stop heuristics must provide a constructor of the form
 * <code>XXXHeuristics(CPAConfiguration, LogManager)</code> to create
 * an instance of the heuristics with the given configuration
 * and logger.
 */
public interface StopHeuristics <D extends StopHeuristicsData> {
  /** Get the initial data */
  public D getInitialData(CFANode node);

  /** Collect data with respect to the given set of reached states */
  public D collectData(StopHeuristicsData data, ReachedHeuristicsDataSetView reached);

  /** Process an edge and update the data */
  public D processEdge(StopHeuristicsData data, CFAEdge edge);

  public HeuristicPrecision getInitialPrecision();
}
