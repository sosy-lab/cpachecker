// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.rankings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;

/**
 * Rank the faults by the position in the counterexample. The closer edges of a fault are to the
 * error location the higher the rank.
 */
public class CallHierarchyScoring implements FaultScoring {

  private final Map<CFAEdge, Integer> mapEdgeToPosition;
  private final int firstErrorEdge;

  /**
   * Reward fault contributions that are closer to the error in the counterexample
   *
   * @param pEdgeList counterexample
   */
  public CallHierarchyScoring(List<CFAEdge> pEdgeList) {
    mapEdgeToPosition = new HashMap<>();
    for (int i = 0; i < pEdgeList.size(); i++) {
      mapEdgeToPosition.put(pEdgeList.get(i), i + 1);
    }
    firstErrorEdge = pEdgeList.size() + 1;
  }

  @Override
  public RankInfo scoreFault(Fault fault) {
    int max =
        fault.stream()
            .mapToInt(fc -> mapEdgeToPosition.getOrDefault(fc.correspondingEdge(), 0))
            .max()
            .orElse(0);
    int min = firstErrorEdge - max;
    return FaultInfo.rankInfo(
        "This fault is " + min + " execution step(s) away from the error location.", max);
  }
}
