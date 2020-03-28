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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics.IdentityHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics.OverallAppearanceHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics.SetIdentityHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics.SetSizeHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics.SubsetAppearanceHeuristic;

public class FaultLocalizationHeuristicUtils {

  /**
   * Sample heuristics for sorting FaultLocalizationOutput.
   *
   * @param ranking the RankingMode decides which heuristic will be applied
   * @param <I> Must be the same type as it is used in the FaultLocalizationInfo
   * @return a ranked list of all outputs.
   */
  public static <I extends FaultLocalizationOutput> FaultLocalizationHeuristic<I> rank(RankingMode ranking) {
    switch (ranking) {
      case SUBSET:
        return new SubsetAppearanceHeuristic<>();
      case OVERALL:
        return new OverallAppearanceHeuristic<>();
      case IDENTITY:
      default:
        return new IdentityHeuristic<>();
    }
  }

  /**
   * Sample heuristics for sorting ErrorIndicator.
   *
   * @param ranking the RankingMode decides which heuristic will be applied
   * @param <I> Must be the same type as it is used in the FaultLocalizationInfo
   * @return a heuristic
   */
  public static <I extends FaultLocalizationOutput> FaultLocalizationSetHeuristic<I> rankSets(SetRankingMode ranking) {
    switch (ranking) {
      case SUBSET_SIZE:
        return new SetSizeHeuristic<>();
      case IDENTITY:
      default:
        return new SetIdentityHeuristic<>();
    }
  }

  /**
   * Concatenate heuristics to optimize the result. Each heuristic can optionally assign a score to
   * the FaultLocalizationOutput. If more than one heuristic is used, the resulting list gets sorted
   * by the average value of all scores of an object. The higher the score the higher the rank
   *
   * <p>Example: Assume objects I,J to be objects that extend FaultLocalizationOutput. Heuristic one
   * assigns a score of .75 to I and a score of .25 to J. Heuristic two assigns a score of .66 to I
   * and a score of .34 to J.
   *
   * <p>In the final ranking I will be on the top with a score of (.75 + .66)/2 = .705 J will be
   * second with a score of .295
   *
   * <p>For better readability the score is multiplied by 100 and printed as integer to the user.
   * To change this override the reasonToHtml and toHTML methods in FaultLocalizationOutput and ErrorIndicator
   *
   * <p>The resulting ranking is: I (Score: 70) J (Score: 29)
   *
   * <p>Note that the maximum score is 100 by default. Overriding the corresponding methods can invalidate this.
   * The provided heuristics assign scores between 0 and 1 to the FaultLocalizationOutput of a certain set.
   * For every applied heuristic to a certain set the sum of the scores of the members of the set is exactly 1 by default.
   *
   * @param pHeuristics all heuristics to be concatenated
   * @return concatenated heuristic which sorts by total score.
   */
  public static <I extends FaultLocalizationOutput> FaultLocalizationHeuristic<I> concatHeuristics(
      List<FaultLocalizationHeuristic<I>> pHeuristics) {
    return l -> forAll(l, pHeuristics);
  }

  /**
   * Concatenate FaultLocalizationSetHeuristics
   * @param pHeuristics heuristics to concatenate
   * @param <I> inherits FaultLocaliztionOutput
   * @return new heuristic that concatenates all given heuristics and sorts the elements by total score.
   */
  public static <I extends FaultLocalizationOutput> FaultLocalizationSetHeuristic<I> concatSetHeuristics(
      List<FaultLocalizationSetHeuristic<I>> pHeuristics) {
    return l -> forAllSetHeuristics(l, pHeuristics);
  }

  private static <I extends FaultLocalizationOutput> Map<I, Integer> forAll(
      ErrorIndicatorSet<I> result, List<FaultLocalizationHeuristic<I>> pHeuristic) {
    Map<I, Double> setToScoreMap = new HashMap<>();
    Set<I> elements = new HashSet<>();
    for(FaultLocalizationHeuristic<I> heuristic: pHeuristic){
      elements.addAll(heuristic.rank(result).keySet());
    }
    for(I elem: elements){
      setToScoreMap.put(elem, elem.getScore());
    }
    return scoreToRankMap(setToScoreMap);
  }

  private static <I extends FaultLocalizationOutput> Map<ErrorIndicator<I>, Integer> forAllSetHeuristics(ErrorIndicatorSet<I> result, List<FaultLocalizationSetHeuristic<I>> concat){
    Map<ErrorIndicator<I>, Double> setToScoreMap = new HashMap<>();
    for(FaultLocalizationSetHeuristic<I> heuristic: concat){
      heuristic.rankSubsets(result).forEach((k,v) -> setToScoreMap.merge(k, k.calculateScore(), (v1,v2) -> Math.max(v1,v2)));
    }
    return scoreToRankMapSet(setToScoreMap);
  }

  /**
   * In the process of calculating the likelihood of a reason it is more likely to obtain a map of I to score instead of I to rank.
   * This method assigns the correct rank to a map of Outputs to scores. Note that two objects with scores 0,2222229 and 0,2222228 are ranked seperately.
   * @param outputToScoreMap a map that maps FaultLocalizationOutput to its overall score.
   * @param <I> FaultLocalizationOutput
   * @return map of FaultLocalizationOutput to rank (sorted by highest score)
   */
  public static <I extends FaultLocalizationOutput> Map<I, Integer> scoreToRankMap(Map<I, Double> outputToScoreMap){
    List<I> ranking = new ArrayList<>(outputToScoreMap.keySet());
    ranking.sort(Comparator.comparingDouble(l -> outputToScoreMap.get(l)));

    Map<I, Integer> rankMap = new HashMap<>();
    if(ranking.isEmpty())
      return rankMap;
    double min = outputToScoreMap.get(ranking.get(ranking.size()-1));
    int rank = 1;
    for(int i = ranking.size() - 1; i >= 0; i--){
      if(outputToScoreMap.get(ranking.get(i)) != min){
        rank++;
        min = outputToScoreMap.get(ranking.get(i));
      }
      rankMap.put(ranking.get(i), rank);
    }
    return rankMap;
  }

  /**
   * Obtain a map that ranks ErrorIndicators by passing a ErrorIndicator to Score Map.
   * @param outputToScoreMap a map that maps FaultLocalizationOutput to its overall score.
   * @param <I> FaultLocalizationOutput
   * @return map of ErrorIndicator to rank (sorted by highest score)
   */
  public static <I extends FaultLocalizationOutput> Map<ErrorIndicator<I>, Integer> scoreToRankMapSet(Map<ErrorIndicator<I>, Double> outputToScoreMap){
    List<ErrorIndicator<I>> ranking = new ArrayList<>(outputToScoreMap.keySet());
    ranking.sort(Comparator.comparingDouble(l -> outputToScoreMap.get(l)));

    Map<ErrorIndicator<I>, Integer> rankMap = new HashMap<>();
    if(ranking.isEmpty())
      return rankMap;
    double min = outputToScoreMap.get(ranking.get(ranking.size()-1));
    int rank = 1;
    for(int i = ranking.size() - 1; i >= 0; i--){
      if(outputToScoreMap.get(ranking.get(i)) != min){
        rank++;
        min = outputToScoreMap.get(ranking.get(i));
      }
      rankMap.put(ranking.get(i), rank);
    }
    return rankMap;
  }

  /**
   * Creates a set of distinct FaultLocalizationOutputs in a ErrorIndicatorSet
   * @param errorIndicatorSet obtained ErrorIndicatorSet by a fault localization algorithm
   * @param <I> FaultLocalizationOutput
   * @return condensed set of distinct FaultLocalizationOutputs
   */
  public static <I extends FaultLocalizationOutput> Set<I> condenseErrorIndicatorSet(
      ErrorIndicatorSet<I> errorIndicatorSet) {
    Set<I> allObjects = new HashSet<>();
    for (Set<I> errSet : errorIndicatorSet) {
      allObjects.addAll(errSet);
    }
    return allObjects;
  }

  public enum RankingMode {
    OVERALL,
    IDENTITY,
    SUBSET
  }

  public enum SetRankingMode {
    IDENTITY,
    SUBSET_SIZE
  }


}
