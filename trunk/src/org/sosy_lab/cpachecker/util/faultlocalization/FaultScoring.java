// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;

/** Faults are ranked by their score. FaultScoring is meant to implement one measurement. */
public interface FaultScoring {

  /**
   * Receive the RankInfo for a certain fault.
   *
   * @param fault Generate a RankInfo for this fault
   * @return Generated RankInfo
   */
  RankInfo scoreFault(Fault fault);

  /**
   * Calculates the RankInfo for every fault in the result set and normalizes the scores. Normalized
   * scores are more comparable.
   *
   * @param faults obtained result set by any fault localization algorithm
   */
  default void balancedScore(Set<Fault> faults) {
    // NOTE: overriding this method is not recommended
    double overallScore = 0;
    List<RankInfo> infos = new ArrayList<>();
    for (Fault fault : faults) {
      RankInfo info = scoreFault(fault);
      overallScore += info.getScore();
      infos.add(info);
      fault.addInfo(info);
    }
    for (RankInfo info : infos) {
      info.setScore(info.getScore() / overallScore);
    }
  }
}
