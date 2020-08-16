// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.explainer;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
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
   * @param logger For Printing
   */
  public void explainDeltas(
      List<CFAEdge> counterexample,
      List<CFAEdge> closestExecution,
      LogManager logger,
      CounterexampleInfo ceInfo) {
    logger.log(Level.INFO, "Explain Tool Started");
    counterexample = cleanPath(counterexample);
    closestExecution = cleanPath(closestExecution);
    List<CFAEdge> ceEdges = cleanPath(counterexample);
    List<CFAEdge> spEdges = cleanPath(closestExecution);

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
      if (!ceEdges.contains(pEdge)) {
        deltasSp.add(pEdge);
      } else {
        ceEdges.remove(pEdge);
      }
    }

    deltasCe = cleanZeros(deltasCe);
    deltasSp = cleanZeros(deltasSp);

    logger.log(Level.INFO, "COUNTEREXAMPLE DIFFERENCES");
    for (CFAEdge pCFAEdge : deltasCe) {
      logger.log(Level.INFO, pCFAEdge.getLineNumber() + ": " + pCFAEdge.getDescription());
    }
    logger.log(Level.INFO, "-------------------------------------------");
    logger.log(Level.INFO, "CLOSEST SUCCESSFUL EXECUTION DIFFERENCES");
    for (CFAEdge pCFAEdge : deltasSp) {
      logger.log(Level.INFO, pCFAEdge.getLineNumber() + ": " + pCFAEdge.getDescription());
    }

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

  /**
   * Filters the Path to STOP at the "__VERIFIER_ASSERT" Node
   *
   * @param path The path that we want to filter
   * @return the same path but without the nodes after the Assertion
   */
  private List<CFAEdge> cleanPath(List<CFAEdge> path) {
    List<CFAEdge> flow = path;
    List<CFAEdge> filteredEdges = new ArrayList<>();

    // TODO: Review: filteredEdges contain all the Edges of a path up until the Node that VERIFIES
    // the spec
    for (int i = 0; i < flow.size(); i++) {
      if (flow.get(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        List<String> code = Splitter.onPattern("\\s*[()]\\s*").splitToList(flow.get(i).getCode());
        if (!code.isEmpty()) {
          if (code.get(0).equals("__VERIFIER_assert")) {
            filteredEdges.add(flow.get(i));
            return filteredEdges;
          }
        }
      }
      filteredEdges.add(flow.get(i));
    }
    return filteredEdges;
  }

  private List<CFAEdge> cleanZeros(List<CFAEdge> path) {
    List<CFAEdge> result = new ArrayList<>();
    for (CFAEdge e : path) {
      if (!(e.getEdgeType().equals(CFAEdgeType.BlankEdge))) {
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
    List<Fault> faults = new ArrayList<>();
    for (int i = 0; i < wasChangedTo.getCounterexample().size(); i++) {
      Set<FaultContribution> contributionSet = new HashSet<>();
      FaultContribution con = new FaultContribution(wasChangedTo.getCounterexampleElement(i));
      contributionSet.add(con);
      Fault f = new Fault(contributionSet);
      f.addInfo(FaultInfo.hint(" CHANGED TO: " + wasChangedTo.getSafePathElement(i).getCode()));
      faults.add(f);
    }

    for (CFAEdge delEdge : deleted) {
      Set<FaultContribution> contributionSet = new HashSet<>();
      FaultContribution con = new FaultContribution(delEdge);
      contributionSet.add(con);
      Fault f = new Fault(contributionSet);
      f.addInfo(FaultInfo.hint("DELETED: " + delEdge.getCode()));
      faults.add(f);
    }

    for (CFAEdge exEdge : executed) {
      Set<FaultContribution> contributionSet = new HashSet<>();
      FaultContribution con = new FaultContribution(exEdge);
      contributionSet.add(con);
      Fault f = new Fault(contributionSet);
      f.addInfo(FaultInfo.hint("EXECUTED: " + exEdge.getCode()));
      faults.add(f);
    }

    faultInfo(faults, ceInfos);
  }
}
