// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.utils.CoverageInformationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.utils.FailedAndPassedExecutionCount;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

public abstract class SuspiciousnessMeasure {

  public List<Fault> getAllFaults(
      Set<ARGPath> safePaths, Set<ARGPath> errorPaths, CoverageInformationBuilder coverageInfo)
      throws InterruptedException {
    int totalSafePaths = safePaths.size();
    int totalErrorPaths = errorPaths.size();
    Map<CFAEdge, FailedAndPassedExecutionCount> coverage = coverageInfo.getCoverageInformation();
    List<Fault> faults = new ArrayList<>();
    for (var e : coverage.entrySet()) {
      final CFAEdge cfaEdge = e.getKey();
      final FailedAndPassedExecutionCount executionCount = e.getValue();
      double suspiciousness =
          calculateSuspiciousness(
              executionCount.getFailedCases(),
              executionCount.getPassedCases(),
              totalErrorPaths,
              totalSafePaths);

      FaultContribution pFaultContribution = new FaultContribution(cfaEdge);
      if (suspiciousness != 0) {
        pFaultContribution.setScore(suspiciousness);
        Fault f = new Fault(pFaultContribution);
        f.addInfo(FaultInfo.rankInfo("Suspiciousness measure", suspiciousness));
        FaultRankingUtils.assignScoreTo(f);
        faults.add(f);
      }
    }
    return faults;
  }

  public abstract double calculateSuspiciousness(
      double pFailed, double pPassed, double totalFailed, double totalPassed);
}
