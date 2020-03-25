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

import com.google.common.base.Functions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicatorSet;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristicImpl;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationOutput;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationReason;

public class OverallAppearanceHeuristic<I extends FaultLocalizationOutput> implements
                                                                           FaultLocalizationHeuristic<I> {
  @Override
  public Map<I, Integer> rank(ErrorIndicatorSet<I> result) {
    List<I> selectors = new ArrayList<>(FaultLocalizationHeuristicImpl.condenseErrorIndicatorSet(result));

    Map<I, Long> map =
        selectors.stream()
            .collect(Collectors.groupingBy(Functions.identity(), Collectors.counting()));

    Map<I, Double> mapToScore = new HashMap<>();
    long sum = map.values().stream().mapToLong(pLong -> pLong).sum();
    map.keySet()
        .forEach(
            l -> {
              FaultLocalizationReason reason = new FaultLocalizationReason("Overall occurence.");
              double likelihood = (double) map.get(l) / (double) sum;
              reason.setLikelihood(likelihood);
              mapToScore.put(l, likelihood);
              l.addReason(reason);
            });

    return FaultLocalizationHeuristicImpl.scoreToRankMap(mapToScore);
  }
}
