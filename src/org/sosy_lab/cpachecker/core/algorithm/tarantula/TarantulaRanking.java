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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.FailedCase;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.SafeCase;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.TarantulaCasesStatus;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.TarantulaFault;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

public class TarantulaRanking {
  private final SafeCase safeCase;
  private final FailedCase failedCase;
  private final CoverageInformation coverageInformation;

  public TarantulaRanking(
      SafeCase pSafeCase, FailedCase pFailedCase, ShutdownNotifier pShutdownNotifier) {
    this.safeCase = pSafeCase;
    this.failedCase = pFailedCase;
    this.coverageInformation = new CoverageInformation(pFailedCase, pShutdownNotifier);
  }

  /**
   * Calculates suspicious of tarantula algorithm.
   *
   * @param pFailed Is the number of pFailed cases in each edge.
   * @param pPassed Is the number of pPassed cases in each edge.
   * @param totalFailed Is the total number of all possible error paths.
   * @param totalPassed Is the total number of all possible safe paths.
   * @return Calculated suspicious.
   */
  private double computeSuspicious(
      double pFailed, double pPassed, double totalFailed, double totalPassed) {
    double numerator = pFailed / totalFailed;
    double denominator = (pPassed / totalPassed) + (pFailed / totalFailed);
    if (denominator == 0.0) {
      return 0.0;
    }
    return numerator / denominator;
  }
  /**
   * Gets ranking information with calculated Suspiciousness
   *
   * @return Calculated tarantula ranking.
   */
  private List<TarantulaFault> getRanked() throws InterruptedException {
    Set<ARGPath> safePaths = safeCase.getSafePaths();
    Set<ARGPath> errorPaths = failedCase.getErrorPaths();
    int totalSafePaths = safePaths.size();
    int totalErrorPaths = errorPaths.size();
    Map<FaultContribution, TarantulaCasesStatus> coverage =
        coverageInformation.getCoverageInformation(safePaths, errorPaths);
    List<TarantulaFault> rankedInfo = new ArrayList<>();

    coverage.forEach(
        (pFaultContribution, pTarantulaCasesStatus) -> {
          double suspicious =
              computeSuspicious(
                  pTarantulaCasesStatus.getFailedCases(),
                  pTarantulaCasesStatus.getPassedCases(),
                  totalErrorPaths,
                  totalSafePaths);
          // Skip 0 line numbers
          if (pFaultContribution.correspondingEdge().getLineNumber() != 0) {
            Fault fault = new Fault(pFaultContribution);
            fault.setScore(suspicious);

            rankedInfo.add(new TarantulaFault(suspicious, fault, pFaultContribution));
          }
        });

    return rankedInfo;
  }
  /**
   * Sums up the ranking information so that each line has many CFAEdges by their highest calculated
   * score
   *
   * @param origin input map
   * @return rearranged faults.
   */
  private Map<TarantulaFault, List<String>> rearrangeTarantulaFaults(List<TarantulaFault> origin) {
    Collection<TarantulaFault> collection = new ArrayList<>(origin);

    return collection.stream()
        .collect(
            Collectors.groupingBy(
                e -> e.getFaultContribution().correspondingEdge().getLineNumber()))
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                e ->
                    e.getValue().stream()
                        .max(Comparator.comparing(TarantulaFault::getScore))
                        .orElseThrow(),
                e ->
                    e.getValue().stream()
                        .map(TarantulaFault::getDescription)
                        .collect(Collectors.toList())));
  }
  /**
   * Sort each TarantulaFault and its CFAEdge by its scores
   *
   * @param getRearrangedFaults input map
   * @return sorted Map.
   */
  private Map<TarantulaFault, List<String>> getSortedFaultsByScore(
      Map<TarantulaFault, List<String>> getRearrangedFaults) {

    return getRearrangedFaults.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldValue, newValue) -> oldValue,
                LinkedHashMap::new));
  }

  public List<Fault> getTarantulaFaults() throws InterruptedException {
    List<Fault> tarantulaFaults = new ArrayList<>();
    getSortedFaultsByScore(rearrangeTarantulaFaults(getRanked()))
        .forEach(
            (k, v) -> {
              for (String description : v) {
                k.getFault().addInfo(FaultInfo.hint("Unknown potential fault: " + description));
              }
              tarantulaFaults.add(k.getFault());
            });

    return tarantulaFaults;
  }
}
