// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.tarantula;

import java.util.Map.Entry;
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
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.TarantulaCFAEdgeSuspicious;
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
            if (suspicious != 0) {
              fault.setScore(suspicious);
              rankedInfo.add(
                  new TarantulaFault(
                      suspicious,
                      fault,
                      new TarantulaCFAEdgeSuspicious(
                          pFaultContribution.correspondingEdge(), suspicious)));
            }
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
  private Map<TarantulaFault, List<TarantulaCFAEdgeSuspicious>> rearrangeTarantulaFaults(
      List<TarantulaFault> origin) {
    Collection<TarantulaFault> collection = new ArrayList<>(origin);

    return collection.stream()
        .collect(
            Collectors.groupingBy(
                e -> e.getTarantulaCFAEdgeSuspicious().getCfaEdge().getLineNumber()))
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                e ->
                    e.getValue().stream()
                        .max(Comparator.comparing(TarantulaFault::getLineScore))
                        .orElseThrow(),
                e ->
                    e.getValue().stream()
                        .map(v -> v.getTarantulaCFAEdgeSuspicious())
                        .collect(Collectors.toList())));
  }
  /**
   * Sort each TarantulaFault and its CFAEdge by its scores
   *
   * @param getRearrangedFaults input map
   * @return sorted Map.
   */
  private Map<TarantulaFault, List<TarantulaCFAEdgeSuspicious>> getSortedFaultsByScore(
      Map<TarantulaFault, List<TarantulaCFAEdgeSuspicious>> getRearrangedFaults) {

    return getRearrangedFaults.entrySet().stream()
        .sorted(
            (Comparator.comparingDouble(
                    (Entry<TarantulaFault, List<TarantulaCFAEdgeSuspicious>> k) ->
                        k.getKey().getLineScore()))
                .reversed())
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
              for (TarantulaCFAEdgeSuspicious cfaEdge : v) {
                k.getFault()
                    .addInfo(
                        FaultInfo.hint(
                            "Unknown potential fault at Edge ( "
                                + cfaEdge.getCfaEdge().getDescription()
                                + " ) "
                                + "with suspicious ( "
                                + cfaEdge.getCfaEdgeSuspicious()
                                + " ) "));
              }
              tarantulaFaults.add(k.getFault());
            });

    return tarantulaFaults;
  }
}
