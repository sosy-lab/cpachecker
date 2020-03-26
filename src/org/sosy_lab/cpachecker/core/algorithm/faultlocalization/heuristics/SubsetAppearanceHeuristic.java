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
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicatorSet;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristicUtils;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationOutput;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationReason;

public class SubsetAppearanceHeuristic<I extends FaultLocalizationOutput> implements
                                                                          FaultLocalizationHeuristic<I> {
  @Override
  public Map<I, Integer> rank(ErrorIndicatorSet<I> result) {
    Map<Set<I>, Integer> map = new HashMap<>();
    for (Set<I> selectors : result) {
      for (Set<I> set : result) {
        if (selectors.containsAll(set)) {
          map.merge(set, 1, Integer::sum);
        }
      }
    }

    int totalOccurrences = map.values().stream().mapToInt(Integer::intValue).sum();
    Map<I, Double> mapLikelihood = new HashMap<>();

    for (Set<I> subset : map.keySet()) {
      for (I temp : subset) {
        FaultLocalizationReason reason = new FaultLocalizationReason("Overall subset occurrence.");
        double likelihood = ((double)map.get(subset))/totalOccurrences;
        reason.setLikelihood(likelihood);
        mapLikelihood.put(temp, likelihood);
        temp.addReason(reason);
      }
    }

    return FaultLocalizationHeuristicUtils.scoreToRankMap(mapLikelihood);
  }
}
