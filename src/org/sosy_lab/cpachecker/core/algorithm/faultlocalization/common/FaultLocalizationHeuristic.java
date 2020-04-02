/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common;

import java.util.Map;

public interface FaultLocalizationHeuristic<I extends FaultLocalizationOutput> {

  /**
   * Rank the input set for visualizing in the ReportManager.
   *
   * If more than just one parameter is needed (here: result) a class that
   * implements this interface can be created.
   * For more details and an example see ErrorLocationFarthestHeuristic.
   * To concatenate multiple heuristics FaultLocalizationHeuristicUtils.concatHeuristics() can be used.
   *
   * @param result The result of any FaultLocalizationAlgorithm
   * @return a ranked list of all contained FaultLocalizationOutput objects.
   */
  Map<I, Integer> rank(ErrorIndicatorSet<I> result);
}
