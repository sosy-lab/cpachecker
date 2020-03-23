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

import com.google.common.base.Functions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;

public class FaultLocalizationHeuristicImpl {

  /**
   * Sample heuristics for sorting the result set.
   *
   * @param result obtained set of FaultLocalizationAlgorithm
   * @param m the RankingMode decides which heuristic will be applied
   * @param <I> Must be the same type as it is used in the FaultLocalizationInfo
   * @return a ranked list of all outputs.
   */
  public static <I extends FaultLocalizationOutput> List<I> rank(
      ErrorIndicatorSet<I> result, RankingMode m) {
    if (result.size() == 0) {
      return Collections.emptyList();
    }
    switch (m) {
      case SUBSET:
        return rankByCountingSubsetOccurrencesImpl(result);
      case OVERALL:
        return rankByCountingElementsImpl(result);
      case IDENTITY:
      default:
        return rankIdentityImpl(result);
    }
  }

  /**
   * Heuristic that sorts all elements by relative frequency
   *
   * @return ranked list
   */
  public static <I extends FaultLocalizationOutput>
      FaultLocalizationHeuristic<I> getRankByCountingElements() {
    return FaultLocalizationHeuristicImpl::rankByCountingElementsImpl;
  }

  /**
   * Heuristic that sorts subsets by relative frequency
   *
   * @return ranked list
   */
  public static <I extends FaultLocalizationOutput>
      FaultLocalizationHeuristic<I> getRankByCountingSubsetOccurrences() {
    return FaultLocalizationHeuristicImpl::rankByCountingSubsetOccurrencesImpl;
  }

  /**
   * Will produce an arbitrary sorted list.
   *
   * @return arbitrary sorted list.
   */
  public static <I extends FaultLocalizationOutput> FaultLocalizationHeuristic<I> rankIdentity() {
    return FaultLocalizationHeuristicImpl::rankIdentityImpl;
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
   *
   * <p>The resulting ranking is: I (Score: 70) J (Score: 29)
   *
   * <p>Note that the maximum score is 100.
   *
   * @param pHeuristic all heuristics to be concatenated
   * @return concatenated Heuristic sorted by total score. The score has a range of [0;100]
   */
  public static <I extends FaultLocalizationOutput> FaultLocalizationHeuristic<I> concatHeuristics(
      List<FaultLocalizationHeuristic<I>> pHeuristic) {
    return l -> forAll(l, pHeuristic);
  }

  private static <I extends FaultLocalizationOutput> List<I> forAll(
      ErrorIndicatorSet<I> result, List<FaultLocalizationHeuristic<I>> pHeuristic) {
    Set<I> resultSet = new HashSet<>();
    for (FaultLocalizationHeuristic<I> iFaultLocalizationHeuristic : pHeuristic) {
      resultSet.addAll(iFaultLocalizationHeuristic.rank(result));
    }
    List<I> last = new ArrayList<>(resultSet);
    last.sort(Comparator.comparingDouble(FaultLocalizationOutput::getScore));
    Collections.reverse(last);
    return last;
  }

  // no heuristic applied, printed in order of appearance in the iterator
  private static <I extends FaultLocalizationOutput> List<I> rankIdentityImpl(
      ErrorIndicatorSet<I> result) {
    return new ArrayList<>(condenseErrorIndicatorSet(result));
  }

  private static <I extends FaultLocalizationOutput> List<I> rankByCountingElementsImpl(
      ErrorIndicatorSet<I> result) {
    List<I> selectors = new ArrayList<>(condenseErrorIndicatorSet(result));

    Map<I, Long> map =
        selectors.stream()
            .collect(Collectors.groupingBy(Functions.identity(), Collectors.counting()));

    long sum = map.values().stream().mapToLong(pLong -> pLong).sum();
    map.keySet()
        .forEach(
            l -> {
              FaultLocalizationReason<I> reason = FaultLocalizationReason.defaultExplanationOf(l);
              reason.setLikelihood(((double) map.get(l)) / sum);
              l.addReason(reason);
            });

    List<I> edge = new ArrayList<>(map.keySet());
    edge.sort((a, b) -> (int) (map.get(b) - map.get(a)));

    return edge;
  }

  private static <I extends FaultLocalizationOutput> List<I> rankByCountingSubsetOccurrencesImpl(
      ErrorIndicatorSet<I> result) {
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
        FaultLocalizationReason<I> reason =
            FaultLocalizationReason.defaultExplanationOf(Collections.singleton(temp));
        List<I> related = new ArrayList<>(subset);
        related.remove(temp);
        reason.setRelated(related);
        double likelihood = ((double)map.get(subset))/totalOccurrences;
        reason.setLikelihood(likelihood);
        mapLikelihood.put(temp, likelihood);
        temp.addReason(reason);
      }
    }

    List<I> output = new ArrayList<>(condenseErrorIndicatorSet(result));
    output.sort(Comparator.comparingDouble(mapLikelihood::get));

    return output;
  }

  public static <I extends FaultLocalizationOutput> Set<I> condenseErrorIndicatorSet(
      ErrorIndicatorSet<I> errorIndicatorSet) {
    Set<I> allObjects = new HashSet<>();
    for (Set<I> errSet : errorIndicatorSet) {
      allObjects.addAll(errSet);
    }
    return allObjects;
  }

  public static FaultLocalizationExplanation explainLocationWithoutContext() {
    return FaultLocalizationHeuristicImpl::possibleSolutionsWithoutContext;
  }

  /**
   * possible implementation of a function that maps a FaultLocalizationOutput object to a
   * description (as string) this function relies on singleton sets otherwise an error is thrown.
   * based on the edge type a suggestion for fixing the bug is made. A sample usage can be found
   * here: FaultLocalizationHeuristicsImpl.rankByCountingSubsetOccurrences
   *
   * @param pFaultLocalizationOutputs set of FaultLocalizationOutputs.
   * @return explanation of what might be a fix
   */
  private static <I extends FaultLocalizationOutput> String possibleSolutionsWithoutContext(
      Set<I> pFaultLocalizationOutputs) {
    if (pFaultLocalizationOutputs.size() != 1) {
      throw new IllegalArgumentException("reason without context requires exactly one edge");
    }
    I object = new ArrayList<>(pFaultLocalizationOutputs).get(0);
    CFAEdge pEdge = object.correspondingEdge();
    String description = pEdge.getDescription();
    switch (pEdge.getEdgeType()) {
      case AssumeEdge:
        {
          String[] ops = {"<", ">", "<=", "!=", "==", ">="};
          String op = "";
          for (String o : ops) {
            if (description.contains(o)) {
              op = o;
              break;
            }
          }
          return "Try to replace \""
              + op
              + "\" in \""
              + description
              + "\" with another boolean operator (<, >, <=, !=, ==, >=).";
        }
      case StatementEdge:
        {
          return "Try to change the assigned value of \""
              + Iterables.get(Splitter.on(" ").split(description), 0)
              + "\" in \""
              + description
              + "\" to another value.";
        }
      case DeclarationEdge:
        {
          return "Try to declare the variable in \"" + description + "\" differently.";
        }
      case ReturnStatementEdge:
        {
          return "Try to change the return-value of \"" + description + "\" to another value.";
        }
      case FunctionCallEdge:
        {
          return "The function call \"" + description + "\" may have unwanted side effects.";
        }
      case FunctionReturnEdge:
        {
          String functionName = ((CFunctionReturnEdge) pEdge).getFunctionEntry().getFunctionName();
          return "The function " + functionName + "(...) may have an unwanted return value.";
        }
      case CallToReturnEdge:
      case BlankEdge:
      default:
        return "No proposal found for the statement: \"" + description + "\".";
    }
  }
}
