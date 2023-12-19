package org.sosy_lab.cpachecker.util.faultlocalization.ranking;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;

public class MultiplePreConditionScoring implements FaultScoring {
  private final Map<Fault, Double> faultValue = new HashMap<>();

  @Override
  public RankInfo scoreFault(Fault pFault) {
    return FaultInfo.rankInfo(
        "Sorted by overall occurrence in all faults.", faultValue.get(pFault));
  }

  @Override
  public void balancedScore(Collection<Fault> pFaults) {

    Map<FaultContribution, Double> faultContributionOccurences =
        countFaultContributionOccurrences(pFaults);
    for (Fault f1 : pFaults) {
      double totalOccurrences = 0;
      for (FaultContribution fc : f1) {
        totalOccurrences += faultContributionOccurences.getOrDefault(fc, (double) 0);
      }
      faultValue.put(f1, totalOccurrences / f1.size());
    }

    double sum =
        Double.max(1.0, faultValue.values().stream().mapToDouble(Double::doubleValue).sum());

    for (Fault f : pFaults) {
      RankInfo info = scoreFault(f);
      info.setScore((info.getScore() / sum) * 100);
      f.addInfo(info);
    }
  }

  private Map<FaultContribution, Double> countFaultContributionOccurrences(
      Collection<Fault> pFaults) {
    Map<FaultContribution, Double> faultContributionOccurrences = new HashMap<>();

    for (Fault f : pFaults) {
      for (FaultContribution fc : f) {
        faultContributionOccurrences.put(fc, faultContributionOccurrences.getOrDefault(fc, 0d) + 1);
      }
    }
    return faultContributionOccurrences;
  }
}
