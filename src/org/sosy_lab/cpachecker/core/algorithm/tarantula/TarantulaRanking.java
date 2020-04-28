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
package org.sosy_lab.cpachecker.core.algorithm.tarantula;

import com.google.common.collect.Sets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.FailedCase;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.SafeCase;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.TarantulaCasesStatus;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

public class TarantulaRanking {
  private final SafeCase safeCase;
  private final FailedCase failedCase;

  public TarantulaRanking(ReachedSet pPReachedSet) {
    safeCase = new SafeCase(pPReachedSet);
    failedCase = new FailedCase(pPReachedSet);
  }

  /**
   * Calculates how many total failed cases are in ARG.
   *
   * @return how many failed cases are found.
   */
  private int totalFailed() {

    return failedCase.totalFailed();
  }
  /**
   * Calculates how many total passed cases are in ARG.
   *
   * @return how many passed cases are found.
   */
  private int totalPassed() {
    int total = Sets.union(safeCase.getEdgesOfSafePaths(), failedCase.getFailedPaths()).size();
    return total - totalFailed();
  }
  /**
   * Calculates suspicious of tarantula algorithm.
   *
   * @param pFailed Is the number of pFailed cases in each edge.
   * @param pPassed Is the number of pPassed cases in each edge.
   * @return Calculated suspicious.
   */
  private double computeSuspicious(double pFailed, double pPassed) {
    double numerator = pFailed / totalFailed();
    double denominator = (pPassed / totalPassed()) + (pFailed / totalFailed());
    if (denominator == 0.0) {
      return 0.0;
    }
    return numerator / denominator;
  }
  /**
   * Counts how many failed case / passed case has each Edges. For example <code>
   * line 5: N2 -{[cond == 0]},[2,1]</code> means that this specific Edges has `2` failed cases and
   * only one passed case.
   *
   * @param  paths The whole path contains all error paths and passed paths.
   * @return result as <code>Map<code/>.
   */
  private Map<CFAEdge, TarantulaCasesStatus> coverageInformation(Set<List<CFAEdge>> paths) {

    Map<CFAEdge, TarantulaCasesStatus> map = new LinkedHashMap<>();
    for (List<CFAEdge> individualArray : paths) {
      for (int j = 0; j < individualArray.size(); j++) {
        TarantulaCasesStatus pair;
        if (map.containsKey(individualArray.get(j))) {
          pair = map.get(individualArray.get(j));
          if (failedCase.isFailedPath(individualArray)) {
            pair = new TarantulaCasesStatus(pair.getFailedCases() + 1, pair.getPassedCases());

          } else {
            pair = new TarantulaCasesStatus(pair.getFailedCases(), pair.getPassedCases() + 1);
          }
          map.put(individualArray.get(j), pair);
        } else {
          if (failedCase.isFailedPath(individualArray)) {
            pair = new TarantulaCasesStatus(1, 0);
          } else {
            pair = new TarantulaCasesStatus(0, 1);
          }
        }
        // Skipp the "none" line numbers.
        if (individualArray.get(j).getLineNumber() != 0) {
          map.put(individualArray.get(j), pair);
        }
      }
    }

    return map;
  }
  /**
   * Gets the <code> HashMap<CFAEdge, int[]> </code>.
   *
   * @return Covered edges.
   */
  public Map<CFAEdge, TarantulaCasesStatus> getTable(
      Set<List<CFAEdge>> safePaths, Set<List<CFAEdge>> errorPaths) {

    return coverageInformation(Sets.union(safePaths, errorPaths));
  }

  public Map<CFAEdge, Double> getRanked() {

    Map<CFAEdge, TarantulaCasesStatus> table =
        getTable(safeCase.getEdgesOfSafePaths(), failedCase.getFailedPaths());
    Map<CFAEdge, Double> resultMap = new LinkedHashMap<>();
    table.forEach(
        (key, value) ->
            resultMap.put(key, computeSuspicious(value.getFailedCases(), value.getPassedCases())));

    // Sort the result by its value and ignore the suspicious with 0.0 ration.
    final Map<CFAEdge, Double> sortedByCount =
        resultMap.entrySet().stream()
            .filter(e -> e.getValue() != 0)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return sortByValue(sortedByCount);
  }

  private Map<CFAEdge, Double> sortByValue(final Map<CFAEdge, Double> wordCounts) {

    return wordCounts.entrySet().stream()
        .sorted(Map.Entry.<CFAEdge, Double>comparingByValue().reversed())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }
}
