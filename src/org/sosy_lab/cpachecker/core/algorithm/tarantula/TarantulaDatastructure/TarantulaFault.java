// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

/** Class represents a special fault where a line has a set of Fault contributions */
public class TarantulaFault {

  private final double lineScore;
  private final int lineNumber;
  private final Set<FaultContribution> hints;

  public TarantulaFault(double pLineScore, Set<FaultContribution> pHints, int pLineNumber) {
    this.lineScore = pLineScore;
    this.lineNumber = pLineNumber;
    this.hints = pHints;
  }
  // An Empty constructor for calling the function of this class separately.
  public TarantulaFault() {
    this.lineNumber = 0;
    this.lineScore = 0;
    this.hints = null;
  }

  public double getLineScore() {
    return lineScore;
  }

  public Set<FaultContribution> getHints() {
    return hints;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Determinants tarantula faults after rearranged these faults by sorting this by its line and its
   * corresponding edges. Sort these faults by its score reversed, so that the highest score appears
   * first.
   *
   * @param rearrangeTarantulaFaults rearrangedTarantulaFaults
   * @return list of tarantula faults.
   */
  private List<Fault> faultsDetermination(List<TarantulaFault> rearrangeTarantulaFaults) {
    List<Fault> tarantulaFaults = new ArrayList<>();
    // sort the faults
    rearrangeTarantulaFaults.sort(Comparator.comparing(TarantulaFault::getLineScore).reversed());
    for (TarantulaFault tarantulaFault : rearrangeTarantulaFaults) {
      Fault fault = new Fault(tarantulaFault.getHints());

      for (FaultContribution faultContribution : tarantulaFault.getHints()) {
        fault.setScore(tarantulaFault.getLineScore());
        fault.addInfo(FaultInfo.hint(faultContribution.textRepresentation()));
      }
      tarantulaFaults.add(fault);
    }

    return tarantulaFaults;
  }
  /**
   * Sums up the ranking information so that each line has many CFAEdges by their highest calculated
   * score
   *
   * @param origin input map
   * @return rearranged faults.
   */
  private List<TarantulaFault> rearrangeTarantulaFaults(
      Map<TarantulaFault, FaultContribution> origin) {

    Map<Integer, List<Map.Entry<TarantulaFault, FaultContribution>>>
        faultToListOfFaultContribution =
            origin.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().getLineNumber()));

    return faultToListOfFaultContribution.entrySet().stream()
        .map(
            entry ->
                new TarantulaFault(
                    entry.getValue().stream()
                        .map(Entry::getKey)
                        .max(Comparator.comparingDouble(TarantulaFault::getLineScore))
                        .map(TarantulaFault::getLineScore)
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

  public List<Fault> getTarantulaFaults(Map<TarantulaFault, FaultContribution> getRanked) {
    return faultsDetermination(rearrangeTarantulaFaults(getRanked));
  }

  @Override
  public String toString() {
    return "TarantulaFault{"
        + "lineScore="
        + lineScore
        + ", lineNumber="
        + lineNumber
        + ", hints="
        + hints
        + '}';
  }
}
