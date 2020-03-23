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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicatorSet;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationOutput;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationReason;

public class SubsetSizeHeuristic<I extends FaultLocalizationOutput> implements FaultLocalizationHeuristic<I> {

  @Override
  public List<I> rank(ErrorIndicatorSet<I> result) {

    Map<Set<I>, Integer> mapToSize = new HashMap<>();
    for(Set<I> set: result){
      mapToSize.put(set, set.size());
    }

    List<Integer> distinctSizes = mapToSize.values()
        .stream()
        .mapToInt(Integer::intValue)
        .distinct()
        .sorted()
        .boxed()
        .collect(Collectors.toList());

    List<I> ranked = new ArrayList<>();
    if(distinctSizes.isEmpty()){
      return ranked;
    }

    int minSize = distinctSizes.get(0);

    Map<I, Double> mapLikelihood = new HashMap<>();

    for(Set<I> set: result){
      for(I temp: set){
        mapLikelihood.merge(temp, 100d/Math.pow(2, set.size()-minSize), Double::sum);
      }
    }

    double sum = mapLikelihood.values().stream().mapToDouble(Double::doubleValue).sum();

    Map<I, Double> mapPercentage = new HashMap<>();
    for (I i : mapLikelihood.keySet()) {
      double likelihood = mapLikelihood.get(i)/sum;
      i.addReason(new FaultLocalizationReason<>("Occurrence ins subsets.", likelihood));
      mapPercentage.put(i, likelihood);
    }

    ranked.addAll(mapPercentage.keySet());
    ranked.sort(Comparator.comparingDouble(mapLikelihood::get));
    Collections.reverse(ranked);

    return ranked;
  }
}
