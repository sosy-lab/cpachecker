// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.rankingmetricsinformation;

import com.google.common.collect.Sets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

/** Class CoverageInformation represents the coverage of each CFAEdge by each safe/error paths. */
public class CoverageInformation {
  private final FailedCase failedCase;
  private final ShutdownNotifier shutdownNotifier;

  public CoverageInformation(FailedCase pFailedCase, ShutdownNotifier pShutdownNotifier) {
    failedCase = pFailedCase;
    shutdownNotifier = pShutdownNotifier;
  }

  private Map<CFAEdge, FaultLocalizationCasesStatus> calculateCoverageInformation(
      Set<ARGPath> paths) throws InterruptedException {
    Map<CFAEdge, FaultLocalizationCasesStatus> coverageInfo = new LinkedHashMap<>();

    for (ARGPath path : paths) {
      for (CFAEdge cfaEdge : path.getFullPath()) {
        shutdownNotifier.shutdownIfNecessary();
        FaultLocalizationCasesStatus caseStatus;
        if (!coverageInfo.containsKey(cfaEdge)) {
          coverageInfo.put(cfaEdge, new FaultLocalizationCasesStatus(0, 0));
        }
        caseStatus = coverageInfo.get(cfaEdge);
        if (failedCase.isFailedPath(path)) {
          caseStatus =
              new FaultLocalizationCasesStatus(
                  caseStatus.getFailedCases() + 1, caseStatus.getPassedCases());

        } else {
          caseStatus =
              new FaultLocalizationCasesStatus(
                  caseStatus.getFailedCases(), caseStatus.getPassedCases() + 1);
        }

        // Skipp the "none" line numbers.
        if (cfaEdge.getLineNumber() != 0) {
          coverageInfo.put(cfaEdge, caseStatus);
        }
      }
    }
    return coverageInfo;
  }

  /**
   * Counts how many failed case / passed case has each Edge. For example <code>
   * line 5: N2 -{[cond == 0]},[2,1]</code> means that this specific Edge has `2` failed cases and
   * only one passed case.
   *
   * @param safePaths all safe paths
   * @param errorPaths all error paths
   * @return result as edge and its case status.
   */
  public Map<CFAEdge, FaultLocalizationCasesStatus> getCoverageInformation(
      Set<ARGPath> safePaths, Set<ARGPath> errorPaths) throws InterruptedException {
    return calculateCoverageInformation(Sets.union(safePaths, errorPaths));
  }
}
