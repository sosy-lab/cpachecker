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

import com.google.common.collect.Sets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.FailedCase;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure.TarantulaCasesStatus;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
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
