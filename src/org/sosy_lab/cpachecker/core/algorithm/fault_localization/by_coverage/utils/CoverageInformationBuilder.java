// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.utils;

import com.google.common.collect.Sets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

/**
 * This class creates the coverage data of each CFAEdge by each safe/error paths. The status of the
 * coverage is represented by class {@link FailedAndPassedExecutionCount}.
 */
public class CoverageInformationBuilder {
  private final ShutdownNotifier shutdownNotifier;
  private final Set<ARGPath> safePaths;
  private final Set<ARGPath> errorPaths;

  public CoverageInformationBuilder(
      ShutdownNotifier pShutdownNotifier, Set<ARGPath> pSafePaths, Set<ARGPath> pErrorPaths) {
    shutdownNotifier = pShutdownNotifier;
    safePaths = pSafePaths;
    errorPaths = pErrorPaths;
  }

  private Map<CFAEdge, FailedAndPassedExecutionCount> calculateCoverageInformation(
      Set<ARGPath> paths) throws InterruptedException {
    Map<CFAEdge, FailedAndPassedExecutionCount> coverageInfo = new LinkedHashMap<>();

    for (ARGPath path : paths) {
      for (CFAEdge cfaEdge : path.getFullPath()) {
        shutdownNotifier.shutdownIfNecessary();
        FailedAndPassedExecutionCount caseStatus;
        if (!coverageInfo.containsKey(cfaEdge)) {
          coverageInfo.put(cfaEdge, new FailedAndPassedExecutionCount(0, 0));
        }
        caseStatus = coverageInfo.get(cfaEdge);
        // Check whether the path is a error path
        if (path.getLastState().isTarget()) {
          caseStatus =
              new FailedAndPassedExecutionCount(
                  caseStatus.getFailedCases() + 1, caseStatus.getPassedCases());

        } else {
          caseStatus =
              new FailedAndPassedExecutionCount(
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
   * @return result as edge and its case status.
   */
  public Map<CFAEdge, FailedAndPassedExecutionCount> getCoverageInformation()
      throws InterruptedException {
    return calculateCoverageInformation(Sets.union(safePaths, errorPaths));
  }
}
