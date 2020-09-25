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

public interface FaultRanking {

  RankInfo scoreFault(Fault fault);

  default void balancedScore(Set<Fault> faults) {
    double overallScore = 0;
    List<RankInfo> infos = new ArrayList<>();
    for (Fault fault : faults) {
      RankInfo info = scoreFault(fault);
      overallScore += info.getScore();
      infos.add(info);
      fault.addInfo(info);
    }
    for (RankInfo info : infos) {
      info.setScore(info.getScore()/overallScore);
    }
  }

}
