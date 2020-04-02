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
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicator;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicatorSet;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristicUtils;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationOutput;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationReason;

/**
 * This heuristic adds hint to edges.
 * Hints are not regarded when calculating the score.
 * Useful for delivering important information to the user.
 */
public class SingleEdgeHintHeuristic<I extends FaultLocalizationOutput> implements
                                                                        FaultLocalizationHeuristic<I> {

  @Override
  public Map<I, Integer> rank(ErrorIndicatorSet<I> result) {
    Set<I> condensedSet = FaultLocalizationHeuristicUtils.condenseErrorIndicatorSet(result);
    Map<I, Integer> rank = new HashMap<>();
    for(I temp: condensedSet){
      FaultLocalizationReason reason = FaultLocalizationReason.hintFor(new ErrorIndicator<>(temp));
      temp.addReason(reason);
      rank.put(temp, 1);
    }
    return rank;
  }

}
