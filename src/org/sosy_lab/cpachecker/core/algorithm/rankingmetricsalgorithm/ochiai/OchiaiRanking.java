// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm.ochiai;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalizationrankingmetrics.CoverageInformation;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalizationrankingmetrics.FailedCase;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalizationrankingmetrics.FaultInformation;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalizationrankingmetrics.FaultLocalizationCasesStatus;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalizationrankingmetrics.SafeCase;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;

public class OchiaiRanking {
  private final SafeCase safeCase;
  private final FailedCase failedCase;
  private final CoverageInformation coverageInformation;

  public OchiaiRanking(
      SafeCase pSafeCase, FailedCase pFailedCase, ShutdownNotifier pShutdownNotifier) {
    this.safeCase = pSafeCase;
    this.failedCase = pFailedCase;
    this.coverageInformation = new CoverageInformation(pFailedCase, pShutdownNotifier);
  }

  /**
   * Calculates suspicious of ochiai algorithm.
   *
   * @param pFailed Is the number of pFailed cases in each edge.
   * @param pPassed Is the number of pPassed cases in each edge.
   * @param totalFailed Is the total number of all possible error paths.
   * @return Calculated suspicious.
   */
  private double computeSuspicious(double pFailed, double pPassed, double totalFailed) {

    double denominator = Math.sqrt(totalFailed * (pFailed + pPassed));
    if (denominator == 0.0) {
      return 0.0;
    }
    return pFailed / denominator;
  }
  /**
   * Gets ranking information with calculated Suspiciousness
   *
   * @return Calculated ochiai ranking.
   */
  public Map<FaultInformation, FaultContribution> getRanked() throws InterruptedException {
    Set<ARGPath> safePaths = safeCase.getSafePaths();
    Set<ARGPath> errorPaths = failedCase.getErrorPaths();
    int totalErrorPaths = errorPaths.size();
    Map<CFAEdge, FaultLocalizationCasesStatus> coverage =
        coverageInformation.getCoverageInformation(safePaths, errorPaths);
    Map<FaultInformation, FaultContribution> rankedInfo = new HashMap<>();
    Set<FaultContribution> hints = new HashSet<>();
    coverage.forEach(
        (pCFAEdge, pFaultLocalizationCasesStatus) -> {
          double suspicious =
              computeSuspicious(
                  pFaultLocalizationCasesStatus.getFailedCases(),
                  pFaultLocalizationCasesStatus.getPassedCases(),
                  totalErrorPaths);
          // Skip 0 line numbers
          if (pCFAEdge.getLineNumber() != 0) {
            FaultContribution pFaultContribution = new FaultContribution(pCFAEdge);
            if (suspicious != 0) {
              pFaultContribution.setScore(suspicious);
              hints.add(pFaultContribution);
              rankedInfo.put(
                  new FaultInformation(
                      suspicious, hints, pFaultContribution.correspondingEdge().getLineNumber()),
                  pFaultContribution);
            }
          }
        });

    return rankedInfo;
  }
}
