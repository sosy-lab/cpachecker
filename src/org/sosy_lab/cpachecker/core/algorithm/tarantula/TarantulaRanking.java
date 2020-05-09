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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.FailedCase;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.SafeCase;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.TarantulaCasesStatus;

public class TarantulaRanking {
  private final SafeCase safeCase;
  private final FailedCase failedCase;
  private final CoverageInformation coverageInformation;
  private final ShutdownNotifier shutdownNotifier;

  public TarantulaRanking(
      SafeCase pSafeCase, FailedCase pFailedCase, ShutdownNotifier pShutdownNotifier) {
    this.safeCase = pSafeCase;
    this.failedCase = pFailedCase;
    this.shutdownNotifier = pShutdownNotifier;
    this.coverageInformation = new CoverageInformation(pFailedCase, pShutdownNotifier);
  }

  /**
   * Calculates how many total failed cases are in ARG.
   *
   * @return how many failed cases are found.
   */
  private int totalFailed() {
    return failedCase.getTotalFailedCases();
  }
  /**
   * Calculates how many total passed cases are in ARG.
   *
   * @return how many passed cases are found.
   */
  private int totalPassed() {
    return safeCase.getTotalSafeCases();
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

  public Map<CFAEdge, Double> getRanked() throws InterruptedException {

    Map<CFAEdge, TarantulaCasesStatus> coverage =
        this.coverageInformation.getCoverageInformation(
            safeCase.getSafePaths(), failedCase.getFailedPaths());

    Map<CFAEdge, Double> resultMap = new LinkedHashMap<>();
    coverage.forEach(
        (pCFAEdge, pTarantulaCasesStatus) -> {
          resultMap.put(
              pCFAEdge,
              computeSuspicious(
                  pTarantulaCasesStatus.getFailedCases(), pTarantulaCasesStatus.getPassedCases()));
        });

    // Sort the result by its value and ignore the suspicious with 0.0 ration.
    final Map<CFAEdge, Double> result =
        resultMap.entrySet().stream()
            .filter(e -> e.getValue() != 0)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return sortBySuspicious(result);
  }

  private Map<CFAEdge, Double> sortBySuspicious(final Map<CFAEdge, Double> wordCounts) {

    return wordCounts.entrySet().stream()
        .sorted(Map.Entry.<CFAEdge, Double>comparingByValue().reversed())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }
}
