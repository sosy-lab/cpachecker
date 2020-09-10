// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.rankingmetricsinformation;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

public class FaultLocalizationFault {

  /**
   * Determines faults after rearranged these faults by sorting this by its line and its
   * corresponding edges. Sort these faults by its score reversed, so that the highest score appears
   * first.
   *
   * @param pFaultInfos rearrangedFaults
   * @return list of faults.
   */
  private List<Fault> determineFaults(List<FaultInformation> pFaultInfos) {
    ImmutableList.Builder<Fault> faults = ImmutableList.builder();
    // sort the faults
    pFaultInfos.stream().sorted(Comparator.comparing(FaultInformation::getLineScore).reversed());
    for (FaultInformation faultInformation : pFaultInfos) {
      Fault fault = new Fault(faultInformation.getHints());

      for (FaultContribution faultContribution : faultInformation.getHints()) {
        fault.setScore(faultInformation.getLineScore());
        fault.addInfo(FaultInfo.hint(faultContribution.textRepresentation()));
      }
      faults.add(fault);
    }

    return faults.build();
  }

  /**
   * Sums up the ranking information so that each line has many CFAEdges by their highest calculated
   * score
   *
   * @param origin input map
   * @return rearranged faults.
   */
  private List<FaultInformation> sumUpFaults(Map<FaultInformation, FaultContribution> origin) {

    Map<Integer, List<Map.Entry<FaultInformation, FaultContribution>>>
        faultToListOfFaultContribution =
            origin.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().getLineNumber()));

    return faultToListOfFaultContribution.entrySet().stream()
        .map(
            entry ->
                new FaultInformation(
                    entry.getValue().stream()
                        .map(Entry::getKey)
                        .max(Comparator.comparingDouble(FaultInformation::getLineScore))
                        .map(FaultInformation::getLineScore)
                        .orElse(0D),
                    entry.getValue().stream()
                        .map(
                            faultEntry -> {
                              FaultContribution faultContribution =
                                  new FaultContribution(faultEntry.getValue().correspondingEdge());
                              faultContribution.setScore(faultEntry.getKey().getLineScore());
                              return faultContribution;
                            })
                        .collect(Collectors.toSet()),
                    entry.getKey()))
        .collect(Collectors.toList());
  }

  public List<Fault> getFaults(Map<FaultInformation, FaultContribution> getRanked) {
    return determineFaults(sumUpFaults(getRanked));
  }
}
