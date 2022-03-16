// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.ranking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;

public class OverallOccurrenceScoring implements FaultScoring {

  private Map<Fault, Double> faultValue = new HashMap<>();

  @Override
  public RankInfo scoreFault(Fault fault) {
    return FaultInfo.rankInfo("Sorted by overall occurrence in all faults.", faultValue.get(fault));
  }

  @Override
  public void balancedScore(Set<Fault> faults) {
    for (Fault f1 : faults) {
      double value = 0;
      for (Fault f2 : faults) {
        Set<FaultContribution> intersection = new HashSet<>(f1);
        intersection.removeAll(f2);
        value += f1.size() - intersection.size();
      }
      faultValue.put(f1, value);
    }
    double sum = faultValue.values().stream().mapToDouble(Double::valueOf).sum();
    if (sum == 0) {
      for (Fault f : faults) {
        f.addInfo(
            FaultInfo.rankInfo("Sorted by overall occurrence in all faults.", 1d / faults.size()));
      }
    } else {
      for (Fault f : faults) {
        RankInfo info = scoreFault(f);
        info.setScore(info.getScore() / sum);
        f.addInfo(info);
      }
    }
  }
}
