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

import java.util.List;

public interface FaultLocalizationHeuristic<I extends FaultLocalizationOutput> {

  /**
   * Rank the input set for visualizing in the ReportManager.
   *
   * <p>This is a FunctionalInterface. This enables the possibility to write your own heuristics as
   * follows: l -> { your ranking algorithm return rankedList; } The above statement can simply be
   * passed as parameter. If more than just one parameter is needed (here: result) a class that
   * implements this interface can be created. For more details and an example see
   * ErrorLocationFarthestHeuristic. If you want to have more heuristics applied you can concatenate
   * them by using FaultLocalizationHeuristic.concatHeuristics() This will return a new interface
   * concatenating all passed heuristics and finally rank the objects by the average score. For more
   * details take a look at the documentation of FaultLocalizationHeuristicImpl
   *
   * @param result The result of any FaultLocalizationAlgorithm
   * @return a ranked list of all contained FaultLocalizationOutput objects.
   */
  List<I> rank(ErrorIndicatorSet<I> result);
}
