// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.tarantula;

import com.google.common.collect.Sets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.TarantulaCasesStatus;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;

public class CoverageInformation {
  private final FailedCase failedCase;
  private final ShutdownNotifier shutdownNotifier;

  public CoverageInformation(FailedCase pFailedCase, ShutdownNotifier pShutdownNotifier) {
    failedCase = pFailedCase;
    shutdownNotifier = pShutdownNotifier;
  }

  /**
   * Counts how many failed case / passed case has each Edge. For example <code>
   * line 5: N2 -{[cond == 0]},[2,1]</code> means that this specific Edge has `2` failed cases and
   * only one passed case.
   *
   * @param paths All paths contains all error paths and passed paths.
   * @return result as edge and its case status.
   */
  private Map<FaultContribution, TarantulaCasesStatus> calculateCoverageInformation(
      Set<ARGPath> paths) throws InterruptedException {
    Map<FaultContribution, TarantulaCasesStatus> coverageInfo = new LinkedHashMap<>();

    for (ARGPath path : paths) {
      for (CFAEdge cfaEdge : path.getFullPath()) {
        shutdownNotifier.shutdownIfNecessary();
        TarantulaCasesStatus caseStatus;
        if (!coverageInfo.containsKey(new FaultContribution(cfaEdge))) {
          coverageInfo.put(new FaultContribution(cfaEdge), new TarantulaCasesStatus(0, 0));
        }
        caseStatus = coverageInfo.get(new FaultContribution(cfaEdge));
        if (failedCase.isFailedPath(path)) {
          caseStatus =
              new TarantulaCasesStatus(
                  caseStatus.getFailedCases() + 1, caseStatus.getPassedCases());

        } else {
          caseStatus =
              new TarantulaCasesStatus(
                  caseStatus.getFailedCases(), caseStatus.getPassedCases() + 1);
        }

        // Skipp the "none" line numbers.
        if (cfaEdge.getLineNumber() != 0) {
          coverageInfo.put(new FaultContribution(cfaEdge), caseStatus);
        }
      }
    }

    return coverageInfo;
  }

  /**
   * Gets the all information about the edge coverage.
   *
   * @return Covered edges.
   */
  public Map<FaultContribution, TarantulaCasesStatus> getCoverageInformation(
      Set<ARGPath> safePaths, Set<ARGPath> errorPaths) throws InterruptedException {
    return calculateCoverageInformation(Sets.union(safePaths, errorPaths));
  }
}
