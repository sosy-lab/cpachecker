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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
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

  public Map<TarantulaFault, CFAEdge> getRanked() throws InterruptedException {
    Set<ARGPath> safePaths = safeCase.getSafePaths();
    Set<ARGPath> errorPaths = failedCase.getErrorPaths();
    int totalSafePaths = safePaths.size();
    int totalErrorPaths = errorPaths.size();
    Map<FaultContribution, TarantulaCasesStatus> coverage =
        coverageInformation.getCoverageInformation(safePaths, errorPaths);
    Map<TarantulaFault, CFAEdge> result = new HashMap<>();

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

            result.put(
                new TarantulaFault(suspicious, fault, pFaultContribution),
                pFaultContribution.correspondingEdge());
          }
        });

    return result;
  }

  public Map<TarantulaFault, List<CFAEdge>> rearrangeTheFaults(
      Map<TarantulaFault, CFAEdge> origin) {
    return origin.entrySet().stream()
        .collect(
            Collectors.groupingBy(
                e -> e.getKey().getFaultContribution().correspondingEdge().getLineNumber()))
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                e ->
                    e.getValue().stream()
                        .map(Map.Entry::getKey)
                        .max(Comparator.comparing(TarantulaFault::getScore))
                        .get(),
                e -> e.getValue().stream().map(Map.Entry::getValue).collect(Collectors.toList())));
  }

  public List<Fault> getTarantulaFaults() throws InterruptedException {
    List<Fault> faults = new ArrayList<>();
    rearrangeTheFaults(getRanked())
        .forEach(
            (k, v) -> {
              k.getFault().addInfo(FaultInfo.hint("Unknown potential fault: " + v));
              faults.add(k.getFault());
            });

    return faults;
  }

  /*private Map<TarantulaRanking, CFAEdge> sortBySuspicious(
      final Map<TarantulaRanking, CFAEdge> wordCounts) {

    return wordCounts.entrySet().stream()
        .sorted(Map.Entry.<TarantulaRanking, CFAEdge>comparingByValue().reversed())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }*/
}
