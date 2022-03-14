// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.explainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

/** This Class Explains the Differences Between 2 Executions */
public class ExplainTool {

  /**
   * Takes a CE and a Safe Path and Prints the Differences between them
   *
   * @param counterexample The counterexample that we want to examine
   * @param closestExecution The closest successful found execution
   */
  public void explainDeltas(
      List<CFAEdge> counterexample, List<CFAEdge> closestExecution, CounterexampleInfo ceInfo) {
    closestExecution = new DistanceCalculationHelper().cleanPath(closestExecution);
    List<CFAEdge> spEdges = new ArrayList<>(closestExecution);
    List<CFAEdge> deltasCe = new ArrayList<>();
    List<CFAEdge> deltasSp = new ArrayList<>();

    for (CFAEdge pEdge : counterexample) {
      if (!spEdges.contains(pEdge)) {
        deltasCe.add(pEdge);
      } else {
        spEdges.remove(pEdge);
      }
    }

    for (CFAEdge pEdge : closestExecution) {
      if (!counterexample.contains(pEdge)) {
        deltasSp.add(pEdge);
      } else {
        counterexample.remove(pEdge);
      }
    }

    deltasCe = cleanZeros(deltasCe);
    deltasSp = cleanZeros(deltasSp);

    Alignment<CFAEdge> wasChangedTo = new Alignment<>();
    List<CFAEdge> deleted = new ArrayList<>(deltasCe);
    List<CFAEdge> executed = new ArrayList<>(deltasSp);
    // FIND "WAS .. CHANGED TO: .."
    for (CFAEdge ceEdge : deltasCe) {
      for (CFAEdge spEdge : deltasSp) {
        if (ceEdge.getPredecessor().getNodeNumber() == spEdge.getPredecessor().getNodeNumber()) {
          wasChangedTo.addPair(ceEdge, spEdge);
          deleted.remove(ceEdge);
          executed.remove(spEdge);
        }
      }
    }

    writeFaults(wasChangedTo, deleted, executed, ceInfo);
  }

  private List<CFAEdge> cleanZeros(List<CFAEdge> path) {
    List<CFAEdge> result = new ArrayList<>();
    for (CFAEdge e : path) {
      if (!e.getEdgeType().equals(CFAEdgeType.BlankEdge)) {
        result.add(e);
      }
    }
    return result;
  }

  private void faultInfo(List<Fault> faults, CounterexampleInfo cInfo) {
    FaultLocalizationInfo info = new FaultLocalizationInfo(faults, cInfo);
    info.getHtmlWriter().toHtml(faults.get(0));
    info.apply();
  }

  public void writeFaults(
      Alignment<CFAEdge> wasChangedTo,
      List<CFAEdge> deleted,
      List<CFAEdge> executed,
      CounterexampleInfo ceInfos) {
    List<FaultInfo> hints = new ArrayList<>();
    Set<FaultContribution> contributionSet = new HashSet<>();
    Fault fault;
    for (int i = 0; i < wasChangedTo.getCounterexample().size(); i++) {
      FaultContribution con = new FaultContribution(wasChangedTo.getCounterexampleElement(i));
      contributionSet.add(con);
      hints.add(
          FaultInfo.justify(
              "LINE "
                  + wasChangedTo.getCounterexampleElement(i).getLineNumber()
                  + " WAS: "
                  + wasChangedTo.getCounterexampleElement(i).getCode()
                  + ", CHANGED TO: "
                  + wasChangedTo.getSafePathElement(i).getCode()));
    }

    for (CFAEdge delEdge : deleted) {
      FaultContribution con = new FaultContribution(delEdge);
      contributionSet.add(con);
      hints.add(
          FaultInfo.justify("LINE " + delEdge.getLineNumber() + ", DELETED: " + delEdge.getCode()));
    }

    for (CFAEdge exEdge : executed) {
      FaultContribution con = new FaultContribution(exEdge);
      contributionSet.add(con);
      hints.add(
          FaultInfo.justify(
              "LINE " + exEdge.getLineNumber() + ", WAS EXECUTED: " + exEdge.getCode()));
    }

    fault = new Fault(contributionSet);
    for (FaultInfo hint : hints) {
      fault.addInfo(hint);
    }

    List<Fault> faults = new ArrayList<>();
    faults.add(fault);
    faultInfo(faults, ceInfos);
  }
}
