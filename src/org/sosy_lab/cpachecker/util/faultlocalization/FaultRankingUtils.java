// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.IdentityRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.OverallOccurrenceRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.ranking.SetSizeRanking;

/**
 * Provides a variety of methods that are useful for ranking and assigning scores.
 */
public class FaultRankingUtils {

  private static Function<List<FaultInfo>, Double> evaluationFunction =
      r ->
          r.stream()
              .filter(c -> c.getType().equals(InfoType.RANK_INFO))
              .mapToDouble(FaultInfo::getScore)
              .average()
              .orElse(0);

  public enum RankingMode {
    /** Rank all elements by occurrence in iterator (arbitrary)*/
    IDENTITY,
    /** Rank elements by size of the set. The fewer elements a set has the higher is the rank.*/
    SET_SIZE,
    /** Rank the sets by the overall occurrence of the elements in all sets.*/
    OVERALL
  }

  /**
   * Rankings for Faults.
   *
   * @param ranking the RankingMode decides which ranking will be applied
   * @return a ranked list of all faults.
   */
  public static List<Fault> rank(RankingMode ranking, Set<Fault> faults) {
    switch (ranking) {
      case OVERALL:
        return new OverallOccurrenceRanking().rank(faults);
      case SET_SIZE:
        return new SetSizeRanking().rank(faults);
      case IDENTITY:
      default:
        return new IdentityRanking().rank(faults);
    }
  }

  /**
   * Concatenate rankings to optimize the result. Each ranking can optionally assign a score to
   * the FaultContribution. If more than one ranking is used, the resulting list gets sorted
   * by the provided function.
   *
   * <p>Example: Assume objects I,J to be objects that extend FaultContribution. Ranking 1
   * assigns a score of .75 to I and a score of .25 to J. Ranking 2 assigns a score of .66 to I
   * and a score of .34 to J.
   *
   * <p>In the final ranking I will be on the top with a score of (.75 + .66)/2 = .705 J will be
   * second with a score of .295
   *
   * <p>For better readability the score is multiplied by 100 and printed as integer (in percent) to the user.
   * To change this override the toHtml() methods in the FaultReportWriter
   *
   * <p>The resulting ranking is: I (Score: 70) J (Score: 29)
   *
   * <p>Note that the maximum score is 100 when using default ranking. Using different calculation methods can invalidate this.
   * The provided rankings assign scores between 0 and 1 to the FaultContribution of a certain set.
   * Normalizing the scores is useful and ensures that the final ranking is not meaningless because of wrong weighting.
   *
   * For every applied ranking to a certain set the sum of the scores of the members of the set is exactly 1 by default.
   *
   * @param pRanking all rankings to be concatenated
   * @param finalScoringFunction function for assigning the final overall score to a fault
   * @return concatenated heuristic which sorts by total score.
   */
  public static FaultRanking concatHeuristics(Function<Fault, Double> finalScoringFunction,
                                              FaultRanking... pRanking) {
    return l -> forAll(l, finalScoringFunction, pRanking);
  }

  public static FaultRanking concatHeuristicsDefaultFinalScoring(FaultRanking... pRanking) {
    return l -> forAll(l, r -> evaluationFunction.apply(r.getInfos()), pRanking);
  }

  private static List<Fault> forAll(Set<Fault> result, Function<Fault, Double> finalScoringFunction, FaultRanking... concat){
    if(concat.length == 0){
      return new IdentityRanking().rank(result);
    }
    Set<Fault> all = new HashSet<>();
    for (FaultRanking faultRanking : concat) {
      all.addAll(faultRanking.rank(result));
    }
    return all.stream()
        .sorted((l1,l2) -> Double.compare(finalScoringFunction.apply(l2), finalScoringFunction.apply(l1)))
        .collect(Collectors.toList());
  }

  /**
   * Assign a score to a Fault with the default score evaluation function (average of all likelihoods).
   * When implementing a own method that assigns a score to a Fault make sure that hints are not included in the calculation.
   * @param fault Assigns a score to the Fault.
   */
  public static void assignScoreTo(Fault fault){
    fault.setScore(evaluationFunction.apply(fault.getInfos()));
  }

  /**
   * Assign a score to a FaultContribution with the default score evaluation function (average of all likelihoods).
   * When implementing a own method that assigns a score to a FaultContribution make sure that hints are not included in the calculation.
   * @param faultContribution Assigns a score to the FaultContribution.
   */
  public static void assignScoreTo(FaultContribution faultContribution){
    faultContribution.setScore(evaluationFunction.apply(faultContribution.getInfos()));
  }

  public static RankingResults rankedListFor(Set<Fault> pFaults, Function<Fault, Double> scoringFunction){
    Map<Fault, Double> scoreMap = new HashMap<>();
    pFaults.forEach(e -> scoreMap.put(e, scoringFunction.apply(e)));
    List<Fault> ranked = new ArrayList<>(scoreMap.keySet());
    ranked.sort(Comparator.comparingDouble(l -> scoreMap.get(l)).reversed());
    return new RankingResults(ranked, scoreMap);
  }

  public static class RankingResults{
    private List<Fault> rankedList;
    private Map<Fault, Double> likelihoodMap;

    public RankingResults(List<Fault> pRankedList, Map<Fault, Double> pLikelihoodMap){
      rankedList = pRankedList;
      likelihoodMap = pLikelihoodMap;
    }

    public List<Fault> getRankedList() {
      return rankedList;
    }

    public Map<Fault, Double> getLikelihoodMap() {
      return likelihoodMap;
    }
  }

}
