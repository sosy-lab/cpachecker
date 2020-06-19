// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.tarantula;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
  private Map<TarantulaFault, FaultContribution> getRanked() throws InterruptedException {
    Set<ARGPath> safePaths = safeCase.getSafePaths();
    Set<ARGPath> errorPaths = failedCase.getErrorPaths();
    int totalSafePaths = safePaths.size();
    int totalErrorPaths = errorPaths.size();
    Map<FaultContribution, TarantulaCasesStatus> coverage =
        coverageInformation.getCoverageInformation(safePaths, errorPaths);
    Map<TarantulaFault, FaultContribution> rankedInfo = new HashMap<>();
    Set<FaultContribution> hints = new HashSet<>();
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

            if (suspicious != 0) {
              pFaultContribution.setScore(suspicious);
              hints.add(pFaultContribution);
              rankedInfo.put(
                  new TarantulaFault(
                      suspicious, hints, pFaultContribution.correspondingEdge().getLineNumber()),
                  pFaultContribution);
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
  private List<TarantulaFault> rearrangeTarantulaFaults(
      Map<TarantulaFault, FaultContribution> origin) {

    Map<Integer, List<Map.Entry<TarantulaFault, FaultContribution>>>
        faultToListOfFaultContribution =
            origin.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().getLineNumber()));

    return faultToListOfFaultContribution.entrySet().stream()
        .map(
            entry ->
                new TarantulaFault(
                    entry.getValue().stream()
                        .map(Entry::getKey)
                        .max(Comparator.comparingDouble(TarantulaFault::getLineScore))
                        .map(TarantulaFault::getLineScore)
                        .orElse(0D),
                    entry.getValue().stream()
                        .map(
                            faultEntry -> {
                              FaultContribution faultContribution =
                                  new FaultContribution(faultEntry.getValue().correspondingEdge());
                              faultContribution.setScore(faultEntry.getKey().getLineScore());
                              return faultContribution;
                            })
                        .collect(Collectors.toSet()),
                    entry.getKey()))
        .collect(Collectors.toList());
  }

  public List<Fault> getTarantulaFaults() throws InterruptedException {
    List<Fault> tarantulaFaults = new ArrayList<>();
    List<TarantulaFault> rearrangeTarantulaFaults = rearrangeTarantulaFaults(getRanked());
    rearrangeTarantulaFaults.sort(Comparator.comparing(TarantulaFault::getLineScore).reversed());
    for (TarantulaFault tarantulaFault : rearrangeTarantulaFaults) {

      Fault fault = new Fault(tarantulaFault.getHints());

      for (FaultContribution faultContribution : tarantulaFault.getHints()) {
        fault.setScore(tarantulaFault.getLineScore());
        fault.addInfo(
            FaultInfo.hint(
                faultContribution.textRepresentation()
                    + "at edge ( "
                    + faultContribution.correspondingEdge().getDescription()
                    + " )"));
      }
      tarantulaFaults.add(fault);
    }

    return tarantulaFaults;
  }
}
