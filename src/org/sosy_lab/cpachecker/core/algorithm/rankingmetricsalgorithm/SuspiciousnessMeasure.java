// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.rankingmetricsalgorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsinformation.CoverageInformation;
import org.sosy_lab.cpachecker.core.algorithm.rankingmetricsinformation.FaultLocalizationCasesStatus;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;

public abstract class SuspiciousnessMeasure {

  public List<SingleFaultOfRankingAlgo> getAllFaultsOfRankingAlgo(
      Set<ARGPath> safePaths, Set<ARGPath> errorPaths, CoverageInformation coverageInformation)
      throws InterruptedException {
    int totalSafePaths = safePaths.size();
    int totalErrorPaths = errorPaths.size();
    Map<CFAEdge, FaultLocalizationCasesStatus> coverage =
        coverageInformation.getCoverageInformation();
    List<SingleFaultOfRankingAlgo> singleFaultOfRankingAlgo = new ArrayList<>();
    coverage.forEach(
        (pCFAEdge, pFaultLocalizationCasesStatus) -> {
          double suspicious =
              calculateSuspiciousness(
                  pFaultLocalizationCasesStatus.getFailedCases(),
                  pFaultLocalizationCasesStatus.getPassedCases(),
                  totalErrorPaths,
                  totalSafePaths);

          FaultContribution pFaultContribution = new FaultContribution(pCFAEdge);
          if (suspicious != 0) {
            pFaultContribution.setScore(suspicious);
            singleFaultOfRankingAlgo.add(
                new SingleFaultOfRankingAlgo(
                    suspicious,
                    pFaultContribution,
                    pFaultContribution.correspondingEdge().getLineNumber()));
          }
        });

    return singleFaultOfRankingAlgo;
  }

  public abstract double calculateSuspiciousness(
      double pFailed, double pPassed, double totalFailed, double totalPassed);
}
